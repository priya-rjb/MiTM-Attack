import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.Base64;
import java.util.concurrent.Semaphore;

public class DestServer {
    private static final String ENCRYPTION_KEY = "1234567890123456"; // 16-byte key for AES-128
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
            
            // Read the encrypted credentials from the MITM server
            String encryptedCredentials = in.readLine();
            String credentials;
            try {
                credentials = decrypt(encryptedCredentials);  // Handling the decrypt exception here
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            String[] parts = credentials.split(":");
            String username = parts[0];
            String password = parts[1];
            String response;

            if (!authenticate(username, password)) {
                response = "Login failed";
                // Encrypt the response and send it back to the MITM server
                String encryptedResponse;
                try {
                    encryptedResponse = encrypt(response);  // Handling the encrypt exception here
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
                out.println(encryptedResponse);
                return;
            } 
            
            response = "Login successful";

            // Encrypt the response and send it back to the MITM server
            String encryptedResponse;
            try {
                encryptedResponse = encrypt(response);  // Handling the encrypt exception here
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            out.println(encryptedResponse);

            String encryptedCommand;
            while ((encryptedCommand = in.readLine()) != null) {
                String command;
                try {
                    command = decrypt(encryptedCommand);  // Handling the decrypt exception here
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;  // Skip this iteration if decryption fails
                }
                System.out.println("Received command: " + command);
                String[] commandParts = command.split(" ");

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

                // Encrypt the response and send it back to the MITM server
                try {
                    encryptedResponse = encrypt(response);  // Handling the encrypt exception here
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;  // Skip this iteration if encryption fails
                }
                out.println(encryptedResponse);
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
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

    // AES encryption method
    public static String encrypt(String data) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedData = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encryptedData);
    }

    // AES decryption method
    public static String decrypt(String encryptedData) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedData = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
        return new String(decryptedData);
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
