import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;
import java.sql.ResultSet;
import java.sql.SQLException;

// This is a test to check if the DB connection works
public class App {
    public static void main(String[] args) {

        // Loading in properties(config)
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream("config.properties")) {
            properties.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
            return; // Exit if properties file cannot be loaded
        }

        // Database connection parameters from properties
        String url = properties.getProperty("db.url");
        String username = properties.getProperty("db.username");
        String password = properties.getProperty("db.password");
        
        
        try {
            // Register JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // Open a connection
            System.out.println("Connecting to database...");
            Connection conn = DriverManager.getConnection(url, username, password);
            
            // Create a statement
            Statement stmt = conn.createStatement();
            
            // Create a test table
            String createTable = "CREATE TABLE IF NOT EXISTS messages " +
                               "(id INTEGER not NULL, " +
                               " message VARCHAR(50), " +
                               " PRIMARY KEY ( id ))";
            stmt.executeUpdate(createTable);
            
            // Insert a message
            String insert = "INSERT INTO messages (id, message) " +
                          "VALUES (1, 'Hello, JDBC World!')";
            stmt.executeUpdate(insert);
            
            // Query the message
            String select = "SELECT message FROM messages WHERE id = 1";
            ResultSet rs = stmt.executeQuery(select);
            
            // Display the message
            while(rs.next()) {
                System.out.println("Retrieved message: " + rs.getString("message"));
            }
            
            // Clean up resources
            rs.close();
            stmt.close();
            conn.close();
            
        } catch(SQLException se) {
            se.printStackTrace();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}