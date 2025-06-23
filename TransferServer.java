import com.sun.net.httpserver.*;
import java.io.*;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.*;
import org.json.*;

public class TransferServer {
    private static final int PORT = 8086;
    private static final String MAPPINGS_FILE = "mappings.json";
    private static final String LEDGER_LOG = "ledger.log";
    private static final String QUEUE_FILE = "pendingTransfers.txt";
    private static JSONObject ledger;

    public static void main(String[] args) throws Exception {
        System.out.println("Loading mappings from " + MAPPINGS_FILE);
        String mappingContent = Files.readString(Paths.get(MAPPINGS_FILE));
        ledger = new JSONObject(mappingContent);

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/lookup", TransferServer::handleLookup);
        server.createContext("/transfer", TransferServer::handleTransfer);
        server.setExecutor(null);
        server.start();
        System.out.println("TransferServer running on port " + PORT);
    }

    private static void handleLookup(HttpExchange exchange) throws IOException {
        String hash = getHashQuery(exchange);
        JSONObject result = ledger.has(hash) ? ledger.getJSONObject(hash) : new JSONObject();
        sendJson(exchange, result.toString(2));
    }

    private static void handleTransfer(HttpExchange exchange) throws IOException {
        String hash = getHashQuery(exchange);
        JSONObject entry = ledger.has(hash) ? ledger.getJSONObject(hash) : null;

        String response;
        if (entry != null) {
            BigDecimal amount = new BigDecimal(entry.getString("amount"));
            String routing = entry.getString("routing");
            String account = entry.getString("account");
            String timestamp = new Date().toString();
            String reference = UUID.randomUUID().toString();

            // Build response object
            JSONObject receipt = new JSONObject()
                .put("status", "TRANSFER_AUTHORIZED")
                .put("routing", routing)
                .put("account", account)
                .put("amount", amount.toPlainString())
                .put("timestamp", timestamp)
                .put("reference", reference);
            response = receipt.toString(2);

            // Write to pendingTransfers.txt
            JSONObject transferInstruction = new JSONObject()
                .put("hash", hash)
                .put("routing", routing)
                .put("account", account)
                .put("amount", amount.toPlainString())
                .put("timestamp", timestamp)
                .put("reference", reference);
            Files.writeString(Paths.get(QUEUE_FILE), transferInstruction + "\n",
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            // Log to ledger.log
            String logLine = String.format(
                "[%s] TRANSFER %s USD → %s/%s (%s)\n",
                timestamp, amount.toPlainString(), routing, account, hash);
            Files.writeString(Paths.get(LEDGER_LOG), logLine,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        } else {
            response = "{\"error\": \"Invalid or unknown hash\"}";
        }

        sendJson(exchange, response);
    }

    private static String getHashQuery(HttpExchange exchange) {
        String query = exchange.getRequestURI().getQuery();
        return (query != null && query.startsWith("hash=")) ? query.substring(5) : null;
    }

    private static void sendJson(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes();
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}

