package api;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import controller.TransacaoController;
import entidades.Transacao;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TransacaoAPI {

    public static class ComprarHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> params = parsePostParams(exchange.getRequestBody());
                int idUsuario = Integer.parseInt(params.get("idUsuario"));
                int idAtivo = Integer.parseInt(params.get("idAtivo"));
                int quantidade = Integer.parseInt(params.get("quantidade"));
                double preco = Double.parseDouble(params.get("preco"));

                TransacaoController controller = new TransacaoController();
                controller.registrarCompra(idUsuario, idAtivo, quantidade, preco);

                sendResponse(exchange, 201, "{\"mensagem\": \"Compra realizada\"}");
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    public static class VenderHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> params = parsePostParams(exchange.getRequestBody());
                int idUsuario = Integer.parseInt(params.get("idUsuario"));
                int idAtivo = Integer.parseInt(params.get("idAtivo"));
                int quantidade = Integer.parseInt(params.get("quantidade"));
                double preco = Double.parseDouble(params.get("preco"));

                TransacaoController controller = new TransacaoController();
                controller.registrarVenda(idUsuario, idAtivo, quantidade, preco);

                sendResponse(exchange, 201, "{\"mensagem\": \"Venda realizada\"}");
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    public static class ListarHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                String query = exchange.getRequestURI().getQuery();
                int idUsuario = -1;
                if (query != null && query.contains("idUsuario=")) {
                    idUsuario = Integer.parseInt(query.split("idUsuario=")[1].split("&")[0]);
                }

                if (idUsuario != -1) {
                    TransacaoController controller = new TransacaoController();
                    List<Transacao> transacoes = controller.obterTransacoesUsuario(idUsuario);

                    String json = "[" + transacoes.stream()
                            .map(t -> String.format("{\"id\":%d, \"idUsuario\":%d, \"idAtivo\":%d, \"quantidade\":%d, \"precoUnitario\":%.2f, \"tipo\":\"%s\", \"timestamp\":%d}",
                                    t.getId(), t.getIdUsuario(), t.getIdAtivo(), t.getQuantidade(), t.getPrecoUnitario(), t.getTipo(), t.getTimestamp()))
                            .collect(Collectors.joining(",")) + "]";
                    sendResponse(exchange, 200, json);
                } else {
                    sendResponse(exchange, 400, "{\"erro\": \"idUsuario não fornecido\"}");
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    private static Map<String, String> parsePostParams(InputStream body) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        String query = sb.toString().trim();
        Map<String, String> result = new HashMap<>();
        if (query.startsWith("{")) {
             query = query.substring(1, query.length() - 1);
             String[] pairs = query.split(",");
             for (String pair : pairs) {
                 String[] keyValue = pair.split(":");
                 if (keyValue.length >= 2) {
                    String key = keyValue[0].trim().replace("\"", "");
                    String value = keyValue[1].trim().replace("\"", "");
                    result.put(key, value);
                 }
             }
        }
        return result;
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
