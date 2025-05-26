package com.example.mcpfs.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.sdk.McpServer;
import io.modelcontextprotocol.sdk.McpSyncServer;
import io.modelcontextprotocol.sdk.server.McpServerFeatures;
import io.modelcontextprotocol.sdk.server.ServerCapabilities;
import io.modelcontextprotocol.sdk.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.sdk.server.McpSyncServerExchange; // For sync handlers
import io.modelcontextprotocol.sdk.common.McpSchema; // For McpSchema.Resource, ReadResourceRequest, ReadResourceResult etc.

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Basic embedded HTTP server (e.g., from Jetty or a simple one for self-hosting the servlet)
// For simplicity in this step, we'll assume a simple way to host the servlet.
// A full implementation would use Jetty, Tomcat, or Spring Boot.
// Let's use Jetty as an example for embedding. Add conceptual Jetty dependencies.
// Conceptual Jetty Dependencies (for a fully runnable example, these would be in pom.xml):
// org.eclipse.jetty:jetty-server
// org.eclipse.jetty:jetty-servlet
// org.eclipse.jetty:jetty-util

// For the purpose of this task, focus on the MCP logic.
// The Jetty server setup can be simplified or made conceptual if direct embedding is too complex for the worker.
// If Jetty is too complex, just prepare the McpSyncServer and its transport,
// and note that it needs to be hosted in a Servlet container.

public class FileSystemServer {

    private final Path rootDirectory;
    private final McpSyncServer mcpServer;
    private final int port;

    // Simple embedded Jetty server for hosting the servlet
    private org.eclipse.jetty.server.Server jettyServer;


    public FileSystemServer(String rootPath, int port) throws IOException {
        this.rootDirectory = Paths.get(rootPath).toAbsolutePath().normalize();
        if (!Files.isDirectory(this.rootDirectory)) {
            throw new IOException("Root path is not a directory or does not exist: " + this.rootDirectory);
        }
        this.port = port;

        ObjectMapper objectMapper = new ObjectMapper();
        HttpServletSseServerTransportProvider transportProvider =
            new HttpServletSseServerTransportProvider(objectMapper, "/mcp/message");

        this.mcpServer = McpServer.sync(transportProvider)
            .serverInfo("filesystem-server", "0.1.0")
            .capabilities(ServerCapabilities.builder().resources(true).build())
            .addResource(createUnifiedFileSystemResource()) // Unified resource
            .build();
        
        // Conceptually, the transportProvider.getServlet() would be registered with an HTTP server.
        // Setup Jetty Server
        this.jettyServer = new org.eclipse.jetty.server.Server(port);
        org.eclipse.jetty.servlet.ServletContextHandler contextHandler = 
            new org.eclipse.jetty.servlet.ServletContextHandler(org.eclipse.jetty.servlet.ServletContextHandler.SESSIONS);
        contextHandler.setContextPath("/");
        // The HttpServletSseServerTransportProvider itself is the servlet
        contextHandler.addServlet(new org.eclipse.jetty.servlet.ServletHolder(transportProvider), "/mcp/message/*"); 
        this.jettyServer.setHandler(contextHandler);

    }

    private McpServerFeatures.SyncResourceSpecification createUnifiedFileSystemResource() {
        McpSchema.Resource resourceDefinition = new McpSchema.Resource(
            "file:///", // Base URI for all filesystem access
            "filesystem-access",
            "Provides access to files and directory listings.",
            null, // Mime type determined by actual content/request type
            null  // No specific schema for the resource definition itself
        );

        return new McpServerFeatures.SyncResourceSpecification(
            resourceDefinition,
            this::handleFileSystemRequest // New unified handler
        );
    }

    private McpSchema.ReadResourceResult handleFileSystemRequest(McpSyncServerExchange exchange, McpSchema.ReadResourceRequest request) {
        String requestedUri = request.getUri();
        Path requestedPath = parsePathFromUri(requestedUri); // parsePathFromUri remains the same

        if (requestedPath == null || !isPathWithinRoot(requestedPath)) { // isPathWithinRoot remains the same
            return new McpSchema.ReadResourceResult(
                McpSchema.ResourceContent.error("Access denied or invalid path.", "text/plain")
            );
        }

        if (Files.isDirectory(requestedPath)) {
            // Delegate to a private method for listing directory (similar to old handleListDirectoryRequest)
            return generateDirectoryListingResponse(requestedUri, requestedPath);
        } else if (Files.isRegularFile(requestedPath)) {
            // Delegate to a private method for reading file (similar to old handleReadFileRequest)
            return generateFileReadResponse(requestedPath);
        } else {
            return new McpSchema.ReadResourceResult(
                McpSchema.ResourceContent.error("Path is not a regular file or directory, or does not exist.", "text/plain")
            );
        }
    }

