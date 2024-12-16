import java.sql.*;
import java.util.Scanner;

public class Menu {
    private static final Scanner scanner = new Scanner(System.in);
    
    public static void main(String[] args) {
        while (true) {
            try {
                showMainMenu();
                int choice = getUserChoice();
                
                switch (choice) {
                    case 1:
                        guestMenu();
                        break;
                    case 2:
                        adminMenu();
                        break;
                    case 3:
                        receptionistMenu();
                        break;
                    case 4:
                        housekeepingMenu();
                        break;
                    case 5:
                        System.out.println("Thank you for using the Hotel Management System!");
                        return;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("An error occurred: " + e.getMessage());
            }
        }
    }
    
    private static void showMainMenu() {
        System.out.println("\n=== Hotel Management System ===");
        System.out.println("1. Guest");
        System.out.println("2. Administrator");
        System.out.println("3. Receptionist");
        System.out.println("4. Housekeeping");
        System.out.println("5. Exit");
        System.out.print("Enter your choice: ");
    }
    
    private static int getUserChoice() {
        while (!scanner.hasNextInt()) {
            System.out.println("Please enter a valid number.");
            scanner.next();
        }
        return scanner.nextInt();
    }
    
    private static void guestMenu() throws SQLException {
        while (true) {
            System.out.println("\n=== Guest Menu ===");
            System.out.println("1. Add New Booking");
            System.out.println("2. View Available Rooms");
            System.out.println("3. View My Bookings");
            System.out.println("4. Cancel Booking");
            System.out.println("5. Return to Main Menu");
            
            int choice = getUserChoice();
            if (choice == 5) break;
            
            handleGuestChoice(choice);
        }
    }
    
    private static void adminMenu() throws SQLException {
        while (true) {
            System.out.println("\n=== Administrator Menu ===");
            System.out.println("1. Add Room");
            System.out.println("2. Delete Room");
            System.out.println("3. Manage Room Status");
            System.out.println("4. Add User Account");
            System.out.println("5. View User Accounts");
            System.out.println("6. Generate Revenue Report");
            System.out.println("7. View All Booking Records");
            System.out.println("8. View All Housekeeping Records");
            System.out.println("9. View Most Booked Room Types");
            System.out.println("10. View All Employees");
            System.out.println("11. Return to Main Menu");
            
            int choice = getUserChoice();
            if (choice == 11) break;
            
            handleAdminChoice(choice);
        }
    }
    
    private static void receptionistMenu() throws SQLException {
        while (true) {
            System.out.println("\n=== Receptionist Menu ===");
            System.out.println("1. Add New Booking");
            System.out.println("2. Modify Booking");
            System.out.println("3. Delete Booking");
            System.out.println("4. View Bookings");
            System.out.println("5. Process Payment");
            System.out.println("6. Assign Housekeeping Task");
            System.out.println("7. View All Housekeepers Records");
            System.out.println("8. Return to Main Menu");
            
            int choice = getUserChoice();
            if (choice == 8) break;
            
            handleReceptionistChoice(choice);
        }
    }
    
    private static void housekeepingMenu() throws SQLException {
        while (true) {
            System.out.println("\n=== Housekeeping Menu ===");
            System.out.println("1. View Pending Tasks");
            System.out.println("2. View Completed Tasks");
            System.out.println("3. Update Task Status");
            System.out.println("4. View Cleaning Schedule");
            System.out.println("5. Return to Main Menu");
            
            int choice = getUserChoice();
            if (choice == 5) break;
            
            handleHousekeepingChoice(choice);
        }
    }
    

    private static void handleGuestChoice(int choice) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            GuestOperations.handleGuestChoice(choice, conn);
        }
    }
    
    private static void handleAdminChoice(int choice) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            AdminOperations.handleAdminChoice(choice, conn);
        }
    }
    
    private static void handleReceptionistChoice(int choice) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            ReceptionistOperations.handleReceptionistChoice(choice, conn);
        }
    }
    
    private static void handleHousekeepingChoice(int choice) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            HousekeepingOperations.handleHousekeepingChoice(choice, conn);
        }
    }
}