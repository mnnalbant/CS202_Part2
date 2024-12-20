import java.sql.*;
import javax.swing.*;

public class ReceptionistOperations {
    
    public static void handleReceptionistChoice(int choice, Connection conn) throws SQLException {
        switch (choice) {
            case 1:
                addNewBooking(conn);
                break;
            case 2:
                modifyBooking(conn);
                break;
            case 3:
                deleteBooking(conn);
                break;
            case 4:
                viewBookings(conn);
                break;
            case 5:
                processPayment(conn);
                break;
            case 6:
                assignHousekeepingTask(conn);
                break;
            case 7:
                viewHousekeepersRecords(conn);
                break;
            default:
                JOptionPane.showMessageDialog(null, "Invalid choice");
        }
    }

    private static void addNewBooking(Connection conn) throws SQLException {
        try {
            conn.setAutoCommit(false);
            
            String guestIdStr = JOptionPane.showInputDialog("Enter guest ID:");
            int guestId = Integer.parseInt(guestIdStr);
            
            String checkInDate = JOptionPane.showInputDialog("Enter check-in date (YYYY-MM-DD):");
            String checkOutDate = JOptionPane.showInputDialog("Enter check-out date (YYYY-MM-DD):");
            String numberOfGuestsStr = JOptionPane.showInputDialog("Enter number of guests:");
            int numberOfGuests = Integer.parseInt(numberOfGuestsStr);
            String roomIdStr = JOptionPane.showInputDialog("Enter room ID:");
            int roomId = Integer.parseInt(roomIdStr);
            String receptionistIdStr = JOptionPane.showInputDialog("Enter receptionist ID:");
            int receptionistId = Integer.parseInt(receptionistIdStr);
            
            // Generate new booking ID
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT MAX(bookingID) + 1 FROM Booking");
            int bookingId = rs.next() ? rs.getInt(1) : 1;
            
            // Generate new payment ID
            rs = stmt.executeQuery("SELECT MAX(paymentID) + 1 FROM Payment");
            int paymentId = rs.next() ? rs.getInt(1) : 1;
            
            // Insert booking
            String insertBooking = """
                INSERT INTO Booking (bookingID, guestID, check_in_date, check_out_date, 
                number_of_guests, booking_date) 
                VALUES (?, ?, ?, ?, ?, CURRENT_DATE)
            """;
            PreparedStatement pstmt = conn.prepareStatement(insertBooking);
            pstmt.setInt(1, bookingId);
            pstmt.setInt(2, guestId);
            pstmt.setString(3, checkInDate);
            pstmt.setString(4, checkOutDate);
            pstmt.setInt(5, numberOfGuests);
            pstmt.executeUpdate();
            
            String insertManagedBy = """
                INSERT INTO ManagedBy (bookingID, receptionistID, status)
                VALUES (?, ?, 'confirmed')
            """;
            pstmt = conn.prepareStatement(insertManagedBy);
            pstmt.setInt(1, bookingId);
            pstmt.setInt(2, receptionistId);
            pstmt.executeUpdate();
            
            String insertBookedRoom = "INSERT INTO BookedRooms (roomID, bookingID) VALUES (?, ?)";
            pstmt = conn.prepareStatement(insertBookedRoom);
            pstmt.setInt(1, roomId);
            pstmt.setInt(2, bookingId);
            pstmt.executeUpdate();
            
            String insertPayment = "INSERT INTO Payment (paymentID, bookingID, payment_status) VALUES (?, ?, 'pending')"; //initial payment data w pending status only
            pstmt = conn.prepareStatement(insertPayment);
            pstmt.setInt(1, paymentId);
            pstmt.setInt(2, bookingId);
            pstmt.executeUpdate();
            
            conn.commit();
            JOptionPane.showMessageDialog(null, "Booking created successfully!\nBooking ID: " + bookingId + "\nPayment ID: " + paymentId);
            
        } catch (Exception e) {
            conn.rollback();
            JOptionPane.showMessageDialog(null, "Error creating booking: " + e.getMessage());
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private static void processPayment(Connection conn) throws SQLException {
        try {
            String paymentIdStr = JOptionPane.showInputDialog("Enter payment ID:");
            int paymentId = Integer.parseInt(paymentIdStr);
     
            // Get booking details and calculate payment amount
            String amountQuery = """
                SELECT rt.price_per_night, 
                       DATEDIFF(b.check_out_date, b.check_in_date) as stay_duration
                FROM Payment p
                JOIN Booking b ON p.bookingID = b.bookingID
                JOIN BookedRooms br ON b.bookingID = br.bookingID
                JOIN Room r ON br.roomID = r.roomID
                JOIN RoomType rt ON r.typeID = rt.typeID
                WHERE p.paymentID = ? AND p.payment_status = 'pending'
            """;
            PreparedStatement amountPstmt = conn.prepareStatement(amountQuery);
            amountPstmt.setInt(1, paymentId);
            ResultSet rs = amountPstmt.executeQuery();
     
            if (!rs.next()) {
                JOptionPane.showMessageDialog(null, "Payment not found or already processed.");
                return;
            }
     
            double basePrice = rs.getDouble("price_per_night");
            int stayDuration = rs.getInt("stay_duration");
            double totalAmount = basePrice * stayDuration;
            
            String transactionId = JOptionPane.showInputDialog("Enter transaction ID:");
            String paymentMethod = JOptionPane.showInputDialog("Enter payment method (credit_card/debit_card/cash/bank_transfer):");
            
            String query = """
                UPDATE Payment 
                SET payment_status = 'confirmed',
                    transactionID = ?,
                    payment_date = CURRENT_DATE,
                    payment_method = ?,
                    payment_amount = ?
                WHERE paymentID = ? AND payment_status = 'pending'
            """;
            
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, transactionId);
            pstmt.setString(2, paymentMethod);
            pstmt.setDouble(3, totalAmount);
            pstmt.setInt(4, paymentId);
            
            int updated = pstmt.executeUpdate();
            if (updated > 0) {
                JOptionPane.showMessageDialog(null, String.format("Payment processed successfully!\nTotal Amount: $%.2f", totalAmount));
            } else {
                JOptionPane.showMessageDialog(null, "Payment not found or already processed.");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error processing payment: " + e.getMessage());
        }
     }

    private static void modifyBooking(Connection conn) throws SQLException {
        try {
            String bookingIdStr = JOptionPane.showInputDialog("Enter booking ID to modify:");
            int bookingId = Integer.parseInt(bookingIdStr);
            
            String receptionistIdStr = JOptionPane.showInputDialog("Enter receptionist ID handling this booking:");
            Integer receptionistId = receptionistIdStr.isEmpty() ? null : Integer.parseInt(receptionistIdStr);
            
            JOptionPane.showMessageDialog(null, "Leave blank if no change is needed for the following fields:");
            
            String guestIdStr = JOptionPane.showInputDialog("Enter new guest ID:");
            Integer guestId = guestIdStr.isEmpty() ? null : Integer.parseInt(guestIdStr);
            
            String checkInDate = JOptionPane.showInputDialog("Enter new check-in date (YYYY-MM-DD):");
            String checkOutDate = JOptionPane.showInputDialog("Enter new check-out date (YYYY-MM-DD):");
            String guestsStr = JOptionPane.showInputDialog("Enter new number of guests:");
            Integer numberOfGuests = guestsStr.isEmpty() ? null : Integer.parseInt(guestsStr);
            String status = JOptionPane.showInputDialog("Enter new status (pending/confirmed):");
            
            String query = """
                UPDATE Booking b
                JOIN ManagedBy mb ON b.bookingID = mb.bookingID
                SET 
                    b.guestID = COALESCE(?, b.guestID),
                    b.check_in_date = COALESCE(?, b.check_in_date),
                    b.check_out_date = COALESCE(?, b.check_out_date),
                    b.number_of_guests = COALESCE(?, b.number_of_guests),
                    mb.status = COALESCE(?, mb.status),
                    mb.receptionistID = COALESCE(?, mb.receptionistID)
                WHERE b.bookingID = ?
                AND mb.status NOT IN ('checked_in', 'checked_out')
            """;
            
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setObject(1, guestId);
            pstmt.setString(2, checkInDate.isEmpty() ? null : checkInDate);
            pstmt.setString(3, checkOutDate.isEmpty() ? null : checkOutDate);
            pstmt.setObject(4, numberOfGuests);
            pstmt.setString(5, status.isEmpty() ? null : status);
            pstmt.setObject(6, receptionistId);  // Allow null if empty string entered
            pstmt.setInt(7, bookingId);
            
            int updated = pstmt.executeUpdate();
            
            //If status is changed to cancelled, delete BookedRooms entries
            /*
            if (!status.isEmpty() && status.equalsIgnoreCase("cancelled")) {
            String deleteBookedRooms = "DELETE FROM BookedRooms WHERE bookingID = ?";
            pstmt = conn.prepareStatement(deleteBookedRooms);
            pstmt.setInt(1, bookingId);
            pstmt.executeUpdate();
            }
            conn.commit();
            */
            
            if (updated > 0) {
                JOptionPane.showMessageDialog(null, "Booking modified successfully!");
            } else {
                JOptionPane.showMessageDialog(null, "Booking not found or cannot be modified.");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error modifying booking: " + e.getMessage());
        }
        
    }

    private static void deleteBooking(Connection conn) throws SQLException {
        try {
            conn.setAutoCommit(false);

            String bookingIdStr = JOptionPane.showInputDialog("Enter booking ID to delete:");
            int bookingId = Integer.parseInt(bookingIdStr);
            
            String checkPayment = """
                SELECT COUNT(*) 
                FROM Payment 
                WHERE bookingID = ? 
                AND payment_status = 'confirmed'
            """;
            
            PreparedStatement pstmt = conn.prepareStatement(checkPayment);
            pstmt.setInt(1, bookingId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next() && rs.getInt(1) > 0) {
                JOptionPane.showMessageDialog(null, "Cannot delete booking: Payment is already confirmed");
                conn.rollback();
                return;
            }
            
            String deleteBookedRooms = "DELETE FROM BookedRooms WHERE bookingID = ?";
            pstmt = conn.prepareStatement(deleteBookedRooms);
            pstmt.setInt(1, bookingId);
            pstmt.executeUpdate();
            
            String deleteBooking = "DELETE FROM Booking WHERE bookingID = ?";
            pstmt = conn.prepareStatement(deleteBooking);
            pstmt.setInt(1, bookingId);
            
            int deleted = pstmt.executeUpdate();
            if (deleted > 0) {
                conn.commit();
                JOptionPane.showMessageDialog(null, "Booking deleted successfully!");
            } else {
                conn.rollback();
                JOptionPane.showMessageDialog(null, "Booking not found.");
            }
        } catch (Exception e) {
            conn.rollback();
            JOptionPane.showMessageDialog(null, "Error deleting booking: " + e.getMessage());
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private static void viewBookings(Connection conn) throws SQLException {
        try {
            String query = """
                SELECT 
                    B.bookingID, B.guestID, G.passport_number, G.nationality, 
                    B.check_in_date, B.check_out_date, B.number_of_guests, 
                    B.booking_date, MB.status AS booking_status, 
                    R.room_no, RT.type_name AS room_type 
                FROM Booking B 
                JOIN Guest G ON B.guestID = G.userID 
                LEFT JOIN ManagedBy MB ON B.bookingID = MB.bookingID 
                LEFT JOIN BookedRooms BR ON B.bookingID = BR.bookingID 
                LEFT JOIN Room R ON BR.roomID = R.roomID
                LEFT JOIN RoomType RT ON R.typeID = RT.typeID 
                ORDER BY B.booking_date DESC
            """;
            
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            
            StringBuilder output = new StringBuilder("\nAll Bookings:\n");
            output.append(String.format("%-8s %-20s %-15s %-12s %-12s %-8s %-15s %-10s %-15s%n",
                "ID", "Guest Info", "Room", "Check In", "Check Out", "Guests", "Status", "Type", "Nationality"));
            output.append("-".repeat(120)).append("\n");
            
            while (rs.next()) {
                output.append(String.format("%-8d %-20s %-15s %-12s %-12s %-8d %-15s %-10s %-15s%n",
                    rs.getInt("bookingID"),
                    String.format("[%d] %s", rs.getInt("guestID"), rs.getString("passport_number")),
                    rs.getString("room_no"),
                    rs.getDate("check_in_date"),
                    rs.getDate("check_out_date"),
                    rs.getInt("number_of_guests"),
                    rs.getString("booking_status"),
                    rs.getString("room_type"),
                    rs.getString("nationality")));
            }
            JOptionPane.showMessageDialog(null, output.toString());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error viewing bookings: " + e.getMessage());
        }
    }

    private static void assignHousekeepingTask(Connection conn) throws SQLException {
        try {
            String roomIdStr = JOptionPane.showInputDialog("Enter room ID:");
            int roomId = Integer.parseInt(roomIdStr);
            
            String housekeeperIdStr = JOptionPane.showInputDialog("Enter housekeeper ID:");
            int housekeeperId = Integer.parseInt(housekeeperIdStr);
            
            String receptionistIdStr = JOptionPane.showInputDialog("Enter receptionist ID:");
            int receptionistId = Integer.parseInt(receptionistIdStr);
            
            String scheduledTime = JOptionPane.showInputDialog("Enter scheduled time (YYYY-MM-DD HH:MM:SS):");
            
            // Generate new schedule ID
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT MAX(scheduleID) + 1 FROM HousekeepingSchedule");
            int scheduleId = rs.next() ? rs.getInt(1) : 1;
            
            String query = """
                INSERT INTO HousekeepingSchedule 
                (scheduleID, roomID, housekeeperID, receptionistID, scheduled_time, status)
                VALUES (?, ?, ?, ?, ?, 'incomplete')
            """;
            
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, scheduleId);
            pstmt.setInt(2, roomId);
            pstmt.setInt(3, housekeeperId);
            pstmt.setInt(4, receptionistId);
            pstmt.setString(5, scheduledTime);
            
            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(null, "Housekeeping task assigned successfully! Schedule ID: " + scheduleId);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error assigning housekeeping task: " + e.getMessage());
        }
    }

    private static void viewHousekeepersRecords(Connection conn) throws SQLException {
        try {
            String query = """
                SELECT 
                    HS.housekeeperID, U.first_name, U.last_name, E.shift, 
                    COUNT(HS.scheduleID) AS assigned_tasks,
                    CASE 
                        WHEN COUNT(HS.scheduleID) = 0 THEN 'Available'
                        ELSE 'Busy'
                    END AS availability,
                    MIN(HS.scheduled_time) AS next_task_time
                FROM HousekeepingSchedule HS
                JOIN Employee E ON E.userID = HS.housekeeperID
                JOIN User U ON U.userID = HS.housekeeperID
                WHERE HS.status = 'incomplete'
                GROUP BY HS.housekeeperID, U.first_name, U.last_name, E.shift
            """;
            
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            
            StringBuilder output = new StringBuilder("\nHousekeepers Status:\n");
            output.append(String.format("%-8s %-20s %-10s %-15s %-15s %-20s%n",
                "ID", "Name", "Shift", "Tasks", "Status", "Next Task"));
            output.append("-".repeat(90)).append("\n");
            
            while (rs.next()) {
                output.append(String.format("%-8d %-20s %-10s %-15d %-15s %-20s%n",
                    rs.getInt("housekeeperID"),
                    rs.getString("first_name") + " " + rs.getString("last_name"),
                    rs.getString("shift"),
                    rs.getInt("assigned_tasks"),
                    rs.getString("availability"),
                    rs.getTimestamp("next_task_time")));
            }
            JOptionPane.showMessageDialog(null, output.toString());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error viewing housekeeper records: " + e.getMessage());
        }
    }
} 
