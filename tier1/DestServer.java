import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.concurrent.Semaphore;

public class DestServer {

    private static Connection c = null;
    private static final Semaphore SP = new Semaphore(1);

    static {
        connectDB();
    }

    public static void main(String[] args) {
        // Open port to listen for clients on
        int port = 8800;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("DestServer is listening on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client connected");

                new Thread(() -> handleClient(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to handle client login and commands
    private static void handleClient(Socket socket) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            // First, authenticate client login credentials
            String credentials = in.readLine();
            String[] parts = credentials.split(":");
            String username = parts[0];
            String password = parts[1];

            if (!authenticate(username, password)) {
                out.println("Login failed");
                return;
            }

            out.println("Login successful");

            // Next, take in client commands and process them
            String command;
            while ((command = in.readLine()) != null) {
                System.out.println("Received command: " + command);
                String[] commandParts = command.split(" ");
                String response;

                switch (commandParts[0].toLowerCase()) {
                    case "balance":
                        if (commandParts.length != 1) {
                            response = "Error: 'balance' command does not take arguments.";
                        } else {
                            response = getBalance(username);
                        }
                        break;
                    case "deposit":
                        if (commandParts.length != 2) {
                            response = "Error: 'deposit' command requires an amount as argument.";
                        } else {
                            try {
                                int depositAmount = Integer.parseInt(commandParts[1]);
                                response = deposit(username, depositAmount);
                            } catch (NumberFormatException e) {
                                response = "Error: Invalid amount for deposit.";
                            }
                        }
                        break;
                    case "withdraw":
                        if (commandParts.length != 2) {
                            response = "Error: 'withdraw' command requires an amount as argument.";
                        } else {
                            try {
                                int withdrawAmount = Integer.parseInt(commandParts[1]);
                                response = withdraw(username, withdrawAmount);
                            } catch (NumberFormatException e) {
                                response = "Error: Invalid amount for withdrawal.";
                            }
                        }
                        break;
                    default:
                        response = "Error: Unknown command.";
                        break;
                }
                out.println(response);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                // Close the socket after handling the client
                socket.close();
                System.out.println("Client disconnected");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Connect to backend database
    private static void connectDB() {
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:bank.db");
            c.setAutoCommit(false);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    // Check username and password credentials to authenticate login
    private static boolean authenticate(String username, String password) {
        try (PreparedStatement stmt = c.prepareStatement("SELECT * FROM BANK WHERE USERNAME = ? AND PASSWORD = ?")) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String getBalance(String username) {
        try (PreparedStatement stmt = c.prepareStatement("SELECT BALANCE FROM BANK WHERE USERNAME = ?")) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return "Balance: " + rs.getInt("BALANCE");
            } else {
                return "User not found";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error retrieving balance";
        }
    }

    private static String deposit(String username, int amount) {
        try {
            SP.acquire();
            PreparedStatement stmt = c.prepareStatement("UPDATE BANK SET BALANCE = BALANCE + ? WHERE USERNAME = ?");
            stmt.setInt(1, amount);
            stmt.setString(2, username);
            int rowsAffected = stmt.executeUpdate();
            c.commit();
            if (rowsAffected > 0) {
                return "Deposit successful";
            } else {
                return "User not found";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error during deposit";
        } finally {
            SP.release();
        }
    }

    private static String withdraw(String username, int amount) {
        try {
            SP.acquire();
            PreparedStatement stmt = c.prepareStatement("SELECT BALANCE FROM BANK WHERE USERNAME = ?");
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int currentBalance = rs.getInt("BALANCE");
                if (currentBalance >= amount) {
                    stmt = c.prepareStatement("UPDATE BANK SET BALANCE = BALANCE - ? WHERE USERNAME = ?");
                    stmt.setInt(1, amount);
                    stmt.setString(2, username);
                    int rowsAffected = stmt.executeUpdate();
                    c.commit();
                    if (rowsAffected > 0) {
                        return "Withdrawal successful";
                    } else {
                        return "User not found";
                    }
                } else {
                    return "Insufficient funds";
                }
            } else {
                return "User not found";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error during withdrawal";
        } finally {
            SP.release();
        }
    }
}
