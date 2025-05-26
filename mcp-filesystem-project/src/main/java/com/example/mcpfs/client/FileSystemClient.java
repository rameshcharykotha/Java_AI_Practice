package com.example.mcpfs.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.sdk.McpClient;
import io.modelcontextprotocol.sdk.McpSyncClient;
import io.modelcontextprotocol.sdk.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.sdk.common.McpSchema; // For ReadResourceRequest, ReadResourceResult etc.

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;
import java.util.Scanner;
import java.util.List;

public class FileSystemClient {

    private final McpSyncClient mcpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Path currentPath = Paths.get("/"); // Represents the current relative path on the server

    public FileSystemClient(String serverBaseUrl) {
        // Ensure serverBaseUrl ends with a slash if it's just the base
        String correctedServerBaseUrl = serverBaseUrl.endsWith("/") ? serverBaseUrl : serverBaseUrl + "/";
        String mcpEndpoint = correctedServerBaseUrl + "mcp/message"; // Assuming server endpoint is /mcp/message

        HttpClientSseClientTransport transport = new HttpClientSseClientTransport(URI.create(mcpEndpoint));
        
        this.mcpClient = McpClient.sync(transport)
            .requestTimeout(Duration.ofSeconds(10))
            // No specific client capabilities needed for basic resource reading
            .build();
    }

    public void connect() {
        System.out.println("Connecting to server...");
        this.mcpClient.initialize(); // Initialize connection and protocol handshake
        System.out.println("Connected. Server info: " + this.mcpClient.getServerInfo());
        System.out.println("Server capabilities: " + this.mcpClient.getServerCapabilities());
    }

    public void disconnect() {
        System.out.println("Disconnecting...");
        this.mcpClient.closeGracefully();
        System.out.println("Disconnected.");
    }

    public void runCommandLoop() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("File System Client. Type 'help' for commands.");

