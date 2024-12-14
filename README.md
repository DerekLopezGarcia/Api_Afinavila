# Ktor API Project

This project is an API built using the Ktor framework. It was generated using the Ktor Project Generator and includes several key features and dependencies to facilitate development.

## Features

- **Koin**: Provides dependency injection.
- **Content Negotiation**: Automatic content conversion according to Content-Type and Accept headers.
- **Routing**: Structured routing DSL.
- **GSON**: JSON serialization using GSON library.
- **Logback**: Logging configuration.
- **Ktor YAML Config**: Configuration using YAML files.

## Project Structure

The project follows a typical Ktor structure:

- `build.gradle.kts`: Gradle build script.
- `src/main/kotlin`: Main source directory.
- `src/test/kotlin`: Test source directory.

## Dependencies

Key dependencies included in the project:

- `io.insert-koin:koin-ktor`
- `io.insert-koin:koin-logger-slf4j`
- `io.ktor:ktor-server-content-negotiation`
- `io.ktor:ktor-server-core`
- `io.ktor:ktor-serialization-gson`
- `io.ktor:ktor-server-netty`
- `ch.qos.logback:logback-classic`
- `io.ktor:ktor-server-config-yaml`
- `io.ktor:ktor-server-test-host`
- `org.jetbrains.kotlin:kotlin-test-junit`

## Building & Running

To build or run the project, use one of the following tasks:

- `./gradlew test`: Run the tests.
- `./gradlew build`: Build everything.
- `buildFatJar`: Build an executable JAR of the server with all dependencies included.
- `buildImage`: Build the docker image to use with the fat JAR.
- `publishImageToLocalRegistry`: Publish the docker image locally.
- `run`: Run the server.
- `runDocker`: Run using the local docker image.

## Usage

To start the server, run the following command:

```sh
./gradlew run
