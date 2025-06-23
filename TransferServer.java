import java.io.*;
import java.net.*;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

public class TransferServer {

    private static final int PORT = 8086;
    private static final String MAPPINGS_FILE = "mappings.json";
    private static final String USED_FILE = "used.json";
    private static final String LOG_FILE = "ledger.log";
    private static final String PENDING_FILE = "pendingTransfers.txt";

    private static JSONObject ledger = new JSONObject();
    private static JSONObject used = new JSONObject();

    public static void main(String[] args) throws Exception {
        loadMappings();

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/transfer", TransferServer::handleTransfer);
        server.setExecutor(Executors.newCachedThreadPool());
        System.out.println("TransferServer running on port " + PORT);
        server.start();
    }
public void handleTransfer(HttpServletRequest request, HttpServletResponse response) throws IOException {
    try {
        // transfer logic here
    } catch (Exception e) {
        e.printStackTrace();
        response.setStatus(500);
        response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        response.getWriter().flush();
        response.getWriter().close();
    }
}
    private static void loadMappings() throws IOException {
        // Load mappings.json
        String mappingContent = Files.readString(Paths.get(MAPPINGS_FILE));
        ledger = new JSONObject(mappingContent);
        if (!mappings.containsKey(hash)) {
            response.setStatus(404);
            response.getWriter().write("{\"error\": \"Hash not found or already used.\"}");
            return;
    }

        // Load or initialize used.json
        File usedFile = new File(USED_FILE);
        if (!usedFile.exists()) Files.writeString(usedFile.toPath(), "{}");
        String usedContent = Files.readString(Paths.get(USED_FILE));
        used = new JSONObject(usedContent);
    }
        try {
    // your transfer logic here
    }
        catch (Exception e) {
             e.printStackTrace(); // optional logging
             response.setStatus(500);
             response.getWriter().write("{\"error\": \"Server error: " + e.getMessage() + "\"}");
             response.setStatus(200);
             response.getWriter().write("{\"status\": \"Transfer complete.\"}");
             response.getWriter().flush();
             response.getWriter().close();
    }

    private static void handleTransfer(HttpExchange exchange) throws IOException {
        URI uri = exchange.getRequestURI();
        String query = uri.getQuery();

        if (query == null || !query.startsWith("hash=")) {
            sendJson(exchange, "{\"error\": \"Missing or invalid hash parameter.\"}");
            return;
        }
        String hash = request.getParameter("hash");
        if (hash == null || hash.isEmpty()) {
            response.setStatus(400);
            response.getWriter().write("{\"error\": \"Missing hash parameter.\"}");
            return;
        }

        String hash = query.substring(5);
        if (!ledger.has(hash)) {
            sendJson(exchange, "{\"error\": \"Unknown hash.\"}");
            return;
        }

        if (used.has(hash)) {
            sendJson(exchange, "{\"error\": \"This hash has already been used.\"}");
            return;
        }

        JSONObject entry = ledger.getJSONObject(hash);
        String routing = entry.getString("routing");
        String account = entry.getString("account");
        String amount = entry.getString("amount");

        String timestamp = new Date().toString();
        String reference = UUID.randomUUID().toString();

        JSONObject payload = new JSONObject();
        payload.put("hash", hash);
        payload.put("routing", routing);
        payload.put("account", account);
        payload.put("amount", amount);
        payload.put("timestamp", timestamp);
        payload.put("reference", reference);

        // ✅ Write to pendingTransfers.txt
        Files.writeString(Paths.get(PENDING_FILE), payload.toString() + "\n",
            StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        // ✅ Append to ledger.log
        Files.writeString(Paths.get(LOG_FILE),
            "[" + timestamp + "] TRANSFER " + amount + " USD → " + routing + "/" + account + " (" + hash + ")\n",
            StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        // ✅ Write .json file to toBank/
        Files.writeString(Paths.get("toBank/transfer_" + reference + ".json"),
            payload.toString(), StandardOpenOption.CREATE);

        // ✅ Mark hash as used
        used.put(hash, true);
        Files.writeString(Paths.get(USED_FILE), used.toString(2));

        // ✅ Optionally remove from mappings
        ledger.remove(hash);
        Files.writeString(Paths.get(MAPPINGS_FILE), ledger.toString(2));

        // Return transfer receipt
        payload.put("status", "TRANSFER_AUTHORIZED");
        sendJson(exchange, payload.toString());
    }

    private static void sendJson(HttpExchange exchange, String response) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}

