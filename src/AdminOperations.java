import java.sql.*;
import javax.swing.*;

public class AdminOperations {

    public static void handleAdminChoice(int choice, Connection conn) throws SQLException {
        switch (choice) {
            case 1:
                addRoom(conn);
                break;
            case 2:
                deleteRoom(conn);
                break;
            case 3:
                manageRoomStatus(conn);
                break;
            case 4:
                addUserAccount(conn);
                break;
            case 5:
                viewUserAccounts(conn);
                break;
            case 6:
                generateRevenueReport(conn);
                break;
            case 7:
                viewAllBookingRecords(conn);
                break;
            case 8:
                viewAllHousekeepingRecords(conn);
                break;
            case 9:
                viewMostBookedRoomTypes(conn);
                break;
            case 10:
                viewAllEmployees(conn);
                break;
            default:
                JOptionPane.showMessageDialog(null, "Invalid choice");
        }
    }

    private static void addRoom(Connection conn) throws SQLException {
        try {
            String hotelIdStr = JOptionPane.showInputDialog("Enter hotel ID:");
            int hotelId = Integer.parseInt(hotelIdStr);

            String roomNo = JOptionPane.showInputDialog("Enter room number:");
            String typeIdStr = JOptionPane.showInputDialog("Enter room type ID:");
            int typeId = Integer.parseInt(typeIdStr);

            // Generate new room ID
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT MAX(roomID) + 1 FROM Room");
            int roomId = rs.next() ? rs.getInt(1) : 1;

            String query = "INSERT INTO Room (roomID, hotelID, room_no, typeID, status) VALUES (?, ?, ?, ?, 'available')";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, roomId);
            pstmt.setInt(2, hotelId);
            pstmt.setString(3, roomNo);
            pstmt.setInt(4, typeId);

            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(null, "Room added successfully! Room ID: " + roomId);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error adding room: " + e.getMessage());
        }
    }

    private static void deleteRoom(Connection conn) throws SQLException {
        try {
            conn.setAutoCommit(false);
            String roomIdStr = JOptionPane.showInputDialog("Enter room ID to delete:");
            int roomId = Integer.parseInt(roomIdStr);
                        
            // Check for future bookings
            String checkBookings = """
                SELECT COUNT(*) as future_bookings
                FROM BookedRooms br
                JOIN Booking b ON br.bookingID = b.bookingID
                JOIN ManagedBy mb ON b.bookingID = mb.bookingID
                WHERE br.roomID = ?
                AND mb.status NOT IN ('denied', 'canceled')
                AND b.check_out_date > CURRENT_DATE
            """;
            
            PreparedStatement pstmt = conn.prepareStatement(checkBookings);
            pstmt.setInt(1, roomId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next() && rs.getInt("future_bookings") > 0) {
                JOptionPane.showMessageDialog(null, "Cannot delete room: There are future bookings for this room.");
                conn.rollback();
                return;
            }
            
            // Mark room as deleted
            String updateRoom = "UPDATE Room SET status = 'deleted' WHERE roomID = ?";
            pstmt = conn.prepareStatement(updateRoom);
            pstmt.setInt(1, roomId);
            int updated = pstmt.executeUpdate();
            
            if (updated > 0) {
                conn.commit();
                JOptionPane.showMessageDialog(null, "Room marked as deleted successfully!");
            } else {
                JOptionPane.showMessageDialog(null, "Room not found.");
                conn.rollback();
            }
        } catch (Exception e) {
            conn.rollback();
            JOptionPane.showMessageDialog(null, "Error deleting room: " + e.getMessage());
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private static void manageRoomStatus(Connection conn) throws SQLException {
        try {
            String roomIdStr = JOptionPane.showInputDialog("Enter room ID:");
            int roomId = Integer.parseInt(roomIdStr);
            
            String newStatus = JOptionPane.showInputDialog("Enter new status (available, maintenance, cleaning, occupied):");
            
            if (!isValidStatus(newStatus)) {
                JOptionPane.showMessageDialog(null, "Invalid status. Please use one of the available options.");
                return;
            }
            
            String query = "UPDATE Room SET status = ? WHERE roomID = ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, newStatus);
            pstmt.setInt(2, roomId);
            
            int updated = pstmt.executeUpdate();
            if (updated > 0) {
                JOptionPane.showMessageDialog(null, "Room status updated successfully!");
            } else {
                JOptionPane.showMessageDialog(null, "Room not found.");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error updating room status: " + e.getMessage());
        }
    }

    private static void addUserAccount(Connection conn) throws SQLException {
        try {
            conn.setAutoCommit(false);

            String userTypeStr = JOptionPane.showInputDialog("Select user type to add:\n1. Guest\n2. Receptionist\n3. Housekeeper");
            int userType = Integer.parseInt(userTypeStr);
            
            // Common user information
            String username = JOptionPane.showInputDialog("Enter username:");
            String password = JOptionPane.showInputDialog("Enter password:");
            String firstName = JOptionPane.showInputDialog("Enter first name:");
            String lastName = JOptionPane.showInputDialog("Enter last name:");
            String email = JOptionPane.showInputDialog("Enter email:");
            String phoneNumber = JOptionPane.showInputDialog("Enter phone number:");
            
            
            // Generate new user ID
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT MAX(userID) + 1 FROM User");
            int userId = rs.next() ? rs.getInt(1) : 1;
            
            String userTypeString;
            switch (userType) {
                case 1:
                    userTypeString = "guest";
                    break;
                case 2:
                    userTypeString = "receptionist";
                    break;
                case 3:
                    userTypeString = "housekeeper";
                    break;
                default:
                    throw new IllegalArgumentException("Invalid user type");
            }
            
            // Insert into User table
            String insertUser = """
                INSERT INTO User (userID, username, password, first_name, last_name, 
                                email, phone_number, user_type)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
            
            PreparedStatement pstmt = conn.prepareStatement(insertUser);
            pstmt.setInt(1, userId);
            pstmt.setString(2, username);
            pstmt.setString(3, password);
            pstmt.setString(4, firstName);
            pstmt.setString(5, lastName);
            pstmt.setString(6, email);
            pstmt.setString(7, phoneNumber);
            pstmt.setString(8, userTypeString);
            
            if (pstmt.executeUpdate() == 0) {
                throw new SQLException("Failed to create user account");
            }
            
            switch (userTypeString) {
                case "guest" -> addGuestDetails(conn, userId);
                case "receptionist" -> addReceptionistDetails(conn, userId);
                case "housekeeper" -> addHousekeeperDetails(conn, userId);
            }
            
            conn.commit();
            JOptionPane.showMessageDialog(null, "User account created successfully! User ID: " + userId);
            
        } catch (Exception e) {
            conn.rollback();
            JOptionPane.showMessageDialog(null, "Error creating user account: " + e.getMessage());
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private static void addGuestDetails(Connection conn, int userId) throws SQLException {
        String passportNumber = JOptionPane.showInputDialog("Enter passport number:");
        String nationality = JOptionPane.showInputDialog("Enter nationality:");
        
        String query = "INSERT INTO Guest (userID, passport_number, nationality) VALUES (?, ?, ?)";
        PreparedStatement pstmt = conn.prepareStatement(query);
        pstmt.setInt(1, userId);
        pstmt.setString(2, passportNumber);
        pstmt.setString(3, nationality);
        pstmt.executeUpdate();
    }

    private static void addEmployeeDetails(Connection conn, int userId) throws SQLException {
        String hotelIdStr = JOptionPane.showInputDialog("Enter hotel ID:");
        int hotelId = Integer.parseInt(hotelIdStr);
        String shift = JOptionPane.showInputDialog("Enter shift (morning/afternoon/night):");
        
        String query = "INSERT INTO Employee (userID, hotelID, shift) VALUES (?, ?, ?)";
        PreparedStatement pstmt = conn.prepareStatement(query);
        pstmt.setInt(1, userId);
        pstmt.setInt(2, hotelId);
        pstmt.setString(3, shift);
        pstmt.executeUpdate();
    }

    private static void addReceptionistDetails(Connection conn, int userId) throws SQLException {
        addEmployeeDetails(conn, userId);
        String query = "INSERT INTO Receptionist(userID) VALUES (?)";
        PreparedStatement pstmt = conn.prepareStatement(query);
        pstmt.setInt(1, userId);
        pstmt.executeUpdate();
    }

    private static void addHousekeeperDetails(Connection conn, int userId) throws SQLException {
        addEmployeeDetails(conn, userId);
        String query = "INSERT INTO Housekeeper(userID) VALUES (?)";
        PreparedStatement pstmt = conn.prepareStatement(query);
        pstmt.setInt(1, userId);
        pstmt.executeUpdate();
    }

    // Helper methods
    private static boolean isValidStatus(String status) {
        return status.matches("^(available|maintenance|cleaning|occupied)$");
    }

    private static void viewUserAccounts(Connection conn) throws SQLException {
        String query = """
            SELECT u.userID, u.username, u.first_name, u.last_name, u.email, 
                   u.phone_number, u.user_type,
                   CASE WHEN e.userID IS NOT NULL THEN e.shift ELSE NULL END as shift,
                   CASE WHEN e.userID IS NOT NULL THEN h.hotel_name ELSE NULL END as hotel_name
            FROM User u
            LEFT JOIN Employee e ON u.userID = e.userID
            LEFT JOIN Hotel h ON e.hotelID = h.hotelID
        """;
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            StringBuilder output = new StringBuilder("\nUser Accounts:\n");
            output.append(String.format("%-6s %-15s %-20s %-20s %-25s %-15s %-12s %-10s %-20s%n",
                "ID", "Username", "First Name", "Last Name", "Email", "Phone", "Type", "Shift", "Hotel"));
            output.append("-".repeat(140)).append("\n");
            
            while (rs.next()) {
                output.append(String.format("%-6d %-15s %-20s %-20s %-25s %-15s %-12s %-10s %-20s%n",
                    rs.getInt("userID"),
                    rs.getString("username"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("email"),
                    rs.getString("phone_number"),
                    rs.getString("user_type"),
                    rs.getString("shift"),
                    rs.getString("hotel_name")));
            }
            JOptionPane.showMessageDialog(null, output.toString());
        }
    }

    private static void generateRevenueReport(Connection conn) throws SQLException {
        try {
            String startDate = JOptionPane.showInputDialog("Enter start date (YYYY-MM-DD):");
            String endDate = JOptionPane.showInputDialog("Enter end date (YYYY-MM-DD):");
            
            String query = """
                SELECT 
                    h.hotelID,
                    h.hotel_name,
                    COUNT(DISTINCT b.bookingID) as total_bookings,
                    SUM(p.payment_amount) as total_revenue
                FROM Hotel h
                LEFT JOIN Room r ON h.hotelID = r.hotelID
                LEFT JOIN BookedRooms br ON r.roomID = br.roomID
                LEFT JOIN Booking b ON br.bookingID = b.bookingID
                LEFT JOIN Payment p ON b.bookingID = p.bookingID
                WHERE b.check_in_date BETWEEN ? AND ?
                AND p.payment_status = 'confirmed'
                GROUP BY h.hotelID, h.hotel_name
            """;
            
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, startDate);
            pstmt.setString(2, endDate);
            ResultSet rs = pstmt.executeQuery();
            
            StringBuilder output = new StringBuilder("\nRevenue Report from " + startDate + " to " + endDate + "\n");
            output.append(String.format("%-8s %-30s %-15s %-15s%n", 
                "HotelID", "Hotel Name", "Total Bookings", "Total Revenue"));
            output.append("-".repeat(70)).append("\n");
            
            double grandTotal = 0;
            int totalBookings = 0;
            
            while (rs.next()) {
                int bookings = rs.getInt("total_bookings");
                double revenue = rs.getDouble("total_revenue");
                grandTotal += revenue;
                totalBookings += bookings;
                
                output.append(String.format("%-8d %-30s %-15d $%-14.2f%n",
                    rs.getInt("hotelID"),
                    rs.getString("hotel_name"),
                    bookings,
                    revenue));
            }
            
            output.append("-".repeat(70)).append("\n");
            output.append(String.format("TOTAL: %-30s %-15d $%-14.2f%n", "", totalBookings, grandTotal));
            JOptionPane.showMessageDialog(null, output.toString());
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error generating revenue report: " + e.getMessage());
        }
    }

    private static void viewAllBookingRecords(Connection conn) throws SQLException {
        try {
            String query = """
                SELECT 
                    b.bookingID,
                    u.first_name as guest_name,
                    r.room_no,
                    h.hotel_name,
                    b.check_in_date,
                    b.check_out_date,
                    p.payment_status,
                    p.payment_amount
                FROM Booking b
                JOIN User u ON b.guestID = u.userID
                JOIN BookedRooms br ON b.bookingID = br.bookingID
                JOIN Room r ON br.roomID = r.roomID
                JOIN Hotel h ON r.hotelID = h.hotelID
                LEFT JOIN Payment p ON b.bookingID = p.bookingID
                ORDER BY b.check_in_date DESC
            """;
            
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            
            StringBuilder output = new StringBuilder("\nAll Booking Records:\n");
            output.append(String.format("%-8s %-20s %-10s %-20s %-12s %-12s %-15s %-10s%n",
                "ID", "Guest", "Room", "Hotel", "Check In", "Check Out", "Status", "Amount"));
            output.append("-".repeat(110)).append("\n");
            
            while (rs.next()) {
                output.append(String.format("%-8d %-20s %-10s %-20s %-12s %-12s %-15s $%-9.2f%n",
                    rs.getInt("bookingID"),
                    rs.getString("guest_name"),
                    rs.getString("room_no"),
                    rs.getString("hotel_name"),
                    rs.getDate("check_in_date"),
                    rs.getDate("check_out_date"),
                    rs.getString("payment_status"),
                    rs.getDouble("payment_amount")));
            }
            JOptionPane.showMessageDialog(null, output.toString());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error viewing booking records: " + e.getMessage());
        }
    }

    private static void viewAllHousekeepingRecords(Connection conn) throws SQLException {
        try {
            String query = """
                SELECT 
                    hs.scheduleID,
                    r.room_no,
                    h.hotel_name,
                    u.first_name as housekeeper_name,
                    hs.scheduled_time,
                    hs.status
                FROM HousekeepingSchedule hs
                JOIN Room r ON hs.roomID = r.roomID
                JOIN Hotel h ON r.hotelID = h.hotelID
                JOIN User u ON hs.housekeeperID = u.userID
                ORDER BY hs.scheduled_time DESC
            """;
            
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            
            StringBuilder output = new StringBuilder("\nHousekeeping Records:\n");
            output.append(String.format("%-8s %-10s %-20s %-20s %-20s %-10s%n",
                "ID", "Room", "Hotel", "Housekeeper", "Scheduled Time", "Status"));
            output.append("-".repeat(100)).append("\n");
            
            while (rs.next()) {
                output.append(String.format("%-8d %-10s %-20s %-20s %-20s %-10s%n",
                    rs.getInt("scheduleID"),
                    rs.getString("room_no"),
                    rs.getString("hotel_name"),
                    rs.getString("housekeeper_name"),
                    rs.getTimestamp("scheduled_time"),
                    rs.getString("status")));
            }
            JOptionPane.showMessageDialog(null, output.toString());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error viewing housekeeping records: " + e.getMessage());
        }
    }

    private static void viewMostBookedRoomTypes(Connection conn) throws SQLException {
        try {
            String query = """
                SELECT 
                    rt.type_name,
                    COUNT(br.roomID) as booking_count,
                    rt.base_capacity,
                    rt.price_per_night
                FROM RoomType rt
                JOIN Room r ON rt.typeID = r.typeID
                JOIN BookedRooms br ON r.roomID = br.roomID
                JOIN Booking b ON br.bookingID = b.bookingID
                JOIN ManagedBy mb ON b.bookingID = mb.bookingID
                WHERE mb.status NOT IN ('denied', 'canceled')
                GROUP BY rt.typeID, rt.type_name, rt.base_capacity, rt.price_per_night
                ORDER BY booking_count DESC
            """;
            
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            
            StringBuilder output = new StringBuilder("\nMost Booked Room Types:\n");
            output.append(String.format("%-20s %-15s %-15s %-15s%n",
                "Room Type", "Bookings", "Capacity", "Price/Night"));
            output.append("-".repeat(70)).append("\n");
            
            while (rs.next()) {
                output.append(String.format("%-20s %-15d %-15d $%-14.2f%n",
                    rs.getString("type_name"),
                    rs.getInt("booking_count"),
                    rs.getInt("base_capacity"),
                    rs.getDouble("price_per_night")));
            }
            JOptionPane.showMessageDialog(null, output.toString());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error viewing room types: " + e.getMessage());
        }
    }

    private static void viewAllEmployees(Connection conn) throws SQLException {
        try {
            String query = """
                SELECT 
                    u.userID,
                    u.first_name,
                    u.last_name,
                    u.user_type,
                    e.shift,
                    h.hotel_name
                FROM User u
                JOIN Employee e ON u.userID = e.userID
                JOIN Hotel h ON e.hotelID = h.hotelID
                WHERE u.user_type != 'guest'
                ORDER BY h.hotel_name, u.user_type
            """;
            
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            
            StringBuilder output = new StringBuilder("\nEmployee List:\n");
            output.append(String.format("%-8s %-15s %-15s %-15s %-10s %-20s%n",
                "ID", "First Name", "Last Name", "Role", "Shift", "Hotel"));
            output.append("-".repeat(90)).append("\n");
            
            while (rs.next()) {
                output.append(String.format("%-8d %-15s %-15s %-15s %-10s %-20s%n",
                    rs.getInt("userID"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("user_type"),
                    rs.getString("shift"),
                    rs.getString("hotel_name")));
            }
            JOptionPane.showMessageDialog(null, output.toString());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error viewing employees: " + e.getMessage());
        }
    }
}