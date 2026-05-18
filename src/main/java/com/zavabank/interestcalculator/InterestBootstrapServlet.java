package com.zavabank.interestcalculator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

public class InterestBootstrapServlet extends HttpServlet {
    @Override
    public void init() throws ServletException {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = InterestConnectionFactory.openConnection();
            statement = connection.prepareStatement(
                "IF OBJECT_ID('InterestAccruals', 'U') IS NULL " +
                    "CREATE TABLE InterestAccruals (" +
                    "AccrualID BIGINT IDENTITY(1,1) PRIMARY KEY, " +
                    "AccountID INT NOT NULL REFERENCES Accounts(AccountID), " +
                    "InterestType NVARCHAR(20) NOT NULL, " +
                    "PrincipalAmount DECIMAL(18,2) NOT NULL, " +
                    "AnnualRate DECIMAL(10,8) NOT NULL, " +
                    "AccrualDays INT NOT NULL, " +
                    "InterestAmount DECIMAL(18,2) NOT NULL, " +
                    "CreatedDate DATETIME NOT NULL DEFAULT GETDATE()" +
                    ");"
            );
            statement.execute();
        } catch (SQLException exception) {
            throw new ServletException("Interest bootstrap failed.", exception);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException ignored) {
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }
}
