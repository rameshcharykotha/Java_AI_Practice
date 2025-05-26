# Java Model Context Protocol (MCP) Application

This project implements a command-line server and client for managing model contexts over a network protocol.
The server can handle multiple clients simultaneously, allowing them to load models, retrieve their contexts (as JSON strings),
and update these contexts.

The available operations are:
*   **Load Model:** Initializes a new model context on the server, identified by a unique model ID.
*   **Get Context:** Retrieves the current context of a specified model as a JSON string.
*   **Update Context:** Modifies the context of a specified model using a provided JSON string.

## Project Structure

```
mcp-project/
├── bin/                  # Output directory for compiled .class files
└── src/
    └── main/
        └── java/
            └── com/
                └── example/
                    └── mcp/
                        ├── client/
                        │   └── Client.java
                        ├── model/
                        │   ├── ModelContext.java
                        │   └── Protocol.java
                        └── server/
                            ├── ClientHandler.java
                            └── Server.java
```

## 1. Compilation

To compile the Java source files, navigate to the root directory of the project (`mcp-project`)
and run the following command. This command assumes you have a Java Development Kit (JDK) installed
and the `javac` compiler is in your system's PATH.

The compiled `.class` files will be placed in the `mcp-project/bin` directory.

```bash
# Ensure you are in the mcp-project directory
# Create the output directory if it doesn't exist:
mkdir -p bin

# Compile all .java files:
javac -d bin src/main/java/com/example/mcp/server/*.java src/main/java/com/example/mcp/client/*.java src/main/java/com/example/mcp/model/*.java
```

## 2. Running the Server

To run the server, use the following command from the `mcp-project` root directory.
The server will start and listen on port `12345` by default.

```bash
# Ensure you are in the mcp-project directory
java -cp bin com.example.mcp.server.Server
```

You should see output similar to:
```
Server started on port 12345
```

## 3. Running the Client

To run a client, use the following command from the `mcp-project` root directory.
You will need to provide the server's IP address and port number as command-line arguments.
If the server is running on the same machine, you can use `127.0.0.1` or `localhost` as the IP address.

```bash
# Ensure you are in the mcp-project directory
java -cp bin com.example.mcp.client.Client <server_ip> <server_port>
```

**Example (connecting to a server on the same machine):**
```bash
java -cp bin com.example.mcp.client.Client 127.0.0.1 12345
```

After connecting, you should see:
```
Connected to server: 127.0.0.1:12345
Enter commands (e.g., 'load <modelId>', 'get <modelId>', 'update <modelId> <jsonData>', 'exit'):
```

## 4. Client Commands

The client accepts the following commands:

*   `load <modelId>`
    *   Sends a request to the server to load a model with the given `modelId`.
    *   Example: `load modelAlpha`

*   `get <modelId>`
    *   Sends a request to the server to retrieve the context for the given `modelId`.
    *   Example: `get modelAlpha`

*   `update <modelId> <jsonData>`
    *   Sends a request to the server to update the context for the given `modelId` with the provided `jsonData`.
    *   The `jsonData` should be a valid JSON object string.
    *   Example: `update modelAlpha {"key1":"value1","description":"This is a test model"}`

*   `exit` or `quit`
    *   Disconnects the client from the server and terminates the client application.

## 5. Manual Test Plan

This plan requires at least two terminal windows: one for the server and one for the client. You can open more client terminals to test multi-client behavior.

**Setup:**
*   Open a terminal window (Terminal S) for the Server.
*   Open another terminal window (Terminal C1) for Client 1.
*   (Optional) Open a third terminal window (Terminal C2) for Client 2.
*   Ensure all Java code is compiled as per the compilation instructions.

**Steps:**

1.  **Start the Server:**
    *   In Terminal S, navigate to the `mcp-project` directory.
    *   Run the server: `java -cp bin com.example.mcp.server.Server`
    *   **Expected Server Output:** `Server started on port 12345`

2.  **Start Client 1:**
    *   In Terminal C1, navigate to the `mcp-project` directory.
    *   Run Client 1: `java -cp bin com.example.mcp.client.Client 127.0.0.1 12345`
    *   **Expected Client C1 Output:**
        ```
        Connected to server: 127.0.0.1:12345
        Enter commands (e.g., 'load <modelId>', 'get <modelId>', 'update <modelId> <jsonData>', 'exit'):
        ```
    *   **Expected Server Output (Terminal S):** A message indicating a new client connected, e.g.,
        ```
        New client connected: /127.0.0.1:xxxxx (some client port)
        ```

