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

public class InterestAccrueAllServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        JSONObject payload = new JSONObject();
        try {
            String body = readBody(request);
            if (body != null && body.trim().length() > 0) {
                payload = new JSONObject(body);
            }
        } catch (Exception ignored) {
            payload = new JSONObject();
        }

        String interestType = payload.optString("interestType", "simple").trim().toLowerCase();
        int days = payload.optInt("days", 30);
        if (days <= 0) {
            days = 30;
        }
        int compoundsPerYear = payload.optInt("compoundsPerYear", InterestConfig.getDefaultCompoundsPerYear());
        if (compoundsPerYear <= 0) {
            compoundsPerYear = 12;
        }
        int termMonths = payload.optInt("termMonths", 12);
        if (termMonths <= 0) {
            termMonths = 12;
        }

        Connection connection = null;
        PreparedStatement select = null;
        ResultSet resultSet = null;
        PreparedStatement update = null;
        PreparedStatement insert = null;
        int accruedCount = 0;
        BigDecimal totalInterest = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        try {
            connection = InterestConnectionFactory.openConnection();
            connection.setAutoCommit(false);
            select = connection.prepareStatement(
                "SELECT a.AccountID, a.Balance, ISNULL(at.InterestRate, 0) AS InterestRate " +
                    "FROM Accounts a LEFT JOIN AccountTypes at ON at.AccountTypeID = a.AccountTypeID " +
                    "WHERE a.Status = 'Active'"
            );
            update = connection.prepareStatement(
                "UPDATE Accounts SET Balance = ?, AvailableBalance = ?, InterestAccrued = ISNULL(InterestAccrued, 0) + ?, " +
                    "LastActivityDate = GETDATE(), ModifiedDate = GETDATE() WHERE AccountID = ?"
            );
            insert = connection.prepareStatement(
                "INSERT INTO InterestAccruals (AccountID, InterestType, PrincipalAmount, AnnualRate, AccrualDays, InterestAmount, CreatedDate) " +
                    "VALUES (?, ?, ?, ?, ?, ?, GETDATE())"
            );

            resultSet = select.executeQuery();
            while (resultSet.next()) {
                int accountId = resultSet.getInt("AccountID");
                BigDecimal principal = resultSet.getBigDecimal("Balance");
                double annualRate = resultSet.getDouble("InterestRate");
                if (annualRate <= 0d) {
                    annualRate = InterestConfig.getDefaultAnnualRate();
                }
                BigDecimal interestAmount = calculateInterest(principal, annualRate, days, interestType, compoundsPerYear, termMonths);
                BigDecimal balanceAfter = principal.add(interestAmount);

                update.setBigDecimal(1, balanceAfter);
                update.setBigDecimal(2, balanceAfter);
                update.setBigDecimal(3, interestAmount);
                update.setInt(4, accountId);
                update.executeUpdate();

                insert.setInt(1, accountId);
                insert.setString(2, interestType);
                insert.setBigDecimal(3, principal);
                insert.setBigDecimal(4, BigDecimal.valueOf(annualRate));
                insert.setInt(5, days);
                insert.setBigDecimal(6, interestAmount);
                insert.executeUpdate();

                accruedCount++;
                totalInterest = totalInterest.add(interestAmount);
            }

            connection.commit();
            JSONObject result = new JSONObject();
            result.put("status", "OK");
            result.put("interestType", interestType);
            result.put("days", days);
            result.put("accruedAccounts", accruedCount);
            result.put("totalInterest", totalInterest.setScale(2, RoundingMode.HALF_UP));
            response.getWriter().write(result.toString());
        } catch (SQLException exception) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ignored) {
                }
            }
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"status\":\"ERROR\",\"message\":\"Unable to accrue interest for all accounts.\"}");
        } finally {
            closeQuietly(resultSet);
            closeQuietly(select);
            closeQuietly(update);
            closeQuietly(insert);
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException ignored) {
                }
            }
            closeQuietly(connection);
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
        throw new IllegalArgumentException("Unsupported interest type.");
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

    private void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }
}
