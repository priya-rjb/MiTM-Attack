import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.*;
import java.util.Base64;
import javax.net.ssl.*;
import java.security.KeyStore;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.KeyStoreException; 
import java.util.concurrent.*;

public class Client {
    private static final String ENCRYPTION_KEY = "1234567890123456"; // 16-byte key for AES-128

    public static void main(String[] args) {
        String serverHost = "lohani.cs.williams.edu";
        int serverPort = 8900;

        try {
            // Load the trust store (the client's trusted CA certificates)
            KeyStore trustStore = null;
            try {
                trustStore = KeyStore.getInstance("JKS");
                FileInputStream trustStoreFile = new FileInputStream("client-truststore.jks");
                trustStore.load(trustStoreFile, "password".toCharArray());
                trustStoreFile.close();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            // Initialize TrustManager with the trusted CAs
            TrustManagerFactory tmf = null;
            try {
                tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(trustStore);
            } catch (NoSuchAlgorithmException | KeyStoreException e) {
                e.printStackTrace();
                return;
            }

            SSLContext sslContext = null;
            try {
                sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, tmf.getTrustManagers(), null);
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                e.printStackTrace();
                return;
            }

            // Create the SSLSocketFactory
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            // Create a socket and set a timeout for SSL handshake
            SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(serverHost, serverPort);

            // Set the socket timeout for reading (this will apply after the handshake)
            sslSocket.setSoTimeout(10000);  // Timeout for I/O operations (in milliseconds)

            // Start handshake in a separate thread with a timeout
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<Void> future = executor.submit(() -> {
                try {
                    sslSocket.startHandshake();  // Start the SSL handshake
                    System.out.println("SSL handshake completed.");
                } catch (SSLHandshakeException e) {
                    System.err.println("SSL Handshake failed: " + e.getMessage());
                    return null;
                } catch (IOException e) {
                    System.err.println("SSL Handshake failed: " + e.getMessage());
                    return null;
                }
                return null;
            });

            try {
                future.get(10, TimeUnit.SECONDS);  // Wait for the handshake to complete (with a 10 second timeout)
            } catch (TimeoutException e) {
                System.err.println("SSL handshake timed out after 10 seconds.");
                sslSocket.close();
                return;
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                sslSocket.close();
                return;
            } finally {
                executor.shutdown();
            }

            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
            PrintWriter out = new PrintWriter(sslSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));

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
