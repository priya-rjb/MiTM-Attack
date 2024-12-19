import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.*;
import java.util.Base64;

public class Client {
    private static final String ENCRYPTION_KEY = "1234567890123456"; // 16-byte key for AES-128

    public static void main(String[] args) {
        String serverHost = "lohani.cs.williams.edu";
        int serverPort = 8900;

        try {
            // Connect to the server
            Socket socket = new Socket(serverHost, serverPort);
            System.out.println("Connected to server.");

            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Get username and password from the user
            System.out.print("Enter username: ");
            String username = stdIn.readLine();
            System.out.print("Enter password: ");
            String password = stdIn.readLine();

            // Combine credentials into a single message
            String credentials = username + ":" + password;

            // Encrypt the credentials
            String encryptedCredentials;
            try {
                encryptedCredentials = encrypt(credentials);  // Handle encryption exception
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            // Send the encrypted credentials to the server
            out.println(encryptedCredentials);

            // Receive the encrypted response from the server
            String encryptedResponse = in.readLine();
            String response;
            try {
                response = decrypt(encryptedResponse);  // Handle decryption exception
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            System.out.println("Server: " + response);
            if (response.equals("Login failed")) {
                System.out.println("Server closed the connection.");
                return;
            }

            // Loop to accept commands (e.g. withdraw, deposit, balance)
            String command;
            while (true) {
                System.out.print("Enter command (or 'exit' to quit): ");
                command = stdIn.readLine();
                if ("exit".equalsIgnoreCase(command)) {
                    break;
                }

                // Encrypt the command
                String encryptedCommand;
                try {
                    encryptedCommand = encrypt(command);  // Handle encryption exception
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;  // Skip if encryption fails
                }
                out.println(encryptedCommand);

                // Receive the encrypted response from the server
                encryptedResponse = in.readLine();
                try {
                    response = decrypt(encryptedResponse);  // Handle decryption exception
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;  // Skip if decryption fails
                }

                System.out.println("Server: " + response);
            }
        } catch (IOException e) {
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
