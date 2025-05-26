package com.example.mcp.client;

import com.example.mcp.model.Protocol;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {
    private String serverIp;
    private int serverPort;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private volatile boolean running = true; // Flag to control loops and threads

    public Client(String serverIp, int serverPort) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }

    public void startClient() {
        try {
            socket = new Socket(serverIp, serverPort);
            System.out.println("Connected to server: " + serverIp + ":" + serverPort);

            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Thread for listening to server messages
            Thread listenerThread = new Thread(this::listenToServer);
            listenerThread.start();

            // Main thread for sending user messages
            sendMessages();

        } catch (UnknownHostException e) {
            System.err.println("Client error: Server not found at " + serverIp + ":" + serverPort + ". " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Client error: I/O exception during connection or stream setup to " + serverIp + ":" + serverPort + ". " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    private void listenToServer() {
        try {
            String messageFromServer;
            while (running && (messageFromServer = in.readLine()) != null) {
                System.out.println("Received from Server: " + messageFromServer);
            }
        } catch (IOException e) {
            if (running) { // Avoid error message if shutdown initiated by client
                System.err.println("Client error: Lost connection to server or error reading: " + e.getMessage());
            }
        } finally {
            // If the loop exits, it means server disconnected or an error occurred.
            // Trigger shutdown from the client side if not already initiated.
            if (running) {
                System.out.println("Connection to server closed.");
                running = false; // Signal other parts of the client to shut down
            }
        }
    }

    private void sendMessages() {
        try (BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in))) {
            String userInput;
            System.out.println("Enter commands (e.g., 'load <modelId>', 'get <modelId>', 'update <modelId> <jsonData>', 'exit'):");
            while (running && (userInput = consoleReader.readLine()) != null) {
                userInput = userInput.trim();
                if ("exit".equalsIgnoreCase(userInput) || "quit".equalsIgnoreCase(userInput)) {
                    running = false;
                    break;
                }

                String messageToSend = null;
                String[] parts = userInput.split(" ", 3); // Split into command, modelId, and optional jsonData
                String command = parts.length > 0 ? parts[0].toLowerCase() : "";

                if ("load".equals(command) && parts.length == 2) {
                    messageToSend = Protocol.LOAD_MODEL_PREFIX + parts[1].trim();
                } else if ("get".equals(command) && parts.length == 2) {
                    messageToSend = Protocol.GET_CONTEXT_PREFIX + parts[1].trim();
                } else if ("update".equals(command) && parts.length == 3) {
                    // Ensure modelId is not empty
                    String modelId = parts[1].trim();
                    if (modelId.isEmpty()) {
                        System.out.println("Client: Model ID cannot be empty for update command.");
                        continue;
                    }
                    messageToSend = Protocol.UPDATE_CONTEXT_PREFIX + modelId + ":" + parts[2].trim();
                } else {
                    System.out.println("Client: Unknown command or incorrect format. Available: load, get, update, exit");
                    continue; 
                }

                if (messageToSend != null && out != null && !socket.isClosed()) {
                    out.println(messageToSend);
                    System.out.println("Sent to Server: " + messageToSend);
                } else if (out == null || socket.isClosed()) {
                    System.err.println("Client error: Cannot send message. Socket is closed or output stream is null.");
                    running = false; // Connection lost or error, signal shutdown
                }
            }
        } catch (IOException e) {
            System.err.println("Client error: Error reading from console: " + e.getMessage());
        } finally {
            running = false; // Ensure shutdown if loop exits
        }
    }

    private void shutdown() {
        running = false; // Ensure all loops and threads are signaled to stop
        System.out.println("Client shutting down...");
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Client error: Exception while closing resources: " + e.getMessage());
        }
        System.out.println("Client shutdown complete.");
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java com.example.mcp.client.Client <server-ip> <server-port>");
            System.out.println("Example: java com.example.mcp.client.Client 127.0.0.1 12345");
            return;
        }
        String ip = args[0];
        int port;
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number: " + args[1]);
            return;
        }

        Client client = new Client(ip, port);
        client.startClient();
    }
}
