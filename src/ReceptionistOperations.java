import java.sql.*;
import java.util.Scanner;

public class ReceptionistOperations {
    private static final Scanner scanner = new Scanner(System.in);
    
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
                System.out.println("Invalid choice");
        }
    }

    private static void addNewBooking(Connection conn) throws SQLException {
        try {
            conn.setAutoCommit(false);
            
            System.out.print("Enter guest ID: ");
            int guestId = Integer.parseInt(scanner.nextLine());
            
            System.out.print("Enter check-in date (YYYY-MM-DD): ");
            String checkInDate = scanner.nextLine();
            
            System.out.print("Enter check-out date (YYYY-MM-DD): ");
            String checkOutDate = scanner.nextLine();
            
            System.out.print("Enter number of guests: ");
            int numberOfGuests = Integer.parseInt(scanner.nextLine());
            
            System.out.print("Enter room ID: ");
            int roomId = Integer.parseInt(scanner.nextLine());
            
            System.out.print("Enter receptionist ID: ");
            int receptionistId = Integer.parseInt(scanner.nextLine());
            
            // Generate new booking ID
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT MAX(bookingID) + 1 FROM Booking");
            int bookingId = rs.next() ? rs.getInt(1) : 1;
            
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
            
            // Insert managed by record
            String insertManagedBy = """
                INSERT INTO ManagedBy (bookingID, receptionistID, status)
                VALUES (?, ?, 'confirmed')
            """;
            pstmt = conn.prepareStatement(insertManagedBy);
            pstmt.setInt(1, bookingId);
            pstmt.setInt(2, receptionistId);
            pstmt.executeUpdate();
            
            // Insert booked room
            String insertBookedRoom = "INSERT INTO BookedRooms (roomID, bookingID) VALUES (?, ?)";
            pstmt = conn.prepareStatement(insertBookedRoom);
            pstmt.setInt(1, roomId);
            pstmt.setInt(2, bookingId);
            pstmt.executeUpdate();
            
            conn.commit();
            System.out.println("Booking created successfully! Booking ID: " + bookingId);
            
        } catch (Exception e) {
            conn.rollback();
            System.out.println("Error creating booking: " + e.getMessage());
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private static void processPayment(Connection conn) throws SQLException {
        try {
            System.out.print("Enter payment ID: ");
            int paymentId = Integer.parseInt(scanner.nextLine());
            
            System.out.print("Enter transaction ID: ");
            String transactionId = scanner.nextLine();
            
            System.out.print("Enter payment method: ");
            String paymentMethod = scanner.nextLine();
            
            String query = """
                UPDATE Payment 
                SET payment_status = 'confirmed',
                    transactionID = ?,
                    payment_date = CURRENT_DATE,
                    payment_method = ?
                WHERE paymentID = ? AND payment_status = 'pending'
            """;
            
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, transactionId);
            pstmt.setString(2, paymentMethod);
            pstmt.setInt(3, paymentId);
            
            int updated = pstmt.executeUpdate();
            if (updated > 0) {
                System.out.println("Payment processed successfully!");
            } else {
                System.out.println("Payment not found or already processed.");
            }
        } catch (Exception e) {
            System.out.println("Error processing payment: " + e.getMessage());
        }
    }

    private static void modifyBooking(Connection conn) throws SQLException {
        try {
            System.out.print("Enter booking ID to modify: ");
            int bookingId = Integer.parseInt(scanner.nextLine());
            
            // Show current booking details
            System.out.println("Leave blank if no change is needed for the following fields:");
            
            System.out.print("Enter new guest ID: ");
            String guestIdStr = scanner.nextLine();
            Integer guestId = guestIdStr.isEmpty() ? null : Integer.parseInt(guestIdStr);
            
            System.out.print("Enter new check-in date (YYYY-MM-DD): ");
            String checkInDate = scanner.nextLine();
            
            System.out.print("Enter new check-out date (YYYY-MM-DD): ");
            String checkOutDate = scanner.nextLine();
            
            System.out.print("Enter new number of guests: ");
            String guestsStr = scanner.nextLine();
            Integer numberOfGuests = guestsStr.isEmpty() ? null : Integer.parseInt(guestsStr);
            
            System.out.print("Enter new status (pending/confirmed): ");
            String status = scanner.nextLine();
            
            String query = """
                UPDATE Booking b
                JOIN ManagedBy mb ON b.bookingID = mb.bookingID
                SET 
                    b.guestID = COALESCE(?, b.guestID),
                    b.check_in_date = COALESCE(?, b.check_in_date),
                    b.check_out_date = COALESCE(?, b.check_out_date),
                    b.number_of_guests = COALESCE(?, b.number_of_guests),
                    mb.status = COALESCE(?, mb.status)
                WHERE b.bookingID = ?
                AND mb.status NOT IN ('checked_in', 'checked_out')
            """;
            
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setObject(1, guestId);
            pstmt.setString(2, checkInDate.isEmpty() ? null : checkInDate);
            pstmt.setString(3, checkOutDate.isEmpty() ? null : checkOutDate);
            pstmt.setObject(4, numberOfGuests);
            pstmt.setString(5, status.isEmpty() ? null : status);
            pstmt.setInt(6, bookingId);
            
            int updated = pstmt.executeUpdate();
            if (updated > 0) {
                System.out.println("Booking modified successfully!");
            } else {
                System.out.println("Booking not found or cannot be modified.");
            }
        } catch (Exception e) {
            System.out.println("Error modifying booking: " + e.getMessage());
        }
    }

    private static void deleteBooking(Connection conn) throws SQLException {
        try {
            System.out.print("Enter booking ID to delete: ");
            int bookingId = Integer.parseInt(scanner.nextLine());
            
            conn.setAutoCommit(false);
            
            // Check if payment is confirmed
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
                System.out.println("Cannot delete booking: Payment is already confirmed");
                conn.rollback();
                return;
            }
            
            // Delete booked rooms
            String deleteBookedRooms = "DELETE FROM BookedRooms WHERE bookingID = ?";
            pstmt = conn.prepareStatement(deleteBookedRooms);
            pstmt.setInt(1, bookingId);
            pstmt.executeUpdate();
            
            // Delete the booking
            String deleteBooking = "DELETE FROM Booking WHERE bookingID = ?";
            pstmt = conn.prepareStatement(deleteBooking);
            pstmt.setInt(1, bookingId);
            
            int deleted = pstmt.executeUpdate();
            if (deleted > 0) {
                conn.commit();
                System.out.println("Booking deleted successfully!");
            } else {
                conn.rollback();
                System.out.println("Booking not found.");
            }
        } catch (Exception e) {
            conn.rollback();
            System.out.println("Error deleting booking: " + e.getMessage());
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
            
            System.out.println("\nAll Bookings:");
            System.out.printf("%-8s %-20s %-15s %-12s %-12s %-8s %-15s %-10s %-15s%n",
                "ID", "Guest Info", "Room", "Check In", "Check Out", "Guests", "Status", "Type", "Nationality");
            System.out.println("-".repeat(120));
            
            while (rs.next()) {
                System.out.printf("%-8d %-20s %-15s %-12s %-12s %-8d %-15s %-10s %-15s%n",
                    rs.getInt("bookingID"),
                    String.format("[%d] %s", rs.getInt("guestID"), rs.getString("passport_number")),
                    rs.getString("room_no"),
                    rs.getDate("check_in_date"),
                    rs.getDate("check_out_date"),
                    rs.getInt("number_of_guests"),
                    rs.getString("booking_status"),
                    rs.getString("room_type"),
                    rs.getString("nationality"));
            }
        } catch (Exception e) {
            System.out.println("Error viewing bookings: " + e.getMessage());
        }
    }

    private static void assignHousekeepingTask(Connection conn) throws SQLException {
        try {
            System.out.print("Enter room ID: ");
            int roomId = Integer.parseInt(scanner.nextLine());
            
            System.out.print("Enter housekeeper ID: ");
            int housekeeperId = Integer.parseInt(scanner.nextLine());
            
            System.out.print("Enter receptionist ID: ");
            int receptionistId = Integer.parseInt(scanner.nextLine());
            
            System.out.print("Enter scheduled time (YYYY-MM-DD HH:MM:SS): ");
            String scheduledTime = scanner.nextLine();
            
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
            System.out.println("Housekeeping task assigned successfully! Schedule ID: " + scheduleId);
            
        } catch (Exception e) {
            System.out.println("Error assigning housekeeping task: " + e.getMessage());
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
            
            System.out.println("\nHousekeepers Status:");
            System.out.printf("%-8s %-20s %-10s %-15s %-15s %-20s%n",
                "ID", "Name", "Shift", "Tasks", "Status", "Next Task");
            System.out.println("-".repeat(90));
            
            while (rs.next()) {
                System.out.printf("%-8d %-20s %-10s %-15d %-15s %-20s%n",
                    rs.getInt("housekeeperID"),
                    rs.getString("first_name") + " " + rs.getString("last_name"),
                    rs.getString("shift"),
                    rs.getInt("assigned_tasks"),
                    rs.getString("availability"),
                    rs.getTimestamp("next_task_time"));
            }
        } catch (Exception e) {
            System.out.println("Error viewing housekeeper records: " + e.getMessage());
        }
    }
} 
