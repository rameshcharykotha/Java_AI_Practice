# Simple Java Multi-Client Chat (MCP) Application

This project implements a basic command-line multi-client chat application in Java.
It consists of a server (`Server.java`), a client handler (`ClientHandler.java`) on the server side,
and a client application (`Client.java`).

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
javac -d bin src/main/java/com/example/mcp/server/*.java src/main/java/com/example/mcp/client/*.java
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
Enter messages to send to the server (type 'exit' or 'quit' to disconnect):
```

## 4. Manual Test Plan

This plan requires at least two terminal windows or command prompts: one for the server and one for each client.

**Setup:**
*   Open a terminal window (Terminal S) for the Server.
*   Open another terminal window (Terminal A) for Client A.
*   Open a third terminal window (Terminal B) for Client B.
*   Ensure all Java code is compiled as per the compilation instructions.

**Steps:**

1.  **Start the Server:**
    *   In Terminal S (Server), navigate to the `mcp-project` directory.
    *   Run the server: `java -cp bin com.example.mcp.server.Server`
    *   **Expected Server Output:** `Server started on port 12345`

2.  **Start Client A:**
    *   In Terminal A (Client A), navigate to the `mcp-project` directory.
    *   Run Client A: `java -cp bin com.example.mcp.client.Client 127.0.0.1 12345`
    *   **Expected Client A Output:**
        ```
        Connected to server: 127.0.0.1:12345
        Enter messages to send to the server (type 'exit' or 'quit' to disconnect):
        ```
    *   **Expected Server Output (Terminal S):** A message indicating a new client connected, e.g.,
        ```
        ClientHandler created for /127.0.0.1:xxxxx (some client port)
        New client connected: /127.0.0.1:xxxxx (some client port)
        ```

3.  **Start Client B:**
    *   In Terminal B (Client B), navigate to the `mcp-project` directory.
    *   Run Client B: `java -cp bin com.example.mcp.client.Client 127.0.0.1 12345`
    *   **Expected Client B Output:**
        ```
        Connected to server: 127.0.0.1:12345
        Enter messages to send to the server (type 'exit' or 'quit' to disconnect):
        ```
    *   **Expected Server Output (Terminal S):** Another message indicating a new client connected, e.g.,
        ```
        ClientHandler created for /127.0.0.1:yyyyy (some other client port)
        New client connected: /127.0.0.1:yyyyy (some other client port)
        ```

4.  **Client A Sends a Message:**
    *   In Terminal A (Client A), type `Hello from Client A!` and press Enter.
    *   **Expected Client A Output:** `Sent to Server: Hello from Client A!`
    *   **Expected Server Output (Terminal S):** `Received from client /127.0.0.1:xxxxx: Hello from Client A!`
    *   **Expected Client B Output (Terminal B):** `Received from Server: Hello from Client A!`

5.  **Client B Sends a Message:**
    *   In Terminal B (Client B), type `Hi Client A, this is B.` and press Enter.
    *   **Expected Client B Output:** `Sent to Server: Hi Client A, this is B.`
    *   **Expected Server Output (Terminal S):** `Received from client /127.0.0.1:yyyyy: Hi Client A, this is B.`
    *   **Expected Client A Output (Terminal A):** `Received from Server: Hi Client A, this is B.`

6.  **Client A Disconnects:**
    *   In Terminal A (Client A), type `exit` and press Enter.
    *   **Expected Client A Output:**
        ```
        Client shutting down...
        Client shutdown complete.
        ```
        (The client application will terminate)
    *   **Expected Server Output (Terminal S):** A message indicating client disconnection, e.g.,
        ```
        Client /127.0.0.1:xxxxx disconnected due to IOException in run(): Connection reset by peer (or similar)
        Client /127.0.0.1:xxxxx connection closing.
        Client /127.0.0.1:xxxxx resources closed.
        Client disconnected: /127.0.0.1:xxxxx
        ```
        (The exact IOException message might vary based on OS and timing).

7.  **Client B Sends Another Message:**
    *   In Terminal B (Client B), type `Client A, are you still there?` and press Enter.
    *   **Expected Client B Output:** `Sent to Server: Client A, are you still there?`
    *   **Expected Server Output (Terminal S):** `Received from client /127.0.0.1:yyyyy: Client A, are you still there?`
    *   **Expected Client A Output (Terminal A):** No output, as Client A is disconnected.

8.  **Client B Disconnects:**
    *   In Terminal B (Client B), type `quit` and press Enter.
    *   **Expected Client B Output:**
        ```
        Client shutting down...
        Client shutdown complete.
        ```
        (The client application will terminate)
    *   **Expected Server Output (Terminal S):** Similar disconnection messages for Client B.

This completes the manual test. The server can be stopped by pressing `Ctrl+C` in Terminal S.
```
