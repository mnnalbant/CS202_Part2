import java.sql.*;
import javax.swing.*;

public class Menu {
    private static JFrame frame;
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            frame = new JFrame("Hotel Management System");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            showMainMenu();
            frame.pack();
            frame.setVisible(true);
        });
    }
    
    private static void showMainMenu() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        JButton guestButton = new JButton("Guest");
        guestButton.addActionListener(e -> showGuestOptions());
        panel.add(guestButton);
        
        JButton adminButton = new JButton("Administrator");
        adminButton.addActionListener(e -> showAdminOptions());
        panel.add(adminButton);
        
        JButton receptionistButton = new JButton("Receptionist");
        receptionistButton.addActionListener(e -> showReceptionistOptions());
        panel.add(receptionistButton);
        
        JButton housekeepingButton = new JButton("Housekeeping");
        housekeepingButton.addActionListener(e -> showHousekeepingOptions());
        panel.add(housekeepingButton);
        
        JButton exitButton = new JButton("Exit");
        exitButton.addActionListener(e -> {
            System.out.println("Thank you for using the Hotel Management System!");
            System.exit(0);
        });
        panel.add(exitButton);
        
        frame.getContentPane().removeAll();
        frame.getContentPane().add(panel);
        frame.revalidate();
        frame.repaint();
    }
    
    private static void showAdminOptions() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        // Admin options
        String[] options = {
            "Add Room",
            "Delete Room",
            "Manage Room Status",
            "Add User Account",
            "View User Accounts",
            "Generate Revenue Report",
            "View All Booking Records",
            "View All Housekeeping Records",
            "View Most Booked Room Types",
            "View All Employees"
        };
        
        for (int i = 0; i < options.length; i++) {
            String option = options[i];
            JButton optionButton = new JButton(option);
            final int choice = i + 1; // Choices start from 1
            optionButton.addActionListener(e -> handleAdminChoice(choice));
            panel.add(optionButton);
        }
        
        JButton backButton = new JButton("Back to Main Menu");
        backButton.addActionListener(e -> showMainMenu());
        panel.add(backButton);
        
        frame.getContentPane().removeAll();
        frame.getContentPane().add(panel);
        frame.revalidate();
        frame.repaint();
    }
    
    private static void showGuestOptions() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        // Guest options
        String[] options = {
            "Add New Booking",
            "View Available Rooms",
            "View My Bookings",
            "Cancel Booking"
        };
        
        for (int i = 0; i < options.length; i++) {
            String option = options[i];
            JButton optionButton = new JButton(option);
            final int choice = i + 1; // Choices start from 1
            optionButton.addActionListener(e -> handleGuestChoice(choice));
            panel.add(optionButton);
        }
        
        JButton backButton = new JButton("Back to Main Menu");
        backButton.addActionListener(e -> showMainMenu());
        panel.add(backButton);
        
        frame.getContentPane().removeAll();
        frame.getContentPane().add(panel);
        frame.revalidate();
        frame.repaint();
    }
    
    private static void showReceptionistOptions() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        // Receptionist options
        String[] options = {
            "Add New Booking",
            "Modify Booking",
            "Delete Booking",
            "View Bookings",
            "Process Payment",
            "Assign Housekeeping Task",
            "View Housekeepers Records"
        };
        
        for (int i = 0; i < options.length; i++) {
            String option = options[i];
            JButton optionButton = new JButton(option);
            final int choice = i + 1; // Choices start from 1
            optionButton.addActionListener(e -> handleReceptionistChoice(choice));
            panel.add(optionButton);
        }
        
        JButton backButton = new JButton("Back to Main Menu");
        backButton.addActionListener(e -> showMainMenu());
        panel.add(backButton);
        
        frame.getContentPane().removeAll();
        frame.getContentPane().add(panel);
        frame.revalidate();
        frame.repaint();
    }
    
    private static void showHousekeepingOptions() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        // Housekeeping options
        String[] options = {
            "View Pending Tasks",
            "View Completed Tasks",
            "Update Task Status",
            "View Cleaning Schedule"
        };
        
        for (int i = 0; i < options.length; i++) {
            String option = options[i];
            JButton optionButton = new JButton(option);
            final int choice = i + 1; // Choices start from 1
            optionButton.addActionListener(e -> handleHousekeepingChoice(choice));
            panel.add(optionButton);
        }
        
        JButton backButton = new JButton("Back to Main Menu");
        backButton.addActionListener(e -> showMainMenu());
        panel.add(backButton);
        
        frame.getContentPane().removeAll();
        frame.getContentPane().add(panel);
        frame.revalidate();
        frame.repaint();
    }
    
    private static void handleAdminChoice(int choice) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            AdminOperations.handleAdminChoice(choice, conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private static void handleGuestChoice(int choice) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            GuestOperations.handleGuestChoice(choice, conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private static void handleReceptionistChoice(int choice) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            ReceptionistOperations.handleReceptionistChoice(choice, conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private static void handleHousekeepingChoice(int choice) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            HousekeepingOperations.handleHousekeepingChoice(choice, conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}