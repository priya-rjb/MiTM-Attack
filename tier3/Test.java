import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.*;
import java.util.Base64;
import javax.net.ssl.*;
import java.security.KeyStore;
import java.util.concurrent.*;

public class Test {
    private static final String ENCRYPTION_KEY = "1234567890123456"; // 16-byte key for AES-128

    public static void main(String[] args) {
        String serverHost = "lohani.cs.williams.edu";
        int serverPort = 8800;
        int numClients = 3; // Number of clients to simulate
        int numCommands = 20; // Number of commands per client
        ExecutorService executor = Executors.newFixedThreadPool(numClients);

        // Registered users
        String[][] users = {
            {"alice", "password123"},
            {"bob", "secure456"},
            {"charlie", "mypassword789"}
        };

        for (int i = 0; i < numClients; i++) {
            int clientId = i;
            //String[] user = users[1];
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
            // Load truststore
            KeyStore trustStore = KeyStore.getInstance("JKS");
            FileInputStream trustStoreFile = new FileInputStream("client-truststore.jks");
            trustStore.load(trustStoreFile, "password".toCharArray());
            trustStoreFile.close();

            // Initialize TrustManager
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);

            // Initialize SSLContext
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);

            // Create SSLSocket
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(serverHost, serverPort);
            sslSocket.startHandshake();

            PrintWriter out = new PrintWriter(sslSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));

            // Encrypt, send credentials, and measure total time
            String credentials = username + ":" + password;
            long totalStartTime = System.nanoTime();
            String encryptedCredentials = encrypt(credentials);
            out.println(encryptedCredentials);

            String encryptedResponse = in.readLine();
            String response = decrypt(encryptedResponse);
            long totalEndTime = System.nanoTime();

            long totalTime = (totalEndTime - totalStartTime) / 1_000_000; // Convert to ms
            System.out.println("Client " + clientId + " - Total Time for Login (encrypt, send, receive, decrypt): " + totalTime + " ms");

            if (!"Login successful".equalsIgnoreCase(response)) {
                System.out.println("Client " + clientId + " - Login failed: " + response);
                sslSocket.close();
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

                totalStartTime = System.nanoTime();
                String encryptedCommand = encrypt(command);
                out.println(encryptedCommand);

                encryptedResponse = in.readLine();
                response = decrypt(encryptedResponse);
                totalEndTime = System.nanoTime();

                totalTime = (totalEndTime - totalStartTime) / 1_000_000; // Convert to ms
                System.out.println("Client " + clientId + " - Command: " + command);
                System.out.println("Client " + clientId + " - Response: " + response);
                System.out.println("Client " + clientId + " - Total Time (encrypt, send, receive, decrypt): " + totalTime + " ms");
            }

            sslSocket.close();
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
