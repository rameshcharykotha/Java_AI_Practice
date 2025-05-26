# MCP Filesystem Server and Client

## Overview

This project implements a Model Context Protocol (MCP) server that exposes a local filesystem, and an MCP client to browse it.
It uses the official [MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk).

The server allows clients to:
- List directory contents.
- Read file contents.

The client provides a simple command-line interface to interact with the server.

## Prerequisites

- Java 17 or higher
- Apache Maven (for building)

## Dependencies

Key dependencies include:
- `io.modelcontextprotocol.sdk:mcp` (Core MCP Java SDK)
- `com.fasterxml.jackson.core:jackson-databind` (for JSON processing)
- Embedded Jetty for the server (e.g., `org.eclipse.jetty:jetty-server`, `org.eclipse.jetty:jetty-servlet`)

(A full list would be in `pom.xml` - See conceptual `pom.xml` below)

## Project Structure (Conceptual)

```
mcp-filesystem-project/
├── pom.xml
└── src/
    └── main/
        └── java/
            └── com/
                └── example/
                    └── mcpfs/
                        ├── client/
                        │   └── FileSystemClient.java
                        └── server/
                            └── FileSystemServer.java
```

## Building

1.  Navigate to the project root directory (`mcp-filesystem-project`).
2.  Compile the project using Maven:
    ```bash
    mvn clean package
    ```
    This will generate a JAR file in the `target/` directory (e.g., `mcpfs-project-1.0-SNAPSHOT.jar`).

## Running the Server

The `FileSystemServer` serves files from a specified root directory on a given port.

**Command:**
```bash
java -cp target/your-project-jar-name.jar com.example.mcpfs.server.FileSystemServer <path-to-serve> <port>
```

**Example:**
To serve the directory `/srv/myfiles` on port `8080`:
```bash
java -cp target/mcpfs-project-1.0-SNAPSHOT.jar com.example.mcpfs.server.FileSystemServer /srv/myfiles 8080
```
The server will print a message indicating it has started and the MCP endpoint (e.g., `/mcp/message`).

## Running the Client

The `FileSystemClient` connects to the server and provides a command-line interface.

**Command:**
```bash
java -cp target/your-project-jar-name.jar com.example.mcpfs.client.FileSystemClient <server-base-url>
```

**Example:**
If the server is running on `http://localhost:8080`:
```bash
java -cp target/mcpfs-project-1.0-SNAPSHOT.jar com.example.mcpfs.client.FileSystemClient http://localhost:8080
```

## Client Commands

Once the client is connected, you can use the following commands:

-   `ls [path]`        - List directory contents. If `path` is omitted, lists current directory.
-   `cd <path>`        - Change current directory on the server. `path` can be relative or absolute (from server's root).
-   `cat <path>`       - Show file content.
-   `pwd`              - Print the current working directory (client's perspective of path on the server).
-   `help`             - Show this help message.
-   `exit` / `quit`    - Exit the client.

Paths are relative to the server's root directory unless they start with `/`.

### Example Client Session

```
Connecting to server...
Connected. Server info: ServerInfo[name=filesystem-server, version=0.1.0, protocolVersion=...]
Server capabilities: ServerCapabilities[resources=true, tools=false, prompts=false, logging=Optional.empty, completions=Optional.empty]
File System Client. Type 'help' for commands.
/> ls
mydir/
myfile.txt    (1024 bytes)
/> cd mydir
Current path: /mydir
/mydir> ls
anotherfile.txt    (50 bytes)
/mydir> cat anotherfile.txt
Content of another file.
/mydir> cd ..
Current path: /
/> exit
Disconnecting...
Disconnected.
```

## Conceptual `pom.xml` Snippet

For reference, the Maven dependencies would look something like this:

```xml
<project ...>
    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <mcp.sdk.version>0.10.0</mcp.sdk.version> <!-- Check for latest MCP SDK version -->
        <jackson.version>2.15.3</jackson.version>
        <jetty.version>11.0.15</jetty.version> <!-- Check for a recent Jetty 11 version -->
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>io.modelcontextprotocol.sdk</groupId>
                <artifactId>mcp-bom</artifactId>
                <version>${mcp.sdk.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <!-- MCP Core SDK -->
        <dependency>
            <groupId>io.modelcontextprotocol.sdk</groupId>
            <artifactId>mcp</artifactId>
        </dependency>

        <!-- Jackson for JSON processing -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>${jackson.version}</version>
        </dependency>

        <!-- Jetty for embedded server -->
        <dependency>
            <groupId>org.eclipse.jetty</groupId>
            <artifactId>jetty-server</artifactId>
            <version>${jetty.version}</version>
        </dependency>
        <dependency>
            <groupId>org.eclipse.jetty</groupId>
            <artifactId>jetty-servlet</artifactId>
            <version>${jetty.version}</version>
        </dependency>
         <dependency>
            <groupId>org.eclipse.jetty</groupId>
            <artifactId>jetty-util</artifactId> <!-- May not be explicitly needed if transitive -->
            <version>${jetty.version}</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.2.4</version> <!-- Or a newer version -->
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                        <configuration>
                            <transformers>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                    <manifestEntries>
                                        <Main-Class-Client>com.example.mcpfs.client.FileSystemClient</Main-Class-Client>
                                        <Main-Class-Server>com.example.mcpfs.server.FileSystemServer</Main-Class-Server>
                                    </manifestEntries>
                                </transformer>
                            </transformers>
                            <finalName>mcpfs-project-uber</finalName> <!-- Example uber JAR name -->
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```
Note: The `maven-shade-plugin` part is to create an executable uber JAR, which is convenient but optional. If not used, the `java -cp` command would need to include all dependencies on the classpath. The `Main-Class-Client` and `Main-Class-Server` in `<manifestEntries>` are just illustrative for setting main classes if one were to make separate runnable JARs or use a launcher script. For `java -cp target/your-project-jar-name.jar com.example.mcpfs.server.FileSystemServer`, the JAR just needs to be on the classpath.