    private McpSchema.ReadResourceResult generateDirectoryListingResponse(String requestedUri, Path directoryPath) {
        try {
            List<Map<String, Object>> entries = new ArrayList<>();
            Files.list(directoryPath).forEach(p -> {
                Map<String, Object> entry = new HashMap<>();
                entry.put("name", p.getFileName().toString());
                entry.put("type", Files.isDirectory(p) ? "directory" : "file");
                if (Files.isRegularFile(p)) {
                    try {
                        entry.put("size", Files.size(p));
                    } catch (IOException e) { /* ignore */ }
                }
                entries.add(entry);
            });
            ObjectMapper jsonMapper = new ObjectMapper();
            String jsonResponse = jsonMapper.writeValueAsString(Map.of("path", requestedUri, "entries", entries));
            return new McpSchema.ReadResourceResult(McpSchema.ResourceContent.of(jsonResponse, "application/json"));
        } catch (IOException e) {
            return new McpSchema.ReadResourceResult(
                McpSchema.ResourceContent.error("Error listing directory: " + e.getMessage(), "text/plain")
            );
        }
    }

    private McpSchema.ReadResourceResult generateFileReadResponse(Path filePath) {
        try {
            byte[] content = Files.readAllBytes(filePath);
            String mimeType = Files.probeContentType(filePath);
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            return new McpSchema.ReadResourceResult(McpSchema.ResourceContent.of(content, mimeType));
        } catch (IOException e) {
            return new McpSchema.ReadResourceResult(
                McpSchema.ResourceContent.error("Error reading file: " + e.getMessage(), "text/plain")
            );
        }
    }

    private Path parsePathFromUri(String uri) {
        if (uri == null || !uri.startsWith("file:///")) {
            return null;
        }
        try {
            // Assumes URI path is absolute after "file:///"
            String pathStr = uri.substring("file:///".length());
            // Normalize and ensure it's an absolute path interpretation from the URI root.
            // This needs to be combined with the server's rootDirectory.
            // The URI path should be relative to an implicit server root or the server root must be part of the URI.
            // For now, let's assume the client sends paths that are *meant* to be appended to server's root.
            // This is a simplification. A proper MCP server might expect URIs that include its own mount point
            // or fully qualified paths it's authoritative for.
            // Let's refine this: the resource URI "file:///" means the server root.
            // "file:///foo/bar" means rootDirectory.resolve("foo/bar")
            if (pathStr.isEmpty()) return this.rootDirectory;
            return this.rootDirectory.resolve(pathStr).normalize();

        } catch (Exception e) {
            System.err.println("Error parsing URI path: " + uri + " - " + e.getMessage());
            return null;
        }
    }

    private boolean isPathWithinRoot(Path path) {
        return path.toAbsolutePath().startsWith(this.rootDirectory.toAbsolutePath());
    }

    public void start() throws Exception {
        // mcpServer.initialize(); // initialize is for client
        System.out.println("Starting FileSystemServer on port " + port + " serving " + rootDirectory);
        this.jettyServer.start(); 
        // The McpServer itself doesn't have a start() method; its lifecycle is tied to the transport/hosting environment.
        System.out.println("FileSystemServer started. MCP endpoint at /mcp/message");
        // jettyServer.join(); // This would block if called here, usually run in main
    }

    public void stop() throws Exception {
        System.out.println("Stopping FileSystemServer...");
        if (this.jettyServer != null) {
            this.jettyServer.stop();
        }
        // mcpServer.close(); // This would close the MCP application context, including shutting down its executors.
        // Call this when the application is truly shutting down.
        if (this.mcpServer != null) {
             //this.mcpServer.close(); // TODO: Check SDK for proper server shutdown procedure if any
        }
        System.out.println("FileSystemServer stopped.");
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java com.example.mcpfs.server.FileSystemServer <root-directory-path> <port>");
            System.exit(1);
        }
        String rootDir = args[0];
        int portNum = 8080; // Default port
        try {
            portNum = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number: " + args[1] + ". Using default 8080.");
        }

        try {
            FileSystemServer server = new FileSystemServer(rootDir, portNum);
            server.start();
            // Add runtime shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    server.stop();
                } catch (Exception e) {
                    System.err.println("Error stopping server: " + e.getMessage());
                }
            }));
            
            // Keep main thread alive for Jetty server
             server.jettyServer.join();

        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
