import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.*;
import org.json.*;

public class TransferServer {
    public static void main(String[] args) throws Exception {
        int port = 8086;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        String mappingContent = Files.readString(Paths.get("mappings.json"));
        JSONObject ledger = new JSONObject(mappingContent);

        server.createContext("/lookup", exchange -> {
            String query = exchange.getRequestURI().getQuery();  // ?hash=abc...
            String hash = query != null && query.startsWith("hash=") ? query.substring(5) : null;
            JSONObject result = ledger.has(hash) ? ledger.getJSONObject(hash) : new JSONObject();

            String response = result.toString(2);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
        });

        server.setExecutor(null);
        server.start();
        System.out.println("Server running on port " + port);
    }
}

