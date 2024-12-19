import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Test {

    public static void main(String[] args) {
        String serverHost = "lohani.cs.williams.edu"; // Server hostname
        int serverPort = 8900; // DestServer port
        int numClients = 1; // Number of clients to simulate
        int numCommands = 20; // Number of commands per client
        ExecutorService executor = Executors.newFixedThreadPool(numClients);

        // Registered users
        String[][] users = {
            {"alice", "password123"},
            {"bob", "secure456"},
            {"charlie", "mypassword789"},
            // {"diana", "qwerty123"},
            // {"eve", "abc123xyz"}
        };

        for (int i = 0; i < numClients; i++) {
            int clientId = i;
            // Assign a user in round-robin fashion
            String[] user = users[1];
            //String[] user = users[clientId % users.length];
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

            // Login
            System.out.println("Client " + clientId + " attempting to log in with username: " + username);
            long totalStartTime = System.nanoTime();
            out.println(username + ":" + password);
            String response = in.readLine();
            long totalEndTime = System.nanoTime();

            long totalTime = (totalEndTime - totalStartTime) / 1_000_000; // Convert to ms
            System.out.println("Client " + clientId + " - Total Time for Login: " + totalTime + " ms");
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

                //Send the commands to the Server:
                long startTime = System.nanoTime();
                out.println(command);
                //Receive the response from the server:
                response = in.readLine();
                long endTime = System.nanoTime();

                //Show the times:
                System.out.println("Client " + clientId + " - Command: " + command);
                System.out.println("Client " + clientId + " - Response: " + response);
                System.out.println("Client " + clientId + " - Time: " + (endTime - startTime) / 1e6 + " ms");
            }

            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
