package app;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import api.*;

public class ServidorWeb {
    public static void iniciar() throws IOException {
        int porta = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(porta), 0);

        // Rotas de API
        server.createContext("/api/login", new UsuarioAPI.LoginHandler());
        server.createContext("/api/cadastro", new UsuarioAPI.CadastroHandler());
        server.createContext("/api/ativos", new AtivoAPI.ListarHandler());
        server.createContext("/api/comprar", new TransacaoAPI.ComprarHandler());
        server.createContext("/api/vender", new TransacaoAPI.VenderHandler());
        server.createContext("/api/transacoes", new TransacaoAPI.ListarHandler());

        // Rota para arquivos estáticos (HTML/CSS/JS) na pasta src/view
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(null);
        System.out.println("Servidor rodando em http://localhost:" + porta);
        server.start();
    }

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/index.html";
            }

            Path file = Paths.get("src/view" + path);
            if (Files.exists(file) && !Files.isDirectory(file)) {
                byte[] bytes = Files.readAllBytes(file);
                String contentType = "text/html";
                if (path.endsWith(".css")) contentType = "text/css";
                if (path.endsWith(".js")) contentType = "application/javascript";
                
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                String response = "404 (Not Found): " + path;
                exchange.sendResponseHeaders(404, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        }
    }
}
