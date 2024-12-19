import java.io.*;
import java.net.*;


public class Client {
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

            // Send credentials to server
            out.println(credentials);

            // Read response from the server
            String serverResponse = in.readLine();
            System.out.println("Server: " + serverResponse);
            if (serverResponse.equals("Login failed")) {
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
                out.println(command);
                System.out.println("Server: " + in.readLine());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
