import java.io.*;
import java.net.*;
import java.util.*;
import com.sun.net.httpserver.*;

public class TransferServer {
    private static Map<String, String> mappings = new HashMap<>();
    private static Set<String> used = new HashSet<>();

    public static void main(String[] args) throws IOException {
        loadMappings();
        loadUsed();

        HttpServer server = HttpServer.create(new InetSocketAddress(8086), 0);
        server.createContext("/transfer", new TransferHandler());
        server.setExecutor(null);
        System.out.println("Server listening at http://localhost:8086");
        server.start();
    }

    static class TransferHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String responseText = "";
            try {
                if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                    exchange.sendResponseHeaders(405, 0);
                    exchange.getResponseBody().write("Method Not Allowed".getBytes());
                    return;
                }

                URI requestURI = exchange.getRequestURI();
                String query = requestURI.getQuery();

                Map<String, String> params = parseQuery(query);
                String hash = params.get("hash");

                if (hash == null || hash.isEmpty()) {
                    responseText = "{\"error\": \"Missing hash parameter.\"}";
                    sendJson(exchange, 400, responseText);
                    return;
                }

                if (!mappings.containsKey(hash)) {
                    responseText = "{\"error\": \"Hash not found or already used.\"}";
                    sendJson(exchange, 404, responseText);
                    return;
                }

                // Simulate drop to toBank/transfer_<hash>.json
                File out = new File("toBank/transfer_" + hash.substring(0, 8) + ".json");
                out.getParentFile().mkdirs();

                try (FileWriter writer = new FileWriter(out)) {
                    writer.write("{ \"hash\": \"" + hash + "\", \"routing\": \"" + mappings.get(hash) + "\" }");
                }

                used.add(hash);
                mappings.remove(hash);
                persistUsed();

                responseText = "{\"status\": \"Transfer complete.\"}";
                sendJson(exchange, 200, responseText);

            } catch (Exception e) {
                e.printStackTrace();
                responseText = "{\"error\": \"Server exception: " + e.getMessage() + "\"}";
                sendJson(exchange, 500, responseText);
            }
        }
    }

    private static void sendJson(HttpExchange exchange, int code, String body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] bytes = body.getBytes();
        exchange.sendResponseHeaders(code, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.flush();
        os.close();
    }

    private static void loadMappings() {
        try (BufferedReader reader = new BufferedReader(new FileReader("mappings.json"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(":")) {
                    String[] pair = line.replaceAll("[\"{},]", "").split(":");
                    if (pair.length == 2) {
                        mappings.put(pair[0].trim(), pair[1].trim());
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("No mappings.json found, continuing with empty mappings.");
        }
    }

    private static void loadUsed() {
        try (BufferedReader reader = new BufferedReader(new FileReader("used.json"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("\"")) {
                    String clean = line.replaceAll("[\"{},:]", "").trim();
                    if (!clean.isEmpty()) used.add(clean);
                }
            }
        } catch (IOException e) {
            System.out.println("No used.json found, continuing with empty used set.");
        }
    }

    private static void persistUsed() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("used.json"))) {
            writer.write("{\n");
            int i = 0;
            for (String hash : used) {
                writer.write("  \"" + hash + "\": true");
                if (++i < used.size()) writer.write(",\n");
            }
            writer.write("\n}\n");
        } catch (IOException e) {
            System.err.println("Failed to write used.json: " + e.getMessage());
        }
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length == 2) {
                    params.put(pair[0], pair[1]);
                }
            }
        }
        return params;
    }
}
private static void persistMappings() {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter("mappings.json"))) {
        writer.write("{\n");
        int count = 0;
        for (Map.Entry<String, String> entry : mappings.entrySet()) {
            writer.write("  \"" + entry.getKey() + "\": \"" + entry.getValue() + "\"");
            if (++count < mappings.size()) writer.write(",\n");
        }
        writer.write("\n}\n");
    } catch (IOException e) {
        System.err.println("Failed to write mappings.json: " + e.getMessage());
    }
}