        while (true) {
            System.out.print(currentPath.toString().replace("\\", "/") + "> ");
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split("\s+", 2);
            String command = parts[0].toLowerCase();
            String argument = parts.length > 1 ? parts[1] : "";

            try {
                switch (command) {
                    case "ls":
                        handleLs(argument);
                        break;
                    case "cd":
                        handleCd(argument);
                        break;
                    case "cat":
                        handleCat(argument);
                        break;
                    case "pwd":
                        System.out.println(currentPath.toString().replace("\\", "/"));
                        break;
                    case "help":
                        printHelp();
                        break;
                    case "exit":
                    case "quit":
                        return;
                    default:
                        System.out.println("Unknown command: " + command);
                        printHelp();
                }
            } catch (Exception e) {
                System.err.println("Error processing command: " + e.getMessage());
                // e.printStackTrace(); // for debugging
            }
        }
    }

    private void handleLs(String pathArg) throws Exception {
        Path targetPath = resolvePath(pathArg);
        // Ensure the path for ls ends with a slash for the server's URI construction for directories
        String serverPath = targetPath.toString().replace("\\", "/");
        if (!serverPath.endsWith("/")) {
            serverPath += "/";
        }
        
        String resourceUri = "file://" + (serverPath.startsWith("/") ? serverPath.substring(1) : serverPath);
        if (serverPath.equals("/")) { // Root directory
             resourceUri = "file:///";
        }


        McpSchema.ReadResourceRequest request = new McpSchema.ReadResourceRequest(resourceUri);
        McpSchema.ReadResourceResult result = mcpClient.readResource(request);

        if (result.getContent().getError() != null) {
            System.err.println("Server error: " + result.getContent().getError().getMessage());
        } else {
            String mimeType = result.getContent().getMimeType();
            if ("application/json".equals(mimeType)) {
                String jsonContent = new String(result.getContent().getRaw(), StandardCharsets.UTF_8);
                // Assuming JSON structure: {"path": "uri", "entries": [{"name": "...", "type": "...", "size": ...}]}
                Map<String, Object> responseMap = objectMapper.readValue(jsonContent, new TypeReference<Map<String, Object>>() {});
                List<Map<String, Object>> entries = (List<Map<String, Object>>) responseMap.get("entries");
                if (entries != null) {
                    for (Map<String, Object> entry : entries) {
                        String type = (String) entry.get("type");
                        String name = (String) entry.get("name");
                        if ("directory".equals(type)) {
                            System.out.println(name + "/");
                        } else {
                            System.out.println(name + "	(" + entry.getOrDefault("size", 0) + " bytes)");
                        }
                    }
                } else {
                     System.out.println("No entries found or unexpected JSON structure.");
                }
            } else {
                System.err.println("Unexpected content type for ls: " + mimeType);
            }
        }
    }

    private void handleCd(String pathArg) {
        if (pathArg.isEmpty()) {
            currentPath = Paths.get("/"); // cd to root
             System.out.println("Current path: " + currentPath.toString().replace("\\", "/"));
            return;
        }
        Path newPath = resolvePath(pathArg); // Use resolvePath for consistency
        currentPath = newPath; 
        System.out.println("Current path: " + currentPath.toString().replace("\\", "/"));
    }

    private void handleCat(String pathArg) throws Exception {
        if (pathArg.isEmpty()) {
            System.err.println("cat: missing file operand");
            return;
        }
        Path targetPath = resolvePath(pathArg);
         String serverPath = targetPath.toString().replace("\\", "/");
         if (serverPath.endsWith("/")) {
              System.err.println("cat: cannot cat a directory: " + pathArg);
              return;
         }

        String resourceUri = "file://" + (serverPath.startsWith("/") ? serverPath.substring(1) : serverPath);
        if (serverPath.equals("/")) { // Technically, can't cat root, but to be safe with URI
             System.err.println("cat: cannot cat root directory");
             return;
        }

        McpSchema.ReadResourceRequest request = new McpSchema.ReadResourceRequest(resourceUri);
        McpSchema.ReadResourceResult result = mcpClient.readResource(request);

        if (result.getContent().getError() != null) {
            System.err.println("Server error: " + result.getContent().getError().getMessage());
        } else {
            // For simplicity, print text files. Binary files might print garbage.
            // A real client might try to detect binary types and offer to save.
            String content = new String(result.getContent().getRaw(), StandardCharsets.UTF_8);
            System.out.println(content);
        }
    }
    
    private Path resolvePath(String argumentPath) {
        Path p = Paths.get(argumentPath);
        if (p.isAbsolute() || argumentPath.startsWith("/") || argumentPath.startsWith("\\") ) {
            // If user provides an absolute path (e.g. /foo or C:\foo on windows, though client is path-style agnostic)
            // it's interpreted as relative to the server's conceptual root.
            // Paths.get("/").resolve(...) ensures it's treated as from the root of the conceptual file system.
            // Example: if argumentPath is "/user/docs", p.getRoot() might be "/" or null depending on OS and input.
            // We want to ensure it becomes "/user/docs" in a normalized way from root.
             String pathWithoutRoot = argumentPath;
             if (p.getRoot() != null) { // Handles C:\ -> \ on windows, or / -> / on unix
                 pathWithoutRoot = p.subpath(0, p.getNameCount()).toString();
             }
             // Ensure it starts with a slash if it was an absolute path, then normalize
             if (!pathWithoutRoot.startsWith(File.separator) && (argumentPath.startsWith("/") || argumentPath.startsWith("\\"))){
                 pathWithoutRoot = File.separator + pathWithoutRoot;
             } else if (!pathWithoutRoot.startsWith("/") && !pathWithoutRoot.startsWith("\\") && p.isAbsolute()){
                 //This case is tricky, e.g. "C:foo" on Windows. For simplicity, assume slash-prefixed for server context.
                 //The server expects paths like /foo/bar, not C:/foo/bar in the URI.
                 //This client assumes a Unix-like path structure for server interaction.
             }

            return Paths.get("/").resolve(pathWithoutRoot).normalize();
        }
        // Relative path
        return currentPath.resolve(argumentPath).normalize();
    }


    private void printHelp() {
        System.out.println("Available commands:");
        System.out.println("  ls [path]        - List directory contents. If path is omitted, lists current directory.");
        System.out.println("  cd <path>        - Change current directory. '..' for parent, '/' for root.");
        System.out.println("  cat <path>       - Show file content.");
        System.out.println("  pwd              - Print working directory (client-side path).");
        System.out.println("  help             - Show this help message.");
        System.out.println("  exit / quit      - Exit the client.");
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java com.example.mcpfs.client.FileSystemClient <server-base-url>");
            System.err.println("Example: java com.example.mcpfs.client.FileSystemClient http://localhost:8080");
            System.exit(1);
        }

        FileSystemClient client = new FileSystemClient(args[0]);
        try {
            client.connect();
            client.runCommandLoop();
        } catch (Exception e) {
            System.err.println("Client error: " + e.getMessage());
            // e.printStackTrace(); // for debugging
        } finally {
            client.disconnect();
        }
    }
}
