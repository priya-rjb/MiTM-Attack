import java.io.*;
import java.net.*;
import java.util.logging.*;

public class MITMServer {
    private static final Logger LOGGER = Logger.getLogger(MITMServer.class.getName());

    public static void main(String[] args) {
        int port = 8900; // Port for clients to connect

        // Configure logger
        configureLogger();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("MITM Server is listening on port " + port);
            LOGGER.info("MITM Server started and listening on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected");
                LOGGER.info("New client connected");

                // Handle client connection in a new thread
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            LOGGER.severe("MITM Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (
            Socket destSocket = new Socket("localhost", 8800); // Connect to DestServer
            BufferedReader clientIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter clientOut = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader destIn = new BufferedReader(new InputStreamReader(destSocket.getInputStream()));
            PrintWriter destOut = new PrintWriter(destSocket.getOutputStream(), true)
        ) {
            Thread clientToDest = new Thread(() -> {
                try {
                    String clientMessage;
                    while ((clientMessage = clientIn.readLine()) != null) {
                        LOGGER.info("MITM Received from client: " + clientMessage); // Log client message
                        destOut.println(clientMessage); // Send to DestServer
                    }
                } catch (IOException e) {
                    LOGGER.warning("Client disconnected.");
                }
            });

            Thread destToClient = new Thread(() -> {
                try {
                    String destMessage;
                    while ((destMessage = destIn.readLine()) != null) {
                        LOGGER.info("MITM Received from DestServer: " + destMessage); // Log dest server message
                        clientOut.println(destMessage); // Send back to Client
                    }
                } catch (IOException e) {
                    LOGGER.warning("DestServer disconnected.");
                }
            });

            clientToDest.start();
            destToClient.start();

            clientToDest.join();
            destToClient.join();

        } catch (IOException | InterruptedException e) {
            LOGGER.severe("Error during client handling: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                LOGGER.severe("Error closing client socket: " + e.getMessage());
            }
            LOGGER.info("Client connection closed.");
        }
    }

    private static void configureLogger() {
        try {
            // Set up the file handler and formatter
            FileHandler fileHandler = new FileHandler("mitm.log", true); // Append to log file
            fileHandler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fileHandler);

            // Remove the console handler if you don't want console logging
            for (Handler handler : LOGGER.getHandlers()) {
                if (handler instanceof ConsoleHandler) {
                    LOGGER.removeHandler(handler);
                }
            }
        } catch (IOException ex) {
            LOGGER.severe("Failed to configure logger: " + ex.getMessage());
        }
    }

}
