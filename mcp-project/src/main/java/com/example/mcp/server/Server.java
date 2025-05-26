package com.example.mcp.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import com.example.mcp.model.ModelContext;
import com.example.mcp.model.Protocol;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int PORT = 12345;
    // Using a thread pool for managing client threads for better resource management
    private static final ExecutorService clientExecutorService = Executors.newCachedThreadPool();
    private List<ClientHandler> clientHandlers = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, ModelContext> modelContexts = new ConcurrentHashMap<>();

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);
            while (!serverSocket.isClosed()) { // Loop until server socket is closed
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New client connected: " + clientSocket.getRemoteSocketAddress());
                    ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                    clientHandlers.add(clientHandler); // Ensure this line is present
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

    public synchronized String loadModel(String modelId) {
        if (modelContexts.containsKey(modelId)) {
            return Protocol.ERROR_PREFIX + "Model already loaded.";
        }
        modelContexts.put(modelId, new ModelContext());
        return Protocol.SUCCESS_PREFIX + "Model " + modelId + " loaded.";
    }

    public synchronized String getModelContext(String modelId) {
        if (!modelContexts.containsKey(modelId)) {
            return Protocol.ERROR_PREFIX + "Model not found.";
        }
        ModelContext modelContext = modelContexts.get(modelId);
        return Protocol.CONTEXT_DATA_PREFIX + modelContext.toJsonString();
    }

    public synchronized String updateModelContext(String modelId, String jsonData) {
        if (!modelContexts.containsKey(modelId)) {
            return Protocol.ERROR_PREFIX + "Model not found. Load model first.";
        }
        try {
            ModelContext newContextData = ModelContext.fromJsonString(jsonData);
            // Assuming fromJsonString might return an empty context or one with no data if JSON is technically valid but empty (e.g. "{}")
            // Depending on ModelContext.fromJsonString implementation, a null check or a check on its internal state might be needed.
            // For now, if fromJsonString itself doesn't throw an exception for malformed JSON, we proceed.
            // A more robust check could be if newContextData.getData().isEmpty() and jsonData is not "{}",
            // but that depends on specific requirements for "invalid" JSON.
            // The prompt implies ModelContext.fromJsonString handles parsing and might return null or throw error for bad format.
            // Let's assume fromJsonString returns a valid ModelContext object or throws an exception.
            // If it returns an empty ModelContext for invalid JSON, that's handled by ModelContext itself.
            
            // The prompt suggests: ModelContext newContextData = ModelContext.fromJsonString(jsonData); modelContexts.get(modelId).setData(newContextData.getData());
            // And then: "If parsing jsonData fails (e.g., ModelContext.fromJsonString throws an exception or returns null/empty), return Protocol.ERROR_PREFIX + "Invalid JSON data."."
            // And also: "Let's assume replacing the object is cleaner: ModelContext newContextData = ModelContext.fromJsonString(jsonData); if (newContextData == null /* or indicates error */) { return Protocol.ERROR_PREFIX + "Invalid JSON data."; } modelContexts.put(modelId, newContextData);"
            // We will go with replacing the object.
            // We also need to ensure that ModelContext.fromJsonString can signal invalid JSON.
            // The current ModelContext.fromJsonString returns an empty context for invalid JSON. This might not be what we want.
            // For now, let's stick to the prompt's direct advice on replacing.
            // If fromJsonString returns an empty context for "invalid" json, this logic would accept it.
            // This should be refined if "invalid JSON" needs stricter handling than what fromJsonString currently provides.
            
            // ModelContext.fromJsonString will now throw IllegalArgumentException for invalid JSON.
            ModelContext parsedContext = ModelContext.fromJsonString(jsonData);
            modelContexts.put(modelId, parsedContext); // Replace the old context
            return Protocol.SUCCESS_PREFIX + "Model " + modelId + " updated.";
        } catch (IllegalArgumentException e) {
            // Catch specific parsing errors from ModelContext.fromJsonString
            System.err.println("Error parsing JSON data for model " + modelId + ": " + e.getMessage());
            return Protocol.ERROR_PREFIX + "Invalid JSON data: " + e.getMessage();
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
