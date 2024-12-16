import java.sql.*;
import java.util.Scanner;

public class HousekeepingOperations {
    private static final Scanner scanner = new Scanner(System.in);
    
    public static void handleHousekeepingChoice(int choice, Connection conn) throws SQLException {
        switch (choice) {
            case 1:
                viewPendingTasks(conn);
                break;
            case 2:
                viewCompletedTasks(conn);
                break;
            case 3:
                updateTaskStatus(conn);
                break;
            case 4:
                viewCleaningSchedule(conn);
                break;
            default:
                System.out.println("Invalid choice");
        }
    }

    private static void viewPendingTasks(Connection conn) throws SQLException {
        try {
            System.out.print("Enter your housekeeper ID: ");
            int housekeeperId = Integer.parseInt(scanner.nextLine());
            
            if (!validateHousekeeperId(conn, housekeeperId)) {
                System.out.println("Invalid housekeeper ID or user is not a housekeeper.");
                return;
            }
            
            String query = """
                SELECT HS.scheduleID, HS.scheduled_time, R.room_no
                FROM HousekeepingSchedule HS 
                JOIN Room R ON HS.roomID = R.roomID 
                WHERE HS.status = 'incomplete' 
                AND HS.housekeeperID = ?
                ORDER BY HS.scheduled_time ASC
            """;
            
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, housekeeperId);
            
            ResultSet rs = pstmt.executeQuery();
            
            System.out.println("\nPending Tasks:");
            System.out.printf("%-12s %-20s %-10s%n", "Schedule ID", "Scheduled Time", "Room No");
            System.out.println("-".repeat(50));
            
            boolean hasRecords = false;
            while (rs.next()) {
                hasRecords = true;
                System.out.printf("%-12d %-20s %-10s%n",
                    rs.getInt("scheduleID"),
                    rs.getTimestamp("scheduled_time"),
                    rs.getString("room_no"));
            }
            
            if (!hasRecords) {
                System.out.println("No pending tasks found.");
            }
        } catch (Exception e) {
            System.out.println("Error viewing pending tasks: " + e.getMessage());
        }
    }

    private static void viewCompletedTasks(Connection conn) throws SQLException {
        try {
            System.out.print("Enter your housekeeper ID: ");
            int housekeeperId = Integer.parseInt(scanner.nextLine());
            
            if (!validateHousekeeperId(conn, housekeeperId)) {
                System.out.println("Invalid housekeeper ID or user is not a housekeeper.");
                return;
            }
            
            String query = """
                SELECT HS.scheduleID, HS.scheduled_time, R.room_no 
                FROM HousekeepingSchedule HS 
                JOIN Room R ON HS.roomID = R.roomID 
                WHERE HS.status = 'completed' 
                AND HS.housekeeperID = ?
                ORDER BY HS.scheduled_time DESC
            """;
            
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, housekeeperId);
            
            ResultSet rs = pstmt.executeQuery();
            
            System.out.println("\nCompleted Tasks:");
            System.out.printf("%-12s %-20s %-10s%n", "Schedule ID", "Scheduled Time", "Room No");
            System.out.println("-".repeat(50));
            
            boolean hasRecords = false;
            while (rs.next()) {
                hasRecords = true;
                System.out.printf("%-12d %-20s %-10s%n",
                    rs.getInt("scheduleID"),
                    rs.getTimestamp("scheduled_time"),
                    rs.getString("room_no"));
            }
            
            if (!hasRecords) {
                System.out.println("No completed tasks found.");
            }
        } catch (Exception e) {
            System.out.println("Error viewing completed tasks: " + e.getMessage());
        }
    }

    private static void updateTaskStatus(Connection conn) throws SQLException {
        try {
            System.out.print("Enter your housekeeper ID: ");
            int housekeeperId = Integer.parseInt(scanner.nextLine());
            
            if (!validateHousekeeperId(conn, housekeeperId)) {
                System.out.println("Invalid housekeeper ID or user is not a housekeeper.");
                return;
            }
            
            System.out.print("Enter schedule ID: ");
            int scheduleId = Integer.parseInt(scanner.nextLine());
            
            if (!validateScheduleId(conn, scheduleId, housekeeperId)) {
                System.out.println("Invalid schedule ID or task is not assigned to you.");
                return;
            }
            
            conn.setAutoCommit(false);
            
            String updateSchedule = """
                UPDATE HousekeepingSchedule
                SET status = 'completed',
                    completed_time = CURRENT_TIMESTAMP 
                WHERE scheduleID = ?
                AND housekeeperID = ?
                AND status = 'incomplete'
            """;
            
            PreparedStatement pstmt = conn.prepareStatement(updateSchedule);
            pstmt.setInt(1, scheduleId);
            pstmt.setInt(2, housekeeperId);
            
            int updated = pstmt.executeUpdate();
            
            if (updated > 0) {
                String updateRoom = """
                    UPDATE Room 
                    SET status = 'available' 
                    WHERE roomID = (
                        SELECT roomID 
                        FROM HousekeepingSchedule 
                        WHERE scheduleID = ?
                    ) AND status = 'cleaning'
                """;
                
                pstmt = conn.prepareStatement(updateRoom);
                pstmt.setInt(1, scheduleId);
                pstmt.executeUpdate();
                
                conn.commit();
                System.out.println("Task marked as completed successfully!");
            } else {
                System.out.println("Task not found or already completed.");
                conn.rollback();
            }
        } catch (Exception e) {
            conn.rollback();
            System.out.println("Error updating task status: " + e.getMessage());
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private static void viewCleaningSchedule(Connection conn) throws SQLException {
        try {
            System.out.print("Enter your housekeeper ID: ");
            int housekeeperId = Integer.parseInt(scanner.nextLine());
            
            if (!validateHousekeeperId(conn, housekeeperId)) {
                System.out.println("Invalid housekeeper ID or user is not a housekeeper.");
                return;
            }
            
            String query = """
                SELECT HS.scheduleID, HS.scheduled_time, HS.status, R.room_no
                FROM HousekeepingSchedule HS 
                JOIN Room R ON HS.roomID = R.roomID 
                WHERE HS.housekeeperID = ?
                ORDER BY HS.scheduled_time ASC
            """;
            
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, housekeeperId);
            
            ResultSet rs = pstmt.executeQuery();
            
            System.out.println("\nCleaning Schedule:");
            System.out.printf("%-12s %-20s %-15s %-10s%n", 
                "Schedule ID", "Scheduled Time", "Status", "Room No");
            System.out.println("-".repeat(60));
            
            boolean hasRecords = false;
            while (rs.next()) {
                hasRecords = true;
                System.out.printf("%-12d %-20s %-15s %-10s%n",
                    rs.getInt("scheduleID"),
                    rs.getTimestamp("scheduled_time"),
                    rs.getString("status"),
                    rs.getString("room_no"));
            }
            
            if (!hasRecords) {
                System.out.println("No scheduled tasks found.");
            }
        } catch (Exception e) {
            System.out.println("Error viewing cleaning schedule: " + e.getMessage());
        }
    }

    private static boolean validateHousekeeperId(Connection conn, int housekeeperId) throws SQLException {
        String query = """
            SELECT COUNT(*) 
            FROM Employee e 
            JOIN User u ON e.userID = u.userID 
            WHERE e.userID = ? AND u.user_type = 'housekeeper'
        """;
        
        PreparedStatement pstmt = conn.prepareStatement(query);
        pstmt.setInt(1, housekeeperId);
        ResultSet rs = pstmt.executeQuery();
        
        return rs.next() && rs.getInt(1) > 0;
    }

    private static boolean validateScheduleId(Connection conn, int scheduleId, int housekeeperId) throws SQLException {
        String query = """
            SELECT COUNT(*) 
            FROM HousekeepingSchedule 
            WHERE scheduleID = ? AND housekeeperID = ? AND status = 'incomplete'
        """;
        
        PreparedStatement pstmt = conn.prepareStatement(query);
        pstmt.setInt(1, scheduleId);
        pstmt.setInt(2, housekeeperId);
        ResultSet rs = pstmt.executeQuery();
        
        return rs.next() && rs.getInt(1) > 0;
    }
}