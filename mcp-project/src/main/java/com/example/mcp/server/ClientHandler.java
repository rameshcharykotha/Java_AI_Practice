package com.example.mcp.server;

import com.example.mcp.model.Protocol;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private Server server;
    private PrintWriter out;
    private BufferedReader in;

    public ClientHandler(Socket socket, Server server) {
        this.clientSocket = socket;
        this.server = server;
        System.out.println("ClientHandler created for " + clientSocket.getRemoteSocketAddress());
        try {
            this.out = new PrintWriter(clientSocket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (IOException e) {
            System.err.println("ClientHandler (" + clientSocket.getRemoteSocketAddress() + ") IOException on stream setup: " + e.getMessage());
            // Consider closing socket here if setup fails critically
            // For now, run() will likely fail and trigger cleanup.
        }
    }

    @Override
    public void run() {
        String inputLine;
        try {
            // Ensure 'in' is initialized before use. If 'in' can be null due to constructor issues, handle that.
            if (in == null) {
                System.err.println("ClientHandler for " + clientSocket.getRemoteSocketAddress() + " has no input stream. Closing connection.");
                return; // Exit run method if input stream is not available
            }
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Received from client " + clientSocket.getRemoteSocketAddress() + ": " + inputLine);
                String response = processClientRequest(inputLine);
                // Ensure 'out' is initialized and socket is open before sending response
                if (out != null && !clientSocket.isClosed()) {
                    sendMessage(response);
                } else {
                    System.err.println("Cannot send response to " + clientSocket.getRemoteSocketAddress() + ". Output stream or socket closed.");
                    break; // Exit loop if cannot send response
                }
            }
        } catch (IOException e) {
            // Avoid printing error if the client initiated shutdown or socket is already closed.
            if (!clientSocket.isClosed() && !"Socket closed".equals(e.getMessage()) && !"Connection reset".equals(e.getMessage())) {
                 System.err.println("Client " + clientSocket.getRemoteSocketAddress() + " disconnected due to IOException: " + e.getMessage());
            }
        } finally {
            handleDisconnect();
        }
    }

    private String processClientRequest(String request) {
        if (request == null) {
            return Protocol.ERROR_PREFIX + "Null request received.";
        }
        if (request.startsWith(Protocol.LOAD_MODEL_PREFIX)) {
            String modelId = request.substring(Protocol.LOAD_MODEL_PREFIX.length()).trim();
            if (modelId.isEmpty()) {
                return Protocol.ERROR_PREFIX + "Model ID cannot be empty for LOAD_MODEL.";
            }
            return server.loadModel(modelId);
        } else if (request.startsWith(Protocol.GET_CONTEXT_PREFIX)) {
            String modelId = request.substring(Protocol.GET_CONTEXT_PREFIX.length()).trim();
            if (modelId.isEmpty()) {
                return Protocol.ERROR_PREFIX + "Model ID cannot be empty for GET_CONTEXT.";
            }
            return server.getModelContext(modelId);
        } else if (request.startsWith(Protocol.UPDATE_CONTEXT_PREFIX)) {
            String parts = request.substring(Protocol.UPDATE_CONTEXT_PREFIX.length());
            int separatorIndex = parts.indexOf(':'); // Only look for the first colon
            if (separatorIndex <= 0 || separatorIndex == parts.length() - 1) { // modelId or jsonData is empty
                return Protocol.ERROR_PREFIX + "Invalid format for UPDATE_CONTEXT. Expected: <model_id>:<json_data>";
            }
            String modelId = parts.substring(0, separatorIndex).trim();
            String jsonData = parts.substring(separatorIndex + 1).trim();
            if (modelId.isEmpty()) { // Double check after trim
                return Protocol.ERROR_PREFIX + "Model ID cannot be empty for UPDATE_CONTEXT.";
            }
            if (jsonData.isEmpty()) { // Double check after trim
                return Protocol.ERROR_PREFIX + "JSON data cannot be empty for UPDATE_CONTEXT.";
            }
            return server.updateModelContext(modelId, jsonData);
        } else {
            return Protocol.ERROR_PREFIX + "Unknown command: " + request;
        }
    }

    private void handleDisconnect() {
        System.out.println("Client " + clientSocket.getRemoteSocketAddress() + " connection closing.");
        server.removeClient(this);
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
        } catch (IOException e) {
            System.err.println("Exception while closing resources for client " + clientSocket.getRemoteSocketAddress() + ": " + e.getMessage());
        }
        System.out.println("Client " + clientSocket.getRemoteSocketAddress() + " resources closed.");
    }

    public void sendMessage(String message) {
        if (out != null && !clientSocket.isClosed()) {
            out.println(message);
        } else {
            System.err.println("Could not send message to client " + clientSocket.getRemoteSocketAddress() + " as socket is closed or output stream is null.");
        }
    }

    public String getIdentifier() {
        if (clientSocket != null) {
            return clientSocket.getRemoteSocketAddress().toString();
        }
        return "UNKNOWN_CLIENT";
    }
}
