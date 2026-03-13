package api;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import controller.AtivoController;
import entidades.Ativo;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class AtivoAPI {

    public static class ListarHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                AtivoController controller = new AtivoController();
                List<Ativo> ativos = controller.listarAtivos();

                String json = "[" + ativos.stream()
                        .map(a -> String.format("{\"id\":%d, \"nome\":\"%s\", \"ticker\":\"%s\", \"tipo\":\"%s\", \"precoAtual\":%.2f}",
                                a.getId(), a.getNome(), a.getTicker(), a.getTipo(), a.getPrecoAtual()))
                        .collect(Collectors.joining(",")) + "]";

                sendResponse(exchange, 200, json);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
