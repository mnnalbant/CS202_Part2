import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class App {
    public static void main(String[] args) {
        // Database connection parameters
        String url = "jdbc:mysql://localhost:3306/test";
        String username = "root";
        String password = "password";
        
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