3.  **Test `load` command (Client C1):**
    *   In Terminal C1, type: `load modelA`
    *   **Expected Client C1 Output (after pressing Enter):**
        ```
        Sent to Server: LOAD_MODEL:modelA
        Received from Server: SUCCESS:Model modelA loaded.
        ```
    *   **Expected Server Output (Terminal S):**
        ```
        Received from client /127.0.0.1:xxxxx: LOAD_MODEL:modelA
        ```
        (Server logs the processing and successful loading)

4.  **Test `get` command for an initially empty model (Client C1):**
    *   In Terminal C1, type: `get modelA`
    *   **Expected Client C1 Output:**
        ```
        Sent to Server: GET_CONTEXT:modelA
        Received from Server: CONTEXT_DATA:{}
        ```
    *   **Expected Server Output (Terminal S):**
        ```
        Received from client /127.0.0.1:xxxxx: GET_CONTEXT:modelA
        ```

5.  **Test `update` command (Client C1):**
    *   In Terminal C1, type: `update modelA {"name":"test","version":"1.0"}`
    *   **Expected Client C1 Output:**
        ```
        Sent to Server: UPDATE_CONTEXT:modelA:{"name":"test","version":"1.0"}
        Received from Server: SUCCESS:Model modelA updated.
        ```
    *   **Expected Server Output (Terminal S):**
        ```
        Received from client /127.0.0.1:xxxxx: UPDATE_CONTEXT:modelA:{"name":"test","version":"1.0"}
        ```

6.  **Test `get` command again to see updated context (Client C1):**
    *   In Terminal C1, type: `get modelA`
    *   **Expected Client C1 Output:**
        ```
        Sent to Server: GET_CONTEXT:modelA
        Received from Server: CONTEXT_DATA:{"name":"test","version":"1.0"}
        ```

7.  **Test `get` for non-existent model (Client C1):**
    *   In Terminal C1, type: `get modelB`
    *   **Expected Client C1 Output:**
        ```
        Sent to Server: GET_CONTEXT:modelB
        Received from Server: ERROR:Model not found.
        ```
    *   **Expected Server Output (Terminal S):**
        ```
        Received from client /127.0.0.1:xxxxx: GET_CONTEXT:modelB
        ```

8.  **Test `update` for non-existent model (Client C1):**
    *   In Terminal C1, type: `update modelB {"status":"active"}`
    *   **Expected Client C1 Output:**
        ```
        Sent to Server: UPDATE_CONTEXT:modelB:{"status":"active"}
        Received from Server: ERROR:Model not found. Load model first.
        ```
    *   **Expected Server Output (Terminal S):**
        ```
        Received from client /127.0.0.1:xxxxx: UPDATE_CONTEXT:modelB:{"status":"active"}
        ```

9.  **Test invalid JSON for `update` (Client C1):**
    *   In Terminal C1, type: `update modelA {invalid_json`
    *   **Expected Client C1 Output (the exact error message may vary slightly based on parser details):**
        ```
        Sent to Server: UPDATE_CONTEXT:modelA:{invalid_json
        Received from Server: ERROR:Invalid JSON data: JSON string must start with '{' and end with '}'.
        ```
        (Or another specific error message like "Unexpected characters...", "Expected ',' separator...")
    *   **Expected Server Output (Terminal S):**
        ```
        Received from client /127.0.0.1:xxxxx: UPDATE_CONTEXT:modelA:{invalid_json
        Error parsing JSON data for model modelA: JSON string must start with '{' and end with '}'.
        ```

10. **Test Client Disconnection (Client C1):**
    *   In Terminal C1, type: `exit`
    *   **Expected Client C1 Output:**
        ```
        Client shutting down...
        Client shutdown complete.
        ```
        (The client application will terminate)
    *   **Expected Server Output (Terminal S):** Messages indicating client disconnection, e.g.,
        ```
        Client /127.0.0.1:xxxxx disconnected due to IOException: Socket closed (or similar)
        Client /127.0.0.1:xxxxx connection closing.
        Client /127.0.0.1:xxxxx resources closed.
        Client disconnected: /127.0.0.1:xxxxx
        ```

**(Optional) Multi-Client Interaction:**
*   Start Client 2 (Terminal C2) and connect to the server.
*   In Client C2, try to `get modelA`. It should retrieve the context updated by Client C1.
*   In Client C1, load a new model `modelC`.
*   In Client C2, try to `get modelC`.
*   Both clients can independently manage different models or interact with shared models (though "sharing" here just means both can access/update any loaded model context).

This completes the manual test. The server can be stopped by pressing `Ctrl+C` in Terminal S.
```
