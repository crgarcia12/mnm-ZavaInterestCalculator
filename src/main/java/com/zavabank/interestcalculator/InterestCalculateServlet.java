package com.zavabank.interestcalculator;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

public class InterestCalculateServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        JSONObject payload;
        try {
            payload = new JSONObject(readBody(request));
        } catch (Exception exception) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"status\":\"ERROR\",\"message\":\"Invalid JSON payload.\"}");
            return;
        }

        int accountId = payload.optInt("accountId", 0);
        if (accountId <= 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"status\":\"ERROR\",\"message\":\"accountId is required.\"}");
            return;
        }

        Connection connection = null;
        try {
            connection = InterestConnectionFactory.openConnection();
            AccountSnapshot account = loadAccount(connection, accountId);
            if (account == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"status\":\"NOT_FOUND\",\"message\":\"Account not found.\"}");
                return;
            }

            String interestType = payload.optString("interestType", "simple").trim().toLowerCase();
            int days = payload.optInt("days", 30);
            if (days <= 0) {
                days = 30;
            }
            double annualRate = payload.has("annualRate")
                ? payload.optDouble("annualRate", InterestConfig.getDefaultAnnualRate())
                : account.accountTypeRate;
            if (annualRate <= 0d) {
                annualRate = InterestConfig.getDefaultAnnualRate();
            }
            int compoundsPerYear = payload.optInt("compoundsPerYear", InterestConfig.getDefaultCompoundsPerYear());
            if (compoundsPerYear <= 0) {
                compoundsPerYear = 12;
            }
            int termMonths = payload.optInt("termMonths", 12);
            if (termMonths <= 0) {
                termMonths = 12;
            }

            BigDecimal principal = account.balance;
            BigDecimal interestAmount = calculateInterest(principal, annualRate, days, interestType, compoundsPerYear, termMonths);
            BigDecimal newBalance = principal.add(interestAmount);

            boolean applyToBalance = payload.optBoolean("applyToBalance", false);
            if (applyToBalance) {
                applyAccrual(connection, accountId, interestType, principal, annualRate, days, interestAmount, newBalance);
            }

            JSONObject result = new JSONObject();
            result.put("status", "OK");
            result.put("accountId", accountId);
            result.put("interestType", interestType);
            result.put("annualRate", annualRate);
            result.put("days", days);
            result.put("balanceBefore", principal);
            result.put("interestAmount", interestAmount);
            result.put("balanceAfter", newBalance);
            result.put("applied", applyToBalance);
            response.getWriter().write(result.toString());
        } catch (IllegalArgumentException exception) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"status\":\"ERROR\",\"message\":\"" + escapeJson(exception.getMessage()) + "\"}");
        } catch (SQLException exception) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"status\":\"ERROR\",\"message\":\"Unable to calculate interest.\"}");
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    private AccountSnapshot loadAccount(Connection connection, int accountId) throws SQLException {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            statement = connection.prepareStatement(
                "SELECT a.Balance, ISNULL(at.InterestRate, 0) AS InterestRate " +
                    "FROM Accounts a LEFT JOIN AccountTypes at ON at.AccountTypeID = a.AccountTypeID " +
                    "WHERE a.AccountID = ?"
            );
            statement.setInt(1, accountId);
            resultSet = statement.executeQuery();
            if (!resultSet.next()) {
                return null;
            }
            AccountSnapshot snapshot = new AccountSnapshot();
            snapshot.balance = resultSet.getBigDecimal("Balance");
            snapshot.accountTypeRate = resultSet.getDouble("InterestRate");
            return snapshot;
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
            if (statement != null) {
                statement.close();
            }
        }
    }

    private BigDecimal calculateInterest(
        BigDecimal principal,
        double annualRate,
        int days,
        String interestType,
        int compoundsPerYear,
        int termMonths
    ) {
        if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if ("simple".equals(interestType)) {
            return principal.multiply(BigDecimal.valueOf(annualRate))
                .multiply(BigDecimal.valueOf(days))
                .divide(BigDecimal.valueOf(365), 2, RoundingMode.HALF_UP);
        }
        if ("compound".equals(interestType)) {
            double exponent = ((double) compoundsPerYear) * ((double) days / 365d);
            double factor = Math.pow(1d + (annualRate / compoundsPerYear), exponent) - 1d;
            return principal.multiply(BigDecimal.valueOf(factor)).setScale(2, RoundingMode.HALF_UP);
        }
        if ("amortized".equals(interestType)) {
            double monthlyRate = annualRate / 12d;
            if (monthlyRate <= 0d) {
                return principal.divide(BigDecimal.valueOf(termMonths), 2, RoundingMode.HALF_UP);
            }
            double denominator = 1d - Math.pow(1d + monthlyRate, (-1d * termMonths));
            double payment = principal.doubleValue() * (monthlyRate / denominator);
            double monthlyPrincipal = principal.doubleValue() / termMonths;
            double monthlyInterest = payment - monthlyPrincipal;
            double dailyInterest = monthlyInterest / 30d;
            return BigDecimal.valueOf(dailyInterest * days).setScale(2, RoundingMode.HALF_UP);
        }
        throw new IllegalArgumentException("Unsupported interestType. Allowed: simple, compound, amortized.");
    }

    private void applyAccrual(
        Connection connection,
        int accountId,
        String interestType,
        BigDecimal principal,
        double annualRate,
        int days,
        BigDecimal interestAmount,
        BigDecimal newBalance
    ) throws SQLException {
        PreparedStatement updateAccount = null;
        PreparedStatement insertAccrual = null;
        try {
            connection.setAutoCommit(false);
            updateAccount = connection.prepareStatement(
                "UPDATE Accounts SET Balance = ?, AvailableBalance = ?, InterestAccrued = ISNULL(InterestAccrued, 0) + ?, " +
                    "LastActivityDate = GETDATE(), ModifiedDate = GETDATE() WHERE AccountID = ?"
            );
            updateAccount.setBigDecimal(1, newBalance);
            updateAccount.setBigDecimal(2, newBalance);
            updateAccount.setBigDecimal(3, interestAmount);
            updateAccount.setInt(4, accountId);
            updateAccount.executeUpdate();

            insertAccrual = connection.prepareStatement(
                "INSERT INTO InterestAccruals (AccountID, InterestType, PrincipalAmount, AnnualRate, AccrualDays, InterestAmount, CreatedDate) " +
                    "VALUES (?, ?, ?, ?, ?, ?, GETDATE())"
            );
            insertAccrual.setInt(1, accountId);
            insertAccrual.setString(2, interestType);
            insertAccrual.setBigDecimal(3, principal);
            insertAccrual.setBigDecimal(4, BigDecimal.valueOf(annualRate));
            insertAccrual.setInt(5, days);
            insertAccrual.setBigDecimal(6, interestAmount);
            insertAccrual.executeUpdate();
            connection.commit();
        } catch (SQLException exception) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }
            throw exception;
        } finally {
            if (insertAccrual != null) {
                insertAccrual.close();
            }
            if (updateAccount != null) {
                updateAccount.close();
            }
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }

    private String readBody(HttpServletRequest request) throws IOException {
        BufferedReader reader = request.getReader();
        StringBuilder body = new StringBuilder();
        String line = reader.readLine();
        while (line != null) {
            body.append(line);
            line = reader.readLine();
        }
        return body.toString();
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static class AccountSnapshot {
        private BigDecimal balance;
        private double accountTypeRate;
    }
}
