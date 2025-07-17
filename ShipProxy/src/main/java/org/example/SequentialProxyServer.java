package org.practice;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class SequentialProxyServer {

    public static void main(String[] args) {
        final int port = 8080;
        System.out.println("Starting sequential proxy server on port: " + port);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                // Accept one client connection at a time
                try (Socket clientSocket = serverSocket.accept()) {
                    System.out.println("Accepted connection from " + clientSocket.getRemoteSocketAddress());
                    handleClientRequest(clientSocket);
                    System.out.println("Finished handling connection from " + clientSocket.getRemoteSocketAddress());
                } catch (Exception e) {
                    System.err.println("Error handling client request: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.err.println("Could not start server on port " + port);
            e.printStackTrace();
        }
    }

    private static void handleClientRequest(Socket clientSocket) {
        try (InputStream fromClient = clientSocket.getInputStream();
             OutputStream toClient = clientSocket.getOutputStream()) {

            byte[] requestBuffer = new byte[4096];
            int bytesRead = fromClient.read(requestBuffer);
            if (bytesRead == -1) {
                return;
            }

            String requestString = new String(requestBuffer, 0, bytesRead);
            System.out.println("Received request:\n" + requestString.substring(0, Math.min(requestString.length(), 500)) + "...");

            // Parse the destination host and port from the request line
            String firstLine = requestString.substring(0, requestString.indexOf('\n'));
            String host = getHostFromRequest(firstLine);
            int remotePort = 80; // Default HTTP port

            if (host == null) {
                System.err.println("Could not parse host from request.");
                return;
            }

            System.out.println("Connecting to remote host: " + host);

            try (Socket remoteSocket = new Socket(host, remotePort);
                 InputStream fromRemote = remoteSocket.getInputStream();
                 OutputStream toRemote = remoteSocket.getOutputStream()) {

                // Forward the original request to the remote server
                toRemote.write(requestBuffer, 0, bytesRead);
                toRemote.flush();

                // Stream the response back to the client
                byte[] responseBuffer = new byte[4096];
                while ((bytesRead = fromRemote.read(responseBuffer)) != -1) {
                    toClient.write(responseBuffer, 0, bytesRead);
                    toClient.flush();
                }
            }

        } catch (Exception e) {
            System.err.println("Error during proxying: " + e.getMessage());
        }
    }

    private static String getHostFromRequest(String requestLine) {
        try {
            // Example Request Line: GET http://httpforever.com/ HTTP/1.1
            String[] parts = requestLine.split(" ");
            if (parts.length > 1) {
                String url = parts[1];
                if (url.startsWith("http://")) {
                    url = url.substring(7);
                }
                return url.split("/")[0].split(":")[0];
            }
        } catch (Exception e) {
            System.err.println("Failed to parse host from request line: " + requestLine);
        }
        return null;
    }
}