package com.example.mcp.server;

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
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Received from client " + clientSocket.getRemoteSocketAddress() + ": " + inputLine);
                server.broadcastMessage(inputLine, this);
            }
        } catch (IOException e) {
            // This exception can occur if the client disconnects abruptly
            System.err.println("Client " + clientSocket.getRemoteSocketAddress() + " disconnected due to IOException in run(): " + e.getMessage());
        } finally {
            handleDisconnect();
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
