package api;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import controller.UsuarioController;
import entidades.Usuario;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class UsuarioAPI {

    public static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> params = parsePostParams(exchange.getRequestBody());
                String email = params.get("email");
                String senha = params.get("senha");

                UsuarioController controller = new UsuarioController();
                Usuario u = controller.login(email, senha);

                String response;
                int statusCode;
                if (u != null) {
                    statusCode = 200;
                    response = "{\"id\": " + u.getId() + ", \"nome\": \"" + u.getNome() + "\"}";
                } else {
                    statusCode = 401;
                    response = "{\"erro\": \"Credenciais inválidas\"}";
                }

                sendResponse(exchange, statusCode, response);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    public static class CadastroHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> params = parsePostParams(exchange.getRequestBody());
                String nome = params.get("nome");
                String email = params.get("email");
                String senha = params.get("senha");

                UsuarioController controller = new UsuarioController();
                boolean sucesso = controller.cadastrarUsuario(nome, email, senha);

                String response;
                int statusCode;
                if (sucesso) {
                    statusCode = 201;
                    response = "{\"mensagem\": \"Usuário cadastrado com sucesso\"}";
                } else {
                    statusCode = 400;
                    response = "{\"erro\": \"Email já cadastrado\"}";
                }

                sendResponse(exchange, statusCode, response);
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
        String query = sb.toString();
        Map<String, String> result = new HashMap<>();
        if (query.startsWith("{")) {
             query = query.trim().substring(1, query.length() - 1);
             String[] pairs = query.split(",");
             for (String pair : pairs) {
                 String[] keyValue = pair.split(":");
                 if (keyValue.length >= 2) {
                    String key = keyValue[0].trim().replace("\"", "");
                    String value = keyValue[1].trim().replace("\"", "");
                    result.put(key, value);
                 }
             }
        } else {
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length > 1) {
                    result.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8), 
                               URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
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
