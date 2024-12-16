// DatabaseConnection.java
import java.sql.*;
import java.io.*;
import java.util.Properties;

public class DatabaseConnection {
    private static String URL;
    private static String USER;
    private static String PASSWORD;

    static {
        // Load configuration from properties file
        Properties properties = new Properties();
        String configPath = System.getProperty("user.dir") + File.separator + "config.properties";
        
        try (InputStream input = new FileInputStream(configPath)) {
            properties.load(input);
            URL = properties.getProperty("db.url");
            USER = properties.getProperty("db.username");
            PASSWORD = properties.getProperty("db.password");
        } catch (IOException ex) {
            // Log the error and provide more specific error handling
            System.err.println("Failed to load config.properties from: " + configPath);
            System.err.println("Error: " + ex.getMessage());
            
            // Optionally set default values
            URL = "jdbc:mysql://localhost:3306/mydb";
            USER = "root";
            PASSWORD = "";
            
            // Or throw a runtime exception if the application cannot proceed without proper config
            // throw new RuntimeException("Could not load configuration file", ex);
        }
    }
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Database driver not found", e);
        }
    }
}
