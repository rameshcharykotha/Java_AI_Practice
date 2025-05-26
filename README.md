# Java AI Chat

A simple Java-based chat application using Spring Boot and Spring AI with an OpenAI model.

## Features

*   Exposes a REST API endpoint (`/ai`) for chat interaction.
*   Uses Spring AI to connect to an OpenAI language model.
*   Simple to run and interact with.

## Prerequisites

*   **Java Development Kit (JDK) 17 or later:** Required to compile and run the application.
*   **Apache Maven:** Used for project build and dependency management.
*   **OpenAI API Key:** Necessary for the application to communicate with the OpenAI API. You need to have an active OpenAI account and API key.

## Getting Started

Follow these steps to get the application up and running on your local machine.

1.  **Clone the repository:**
    ```bash
    git clone <repository_url> # Replace <repository_url> with the actual URL of this repository
    cd <repository_directory_name> # Replace <repository_directory_name> with the cloned directory name
    ```

2.  **Configure your OpenAI API Key:**
    Open the `src/main/resources/application.properties` file.
    Add the following line, replacing `YOUR_OPENAI_API_KEY` with your actual key:
    ```properties
    spring.ai.openai.api-key=YOUR_OPENAI_API_KEY
    ```

3.  **Build the project:**
    Use Maven to build the project and download dependencies:
    ```bash
    ./mvnw clean install
    ```
    (On Windows, use `mvnw.cmd clean install`)

4.  **Run the application:**
    You can run the application using the Spring Boot Maven plugin:
    ```bash
    ./mvnw spring-boot:run
    ```
    (On Windows, use `mvnw.cmd spring-boot:run`)

    Alternatively, you can run the packaged JAR file from the `target` directory (after building):
    ```bash
    java -jar target/chat-0.0.1-SNAPSHOT.jar
    ```
    The application will start on the default port (usually 8080).

## Usage

Once the application is running, you can interact with the chat API by sending GET requests to the `/ai` endpoint with your message as the `userInput` query parameter.

**Example using cURL:**

```bash
curl -X GET "http://localhost:8080/ai?userInput=Hello, AI!"
```

**Example using a web browser:**

Open your web browser and navigate to:
`http://localhost:8080/ai?userInput=What%20is%20Spring%20AI?`

Replace `Hello, AI!` or `What is Spring AI?` with your desired message. The API will return the AI's response as plain text.

## Running Tests

The project includes unit tests to ensure the application context loads correctly and basic functionality is operational.

To run the tests, use the following Maven command:

```bash
./mvnw test
```
(On Windows, use `mvnw.cmd test`)

Maven will execute the tests and generate a report in the `target/surefire-reports` directory.

## Built With

*   [Spring Boot](https://spring.io/projects/spring-boot) - Framework for building Java applications.
*   [Spring AI](https://spring.io/projects/spring-ai) - Framework for AI engineering, used here for OpenAI integration.
*   [OpenAI API](https://openai.com/api/) - Language model API.
*   [Apache Maven](https://maven.apache.org/) - Dependency management and build tool.
*   Java 17
