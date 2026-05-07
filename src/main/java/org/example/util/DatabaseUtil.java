package org.example.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseUtil {
    // Database connection parameters
    private static final String DB_URL = "jdbc:mysql://localhost:3306/pharmacie?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    // Singleton instance
    private static DatabaseUtil instance;
    private Connection connection;

    // Constructeur privé pour empêcher l'instanciation externe
    private DatabaseUtil() {
    }

    // Méthode Singleton
    public static DatabaseUtil getInstance() {
        if (instance == null) {
            instance = new DatabaseUtil();
        }
        return instance;
    }

    /**
     * Get database connection (static method for convenience)
     */
    public static Connection getConnection() throws SQLException {
        return getInstance().getConnectionInstance();
    }

    /**
     * Get database connection (instance method)
     */
    public Connection getConnectionInstance() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            if (connection != null && !connection.isClosed()) {
                try {
                    if (!connection.isValid(3)) {
                        connection.close();
                        connection = null;
                    }
                } catch (SQLException e) {
                    try {
                        connection.close();
                    } catch (SQLException ignored) {
                        /* ignore */
                    }
                    connection = null;
                }
            }

            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            }

            return connection;
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found", e);
        }
    }

    /**
     * Close database connection
     */
    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Check if database connection is available
     */
    public boolean isDatabaseAvailable() {
        try {
            Connection conn = getConnection();
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}
