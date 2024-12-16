// GuestOperations.java
import java.sql.*;
import javax.swing.*;

public class GuestOperations {
    
    public static void handleGuestChoice(int choice, Connection conn) throws SQLException {
        switch (choice) {
            case 1:
                addNewBooking(conn);
                break;
            case 2:
                viewAvailableRooms(conn);
                break;
            case 3:
                viewMyBookings(conn);
                break;
            case 4:
                cancelBooking(conn);
                break;
            default:
                JOptionPane.showMessageDialog(null, "Invalid choice");
        }
    }
    private static void addNewBooking(Connection conn) throws SQLException {
        try {
            String guestIdStr = JOptionPane.showInputDialog("Enter guest ID:");
            int guestId = Integer.parseInt(guestIdStr);
    
            String roomIdStr = JOptionPane.showInputDialog("Enter room ID:");
            int roomId = Integer.parseInt(roomIdStr);
    
            String checkInDate = JOptionPane.showInputDialog("Enter check-in date (YYYY-MM-DD):");
            String checkOutDate = JOptionPane.showInputDialog("Enter check-out date (YYYY-MM-DD):");
            String numberOfGuestsStr = JOptionPane.showInputDialog("Enter number of guests:");
            int numberOfGuests = Integer.parseInt(numberOfGuestsStr);
    
            // Generate a new booking ID
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT MAX(bookingID) + 1 FROM Booking");
            int bookingId = rs.next() ? rs.getInt(1) : 1;
    
            // Generate a new payment ID
            rs = stmt.executeQuery("SELECT MAX(paymentID) + 1 FROM Payment");
            int paymentId = rs.next() ? rs.getInt(1) : 1;
    
            // Start transaction
            conn.setAutoCommit(false);
    
            // Insert booking
            String insertBooking = "INSERT INTO Booking (bookingID, guestID, check_in_date, check_out_date, booking_date, number_of_guests) VALUES (?, ?, ?, ?, CURRENT_DATE, ?)";
            PreparedStatement pstmt = conn.prepareStatement(insertBooking);
            pstmt.setInt(1, bookingId);
            pstmt.setInt(2, guestId);
            pstmt.setString(3, checkInDate);
            pstmt.setString(4, checkOutDate);
            pstmt.setInt(5, numberOfGuests);
            pstmt.executeUpdate();
    
            // Insert into ManagedBy
            String insertManagedBy = "INSERT INTO ManagedBy (bookingID, receptionistID, status) VALUES (?, NULL, 'pending')";
            pstmt = conn.prepareStatement(insertManagedBy);
            pstmt.setInt(1, bookingId);
            pstmt.executeUpdate();
    
            // Insert into BookedRooms
            String insertBookedRooms = "INSERT INTO BookedRooms (roomID, bookingID) VALUES (?, ?)";
            pstmt = conn.prepareStatement(insertBookedRooms);
            pstmt.setInt(1, roomId);
            pstmt.setInt(2, bookingId);
            pstmt.executeUpdate();
    
            // Insert initial payment record with pending status only
            String insertPayment = "INSERT INTO Payment (paymentID, bookingID, payment_status) VALUES (?, ?, 'pending')";
            pstmt = conn.prepareStatement(insertPayment);
            pstmt.setInt(1, paymentId);
            pstmt.setInt(2, bookingId);
            pstmt.executeUpdate();
    
            conn.commit();
            JOptionPane.showMessageDialog(null, "Booking created successfully! Booking ID: " + bookingId + ", Payment ID: " + paymentId);
    
        } catch (Exception e) {
            conn.rollback();
            JOptionPane.showMessageDialog(null, "Error creating booking: " + e.getMessage());
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private static void viewAvailableRooms(Connection conn) throws SQLException {
        try {
            String checkInDate = JOptionPane.showInputDialog("Enter check-in date (YYYY-MM-DD):");
            String checkOutDate = JOptionPane.showInputDialog("Enter check-out date (YYYY-MM-DD):");

            String query = """
                SELECT DISTINCT
                    r.roomID, h.hotel_name, h.address, h.rating,
                    r.room_no, rt.type_name, rt.base_capacity, rt.price_per_night
                FROM Room r
                JOIN Hotel h ON r.hotelID = h.hotelID
                JOIN RoomType rt ON r.typeID = rt.typeID
                WHERE r.status = 'available'
                AND r.roomID NOT IN (
                    SELECT br.roomID 
                    FROM BookedRooms br
                    JOIN Booking b ON br.bookingID = b.bookingID
                    JOIN ManagedBy mb ON b.bookingID = mb.bookingID
                    WHERE mb.status NOT IN ('denied', 'cancelled')
                    AND (
                        (b.check_in_date BETWEEN ? AND ?)
                        OR (b.check_out_date BETWEEN ? AND ?)
                        OR (? BETWEEN b.check_in_date AND b.check_out_date)
                    )
                )
                ORDER BY rt.price_per_night
            """;

            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, checkInDate);
            pstmt.setString(2, checkOutDate);
            pstmt.setString(3, checkInDate);
            pstmt.setString(4, checkOutDate);
            pstmt.setString(5, checkInDate);

            ResultSet rs = pstmt.executeQuery();

            StringBuilder output = new StringBuilder("\nAvailable Rooms:\n");
            output.append(String.format("%-8s %-20s %-10s %-15s %-10s %-8s%n", 
                "Room ID", "Hotel", "Room No", "Type", "Capacity", "Price"));
            output.append("-".repeat(80)).append("\n");

            while (rs.next()) {
                output.append(String.format("%-8d %-20s %-10s %-15s %-10d $%.2f%n",
                    rs.getInt("roomID"),
                    rs.getString("hotel_name"),
                    rs.getString("room_no"),
                    rs.getString("type_name"),
                    rs.getInt("base_capacity"),
                    rs.getDouble("price_per_night")));
            }

            JOptionPane.showMessageDialog(null, output.toString());

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error viewing available rooms: " + e.getMessage());
        }
    }

    private static void viewMyBookings(Connection conn) throws SQLException {
        try {
            String guestIdStr = JOptionPane.showInputDialog("Enter your guest ID:");
            int guestId = Integer.parseInt(guestIdStr);
    
            String query = """
                SELECT 
                    b.bookingID,
                    h.hotel_name,
                    GROUP_CONCAT(r.room_no) as booked_rooms,
                    GROUP_CONCAT(rt.type_name) as room_types,
                    b.check_in_date,
                    b.check_out_date,
                    b.booking_date,
                    b.number_of_guests,
                    COALESCE(p.payment_status, 'Unpaid') as payment_status
                FROM Booking b
                JOIN BookedRooms br ON b.bookingID = br.bookingID
                JOIN Room r ON br.roomID = r.roomID
                JOIN Hotel h ON r.hotelID = h.hotelID
                JOIN RoomType rt ON r.typeID = rt.typeID
                LEFT JOIN Payment p ON b.bookingID = p.bookingID
                WHERE b.guestID = ?
                GROUP BY 
                    b.bookingID,
                    h.hotel_name,
                    b.check_in_date,
                    b.check_out_date,
                    b.booking_date,
                    b.number_of_guests,
                    p.payment_status
                ORDER BY b.check_in_date DESC
            """;
    
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, guestId);
            ResultSet rs = pstmt.executeQuery();
    
            StringBuilder output = new StringBuilder("\nYour Bookings:\n");
            output.append(String.format("%-8s %-20s %-15s %-15s %-12s %-12s %-8s %-10s%n",
                "ID", "Hotel", "Rooms", "Room Types", "Check In", "Check Out", "Guests", "Status"));
            output.append("-".repeat(100)).append("\n");
    
            while (rs.next()) {
                output.append(String.format("%-8d %-20s %-15s %-15s %-12s %-12s %-8d %-10s%n",
                    rs.getInt("bookingID"),
                    rs.getString("hotel_name"),
                    rs.getString("booked_rooms"),
                    rs.getString("room_types"),
                    rs.getDate("check_in_date"),
                    rs.getDate("check_out_date"),
                    rs.getInt("number_of_guests"),
                    rs.getString("payment_status")));
            }
    
            JOptionPane.showMessageDialog(null, output.toString());
    
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error viewing bookings: " + e.getMessage());
        }
    }
    
