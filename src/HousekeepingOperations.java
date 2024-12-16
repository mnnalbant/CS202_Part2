import java.sql.*;
import javax.swing.*;

public class HousekeepingOperations {
    
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
                JOptionPane.showMessageDialog(null, "Invalid choice");
        }
    }

    private static void viewPendingTasks(Connection conn) throws SQLException {
        try {
            String housekeeperIdStr = JOptionPane.showInputDialog("Enter your housekeeper ID:");
            int housekeeperId = Integer.parseInt(housekeeperIdStr);
            
            if (!validateHousekeeperId(conn, housekeeperId)) {
                JOptionPane.showMessageDialog(null, "Invalid housekeeper ID or user is not a housekeeper.");
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
            
            StringBuilder output = new StringBuilder("\nPending Tasks:\n");
            output.append(String.format("%-12s %-20s %-10s%n", "Schedule ID", "Scheduled Time", "Room No"));
            output.append("-".repeat(50)).append("\n");
            
            boolean hasRecords = false;
            while (rs.next()) {
                hasRecords = true;
                output.append(String.format("%-12d %-20s %-10s%n",
                    rs.getInt("scheduleID"),
                    rs.getTimestamp("scheduled_time"),
                    rs.getString("room_no")));
            }
            
            if (!hasRecords) {
                output.append("No pending tasks found.");
            }
            JOptionPane.showMessageDialog(null, output.toString());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error viewing pending tasks: " + e.getMessage());
        }
    }

    private static void viewCompletedTasks(Connection conn) throws SQLException {
        try {
            String housekeeperIdStr = JOptionPane.showInputDialog("Enter your housekeeper ID:");
            int housekeeperId = Integer.parseInt(housekeeperIdStr);
            
            if (!validateHousekeeperId(conn, housekeeperId)) {
                JOptionPane.showMessageDialog(null, "Invalid housekeeper ID or user is not a housekeeper.");
                return;
            }
            
            String query = """
                SELECT HS.scheduleID, HS.scheduled_time, R.room_no 
                FROM HousekeepingSchedule HS 
                JOIN Room R ON HS.roomID = R.roomID 
                WHERE HS.status = 'complete' 
                AND HS.housekeeperID = ?
                ORDER BY HS.scheduled_time DESC
            """;
            
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, housekeeperId);
            
            ResultSet rs = pstmt.executeQuery();
            
            StringBuilder output = new StringBuilder("\nCompleted Tasks:\n");
            output.append(String.format("%-12s %-20s %-10s%n", "Schedule ID", "Scheduled Time", "Room No"));
            output.append("-".repeat(50)).append("\n");
            
            boolean hasRecords = false;
            while (rs.next()) {
                hasRecords = true;
                output.append(String.format("%-12d %-20s %-10s%n",
                    rs.getInt("scheduleID"),
                    rs.getTimestamp("scheduled_time"),
                    rs.getString("room_no")));
            }
            
            if (!hasRecords) {
                output.append("No completed tasks found.");
            }
            JOptionPane.showMessageDialog(null, output.toString());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error viewing completed tasks: " + e.getMessage());
        }
    }

    private static void updateTaskStatus(Connection conn) throws SQLException {
        try {
            conn.setAutoCommit(false);

            String housekeeperIdStr = JOptionPane.showInputDialog("Enter your housekeeper ID:");
            int housekeeperId = Integer.parseInt(housekeeperIdStr);
            
            if (!validateHousekeeperId(conn, housekeeperId)) {
                JOptionPane.showMessageDialog(null, "Invalid housekeeper ID or user is not a housekeeper.");
                return;
            }
            
            String scheduleIdStr = JOptionPane.showInputDialog("Enter schedule ID:");
            int scheduleId = Integer.parseInt(scheduleIdStr);
            
            if (!validateScheduleId(conn, scheduleId, housekeeperId)) {
                JOptionPane.showMessageDialog(null, "Invalid schedule ID or task is not assigned to you.");
                return;
            }
                        
            String updateSchedule = """
                UPDATE HousekeepingSchedule
                SET status = 'complete'
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
                JOptionPane.showMessageDialog(null, "Task marked as completed successfully!");
            } else {
                JOptionPane.showMessageDialog(null, "Task not found or already completed.");
                conn.rollback();
            }
        } catch (Exception e) {
            conn.rollback();
            JOptionPane.showMessageDialog(null, "Error updating task status: " + e.getMessage());
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private static void viewCleaningSchedule(Connection conn) throws SQLException {
        try {
            String housekeeperIdStr = JOptionPane.showInputDialog("Enter your housekeeper ID:");
            int housekeeperId = Integer.parseInt(housekeeperIdStr);
            
            if (!validateHousekeeperId(conn, housekeeperId)) {
                JOptionPane.showMessageDialog(null, "Invalid housekeeper ID or user is not a housekeeper.");
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
            
            StringBuilder output = new StringBuilder("\nCleaning Schedule:\n");
            output.append(String.format("%-12s %-20s %-15s %-10s%n", 
                "Schedule ID", "Scheduled Time", "Status", "Room No"));
            output.append("-".repeat(60)).append("\n");
            
            boolean hasRecords = false;
            while (rs.next()) {
                hasRecords = true;
                output.append(String.format("%-12d %-20s %-15s %-10s%n",
                    rs.getInt("scheduleID"),
                    rs.getTimestamp("scheduled_time"),
                    rs.getString("status"),
                    rs.getString("room_no")));
            }
            
            if (!hasRecords) {
                output.append("No scheduled tasks found.");
            }
            JOptionPane.showMessageDialog(null, output.toString());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error viewing cleaning schedule: " + e.getMessage());
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