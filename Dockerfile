# syntax=docker/dockerfile:1

# Start with a base image containing Java runtime
FROM openjdk:17-jdk-alpine

# The application's .jar file
ARG JAR_FILE=target/flub-0.0.1-SNAPSHOT.jar

# Make port 8080 available to the world outside this container
EXPOSE 8080

# Add the application's .jar to the container
ADD ${JAR_FILE} app.jar

# Run the .jar file
ENTRYPOINT [ "java" , "-jar" , "/app.jar" ]
