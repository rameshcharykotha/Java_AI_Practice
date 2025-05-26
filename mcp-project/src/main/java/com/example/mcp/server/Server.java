package com.example.mcp.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int PORT = 12345;
    // Using a thread pool for managing client threads for better resource management
    private static final ExecutorService clientExecutorService = Executors.newCachedThreadPool();
    private List<ClientHandler> clientHandlers = Collections.synchronizedList(new ArrayList<>());

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);
            while (!serverSocket.isClosed()) { // Loop until server socket is closed
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New client connected: " + clientSocket.getRemoteSocketAddress());
                    ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                    clientHandlers.add(clientHandler);
                    clientExecutorService.submit(clientHandler); // Use executor service to manage thread
                } catch (IOException e) {
                    // Log error accepting client connection but continue running the server
                    // unless serverSocket itself is the issue (which would be caught by outer loop condition)
                    System.err.println("Error accepting client connection: " + e.getMessage());
                    // If serverSocket is closed, the loop condition will handle termination.
                    if (serverSocket.isClosed()) {
                         System.out.println("Server socket closed, shutting down."); // Informational
                         break; // Exit loop if server socket is closed
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Server IOException on startup: " + e.getMessage());
        } finally {
            // Ensure the executor service is shut down when the server stops
            shutdownExecutorService();
        }
    }

    public void broadcastMessage(String message, ClientHandler sender) {
        // Iterate over a copy of the list to avoid ConcurrentModificationException
        // if a client disconnects (and is removed) during iteration.
        List<ClientHandler> handlersSnapshot = new ArrayList<>(clientHandlers);
        for (ClientHandler clientHandler : handlersSnapshot) {
            if (clientHandler != sender) {
                clientHandler.sendMessage(message);
            }
        }
    }

    public void removeClient(ClientHandler clientHandler) {
        clientHandlers.remove(clientHandler);
        System.out.println("Client disconnected: " + clientHandler.getIdentifier());
    }

    private void shutdownExecutorService() {
        System.out.println("Shutting down client executor service...");
        clientExecutorService.shutdown(); // Disable new tasks from being submitted
        // Optionally, wait for existing tasks to terminate
        // try {
        //     if (!clientExecutorService.awaitTermination(60, TimeUnit.SECONDS)) {
        //         clientExecutorService.shutdownNow(); // Cancel currently executing tasks
        //         if (!clientExecutorService.awaitTermination(60, TimeUnit.SECONDS))
        //             System.err.println("Executor service did not terminate");
        //     }
        // } catch (InterruptedException ie) {
        //     clientExecutorService.shutdownNow();
        //     Thread.currentThread().interrupt();
        // }
        System.out.println("Client executor service shut down.");
    }


    public static void main(String[] args) {
        Server server = new Server();
        server.startServer();
    }
}
