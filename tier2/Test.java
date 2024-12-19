import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.*;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Test {
    private static final String ENCRYPTION_KEY = "1234567890123456"; // 16-byte key for AES-128

    public static void main(String[] args) {
        String serverHost = "lohani.cs.williams.edu";
        int serverPort = 8900;
        int numClients = 2; // Number of clients to simulate
        int numCommands = 10; // Number of commands per client
        ExecutorService executor = Executors.newFixedThreadPool(numClients);

        // Registered users
        String[][] users = {
            {"alice", "password123"},
            {"bob", "secure456"},
            {"charlie", "mypassword789"}
        };

        for (int i = 0; i < numClients; i++) {
            int clientId = i;
            ///String[] user = users[1];
            String[] user = users[clientId % users.length]; // Assign user in round-robin
            executor.submit(() -> runTest(clientId, serverHost, serverPort, user[0], user[1], numCommands));
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.MINUTES)) {
                System.err.println("Some threads did not finish in time.");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("All client simulations completed.");
    }

    private static void runTest(int clientId, String serverHost, int serverPort, String username, String password, int numCommands) {
        try {
            Socket socket = new Socket(serverHost, serverPort);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Measure total time for encrypting, sending, and receiving credentials
            String credentials = username + ":" + password;

            long totalStartTime = System.nanoTime();
            String encryptedCredentials = encrypt(credentials); // Encrypt credentials
            out.println(encryptedCredentials); // Send credentials

            String encryptedResponse = in.readLine(); // Receive encrypted response
            String response = decrypt(encryptedResponse); // Decrypt response
            long totalEndTime = System.nanoTime();

            long totalTime = (totalEndTime - totalStartTime) / 1_000_000; // Convert to milliseconds
            System.out.println("Client " + clientId + " - Total Time for Login (encrypt, send, receive, decrypt): " + totalTime + " ms");

            if (!"Login successful".equalsIgnoreCase(response)) {
                System.out.println("Client " + clientId + " - Login failed: " + response);
                socket.close();
                return;
            }

            System.out.println("Client " + clientId + " logged in successfully as " + username);

            // Execute commands
            for (int i = 0; i < numCommands; i++) {
                String command;
                if (i % 3 == 0) {
                    command = "balance";
                } else if (i % 3 == 1) {
                    command = "deposit " + (100 * (clientId + 1));
                } else {
                    command = "withdraw " + (50 * (clientId + 1));
                }

                // Measure total time for encrypting, sending, and receiving a command
                totalStartTime = System.nanoTime();
                String encryptedCommand = encrypt(command); // Encrypt command
                out.println(encryptedCommand); // Send command

                encryptedResponse = in.readLine(); // Receive encrypted response
                response = decrypt(encryptedResponse); // Decrypt response
                totalEndTime = System.nanoTime();

                totalTime = (totalEndTime - totalStartTime) / 1_000_000; // Convert to milliseconds
                System.out.println("Client " + clientId + " - Command: " + command);
                System.out.println("Client " + clientId + " - Response: " + response);
                System.out.println("Client " + clientId + " - Total Time (encrypt, send, receive, decrypt): " + totalTime + " ms");
            }

            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
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
}
