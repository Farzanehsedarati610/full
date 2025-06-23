import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

public class TransferServer {
    static final int PORT = 8080;

    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Vault TransferServer running on port " + PORT);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            new Thread(() -> handleClient(clientSocket)).start();
        }
    }

    static void handleClient(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

            String line;
            while ((line = in.readLine()) != null) {
                if (line.equals("EXECUTE_VAULT_SUBSTITUTION")) {
                    runSubstitutions();
                    out.write("Vault substitution executed.\n");
                    out.flush();
                    break;
                }
            }

        } catch (IOException e) {
            System.err.println("Vault bridge error: " + e.getMessage());
        }
    }

    static void runSubstitutions() {
        try {
            List<String> mappings = Files.readAllLines(Paths.get("/vault/vault.subst"));

            Files.walk(Paths.get("/var/www"))
                .filter(p -> p.toString().endsWith(".html"))
                .forEach(file -> {
                    try {
                        String content = Files.readString(file);
                        for (String map : mappings) {
                            String[] parts = map.split("=", 2);
                            if (parts.length == 2)
                                content = content.replace(parts[0], parts[1]);
                        }
                        Files.writeString(file, content);
                    } catch (IOException e) {
                        System.err.println("Substitution failed for: " + file);
                    }
                });

        } catch (IOException e) {
            System.err.println("Failed to read vault.subst: " + e.getMessage());
        }
    }
}

