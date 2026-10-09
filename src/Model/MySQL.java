package Model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MySQL {
    private static Connection connection;
    private static final String username = "root";
    private static final String password = "Dinuja@2001";
    private static final String database = "globemed";

    // Singleton: Create or reuse connection
    public static Statement createConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/" + database, username, password);
            } catch (ClassNotFoundException e) {
                throw new SQLException("MySQL Driver not found: " + e.getMessage());
            }
        }
        return connection.createStatement();
    }

    // Execute INSERT, UPDATE, DELETE queries
    public static void iud(String query) throws SQLException {
        Statement stmt = null;
        try {
            stmt = createConnection();
            stmt.executeUpdate(query);
        } catch (SQLException e) {
            throw new SQLException("Query execution failed: " + e.getMessage());
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    System.err.println("Failed to close statement: " + e.getMessage());
                }
            }
        }
    }

    // Execute SELECT queries
    public static ResultSet search(String query) throws SQLException {
        Statement stmt = null;
        try {
            stmt = createConnection();
            return stmt.executeQuery(query);
        } catch (SQLException e) {
            throw new SQLException("Search query failed: " + e.getMessage());
        }
        // Note: Caller must close ResultSet and Statement
    }

    // Close connection (call when app exits)
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                System.err.println("Failed to close connection: " + e.getMessage());
            } finally {
                connection = null;
            }
        }
    }
}