    private static void cancelBooking(Connection conn) throws SQLException {
        try {
            String guestIdStr = JOptionPane.showInputDialog("Enter your guest ID:");
            int guestId = Integer.parseInt(guestIdStr);

            String bookingIdStr = JOptionPane.showInputDialog("Enter booking ID to cancel:");
            int bookingId = Integer.parseInt(bookingIdStr);

            // Start transaction
            conn.setAutoCommit(false);

            // Check if booking exists and can be cancelled
            String checkQuery = """
                SELECT b.bookingID 
                FROM Booking b
                JOIN ManagedBy mb ON b.bookingID = mb.bookingID
                WHERE b.bookingID = ?
                AND b.guestID = ?
                AND mb.status IN ('pending', 'confirmed')
            """;

            PreparedStatement pstmt = conn.prepareStatement(checkQuery);
            pstmt.setInt(1, bookingId);
            pstmt.setInt(2, guestId);
            ResultSet rs = pstmt.executeQuery();

            if (!rs.next()) {
                JOptionPane.showMessageDialog(null, "Booking not found or cannot be cancelled.");
                conn.rollback();
                return;
            }

            // Update ManagedBy status
            String updateManagedBy = "UPDATE ManagedBy SET status = 'cancelled' WHERE bookingID = ?";
            pstmt = conn.prepareStatement(updateManagedBy);
            pstmt.setInt(1, bookingId);
            pstmt.executeUpdate();

            // Update Payment status
            String updatePayment = """
                UPDATE Payment
                SET payment_status = 'refunded'
                WHERE bookingID = ?
                AND payment_status IN ('confirmed', 'pending')
            """;
            pstmt = conn.prepareStatement(updatePayment);
            pstmt.setInt(1, bookingId);
            pstmt.executeUpdate();

            // Delete BookedRooms entries (we may not need this)
           // String deleteBookedRooms = "DELETE FROM BookedRooms WHERE bookingID = ?";
           // pstmt = conn.prepareStatement(deleteBookedRooms);
            //pstmt.setInt(1, bookingId);
            //pstmt.executeUpdate();

            conn.commit();
            JOptionPane.showMessageDialog(null, "Booking cancelled successfully!");

        } catch (Exception e) {
            conn.rollback();
            JOptionPane.showMessageDialog(null, "Error canceling booking: " + e.getMessage());
        } finally {
            conn.setAutoCommit(true);
        }
    }
}