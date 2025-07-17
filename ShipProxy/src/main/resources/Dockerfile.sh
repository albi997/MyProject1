# Stage 1: Build the application
FROM openjdk:11-jdk-slim as builder
WORKDIR /app
COPY SequentialProxyServer.java .
RUN javac SequentialProxyServer.java

# Stage 2: Create the runtime image
FROM openjdk:11-jre-slim
WORKDIR /app
COPY --from=builder /app/SequentialProxyServer.class .

# Expose port 8080 for incoming connections
EXPOSE 8080

# Command to run the server
CMD ["java", "SequentialProxyServer"]