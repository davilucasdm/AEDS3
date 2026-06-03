package clinica.controller;

import clinica.model.*;
import clinica.service.ClinicaService;
import clinica.util.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Executors;

public class ClinicaHttpServer {

    private final ClinicaService svc;
    private final String webDir;
    private HttpServer server;

    // Sessões simples: token → login
    private final Map<String, String> sessoes = new java.util.concurrent.ConcurrentHashMap<>();

    public ClinicaHttpServer(ClinicaService svc, String webDir) {
        this.svc = svc;
        this.webDir = webDir;
    }

    public void iniciar(int porta) throws IOException {
        server = HttpServer.create(new InetSocketAddress(porta), 0);
        server.setExecutor(Executors.newFixedThreadPool(4));

        // Arquivos estáticos
        server.createContext("/", this::staticHandler);

        // API REST
        server.createContext("/api/login", this::handleLogin);
        server.createContext("/api/pacientes", this::handlePacientes);
        server.createContext("/api/medicos", this::handleMedicos);
        server.createContext("/api/especialidades", this::handleEspecialidades);
        server.createContext("/api/consultas", this::handleConsultas);
        server.createContext("/api/vinculos", this::handleVinculos);
        server.createContext("/api/usuarios", this::handleUsuarios);
        server.createContext("/api/ordenar", this::handleOrdenar);
        server.createContext("/api/listar-ordenado", this::handleListarOrdenado);

        server.start();
        System.out.println("Clínica iniciada em http://localhost:" + porta);
    }

    public void parar() {
        if (server != null) {
            server.stop(0);
    
        }}

    // ─── Arquivos estáticos ─────────────────────────────────────────────────
    private void staticHandler(HttpExchange ex) throws IOException {
        String uri = ex.getRequestURI().getPath();
        if ("/".equals(uri)) {
            uri = "/index.html";
        }
        File f = new File(webDir + uri);
        if (!f.exists() || f.isDirectory()) {
            respond(ex, 404, "text/plain", "Not found");
            return;
        }
        String ct = contentType(uri);
        byte[] body = Files.readAllBytes(f.toPath());
        ex.getResponseHeaders().set("Content-Type", ct);
        ex.sendResponseHeaders(200, body.length);
        ex.getResponseBody().write(body);
        ex.getResponseBody().close();
    }

    private String contentType(String uri) {
        if (uri.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        if (uri.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (uri.endsWith(".js")) {
            return "application/javascript; charset=utf-8";
        }
        if (uri.endsWith(".json")) {
            return "application/json";
        }
        return "application/octet-stream";
    }

    // ─── Login ──────────────────────────────────────────────────────────────
    private void handleLogin(HttpExchange ex) throws IOException {
        cors(ex);
        if ("OPTIONS".equals(ex.getRequestMethod())) {
            respond(ex, 204, "text/plain", "");
            return;
        }
        if (!"POST".equals(ex.getRequestMethod())) {
            respond(ex, 405, "application/json", JsonParser.err("Método inválido"));
            return;
        }

        Map<String, String> body = JsonParser.parse(readBody(ex));
        String login = body.getOrDefault("login", "");
        String senha = body.getOrDefault("senha", "");

        try {
            if (svc.login(login, senha)) {
                String token = UUID.randomUUID().toString();
                sessoes.put(token, login);
                Usuario u = svc.buscarUsuarioPorLogin(login);
                respond(ex, 200, "application/json",
                        "{\"ok\":true,\"token\":\"" + token + "\",\"role\":\"" + u.getRole() + "\",\"login\":\"" + login + "\"}");
            } else {
                respond(ex, 401, "application/json", JsonParser.err("Credenciais inválidas"));
            }
        } catch (Exception e) {
            respond(ex, 500, "application/json", JsonParser.err(e.getMessage()));
        }
    }

    // ─── Pacientes ──────────────────────────────────────────────────────────
    private void handlePacientes(HttpExchange ex) throws IOException {
        cors(ex);
        if ("OPTIONS".equals(ex.getRequestMethod())) {
            respond(ex, 204, "text/plain", "");
            return;
        }
        if (!autenticado(ex)) {
            respond(ex, 401, "application/json", JsonParser.err("Não autenticado"));
            return;
        }

        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();   // /api/pacientes  ou /api/pacientes/3
        String[] parts = path.split("/");
        boolean temId = parts.length >= 4;
        int id = temId ? safeInt(parts[3]) : 0;

        try {
            switch (method) {
                case "GET" -> {
                    String q = queryParam(ex, "q");
                    if (q != null && !q.isEmpty()) {
                        respondJson(ex, listJson(svc.buscarPacienteNome(q), Paciente::toJson));
                    } else if (temId) {
                        Paciente p = svc.buscarPaciente(id);
                        if (p == null) {
                            respond(ex, 404, "application/json", JsonParser.err("Não encontrado")); 
                        }else {
                            respondJson(ex, p.toJson());
                        }
                    } else {
                        respondJson(ex, listJson(svc.listarPacientes(), Paciente::toJson));
                    }
                }
                case "POST" -> {
                    Map<String, String> b = JsonParser.parse(readBody(ex));
                    Paciente p = new Paciente(0, true,
                            b.get("nome"), b.get("cpf"), b.get("dataNascimento"),
                            b.get("email"), b.get("telefones"), b.get("endereco"));
                    int novoId = svc.criarPaciente(p);
                    respondJson(ex, "{\"ok\":true,\"id\":" + novoId + "}");
                }
                case "PUT" -> {
                    if (!temId) {
                        respond(ex, 400, "application/json", JsonParser.err("ID obrigatório"));
                        return;
                    }
                    Map<String, String> b = JsonParser.parse(readBody(ex));
                    Paciente p = new Paciente(id, true,
                            b.get("nome"), b.get("cpf"), b.get("dataNascimento"),
                            b.get("email"), b.get("telefones"), b.get("endereco"));
                    respondJson(ex, svc.atualizarPaciente(p) ? JsonParser.ok("Atualizado") : JsonParser.err("Falha"));
                }
                case "DELETE" -> {
                    if (!temId) {
                        respond(ex, 400, "application/json", JsonParser.err("ID obrigatório"));
                        return;
                    }
                    respondJson(ex, svc.deletarPaciente(id) ? JsonParser.ok("Removido") : JsonParser.err("Falha"));
                }
                default ->
                    respond(ex, 405, "application/json", JsonParser.err("Método inválido"));
            }
        } catch (Exception e) {
            respond(ex, 500, "application/json", JsonParser.err(e.getMessage()));
        }
    }

    // ─── Médicos ────────────────────────────────────────────────────────────
    private void handleMedicos(HttpExchange ex) throws IOException {
        cors(ex);
        if ("OPTIONS".equals(ex.getRequestMethod())) {
            respond(ex, 204, "text/plain", "");
            return;
        }
        if (!autenticado(ex)) {
            respond(ex, 401, "application/json", JsonParser.err("Não autenticado"));
            return;
        }

        String method = ex.getRequestMethod();
        String[] parts = ex.getRequestURI().getPath().split("/");
        boolean temId = parts.length >= 4;
        int id = temId ? safeInt(parts[3]) : 0;

        try {
            switch (method) {
                case "GET" -> {
                    String q = queryParam(ex, "q");
                    if (q != null && !q.isEmpty()) {
                        respondJson(ex, listJson(svc.buscarMedicoNome(q), Medico::toJson));
                    } else if (temId) {
                        Medico m = svc.buscarMedico(id);
                        if (m == null) {
                            respond(ex, 404, "application/json", JsonParser.err("Não encontrado")); 
                        }else {
                            respondJson(ex, m.toJson());
                        }
                    } else {
                        respondJson(ex, listJson(svc.listarMedicos(), Medico::toJson));
                    }
                }
                case "POST" -> {
                    Map<String, String> b = JsonParser.parse(readBody(ex));
                    Medico m = new Medico(0, true, b.get("nome"), b.get("crm"), b.get("email"), b.get("telefones"));
                    respondJson(ex, "{\"ok\":true,\"id\":" + svc.criarMedico(m) + "}");
                }
                case "PUT" -> {
                    if (!temId) {
                        respond(ex, 400, "application/json", JsonParser.err("ID obrigatório"));
                        return;
                    }
                    Map<String, String> b = JsonParser.parse(readBody(ex));
                    Medico m = new Medico(id, true, b.get("nome"), b.get("crm"), b.get("email"), b.get("telefones"));
                    respondJson(ex, svc.atualizarMedico(m) ? JsonParser.ok("Atualizado") : JsonParser.err("Falha"));
                }
                case "DELETE" -> {
                    if (!temId) {
                        respond(ex, 400, "application/json", JsonParser.err("ID obrigatório"));
                        return;
                    }
                    respondJson(ex, svc.deletarMedico(id) ? JsonParser.ok("Removido") : JsonParser.err("Falha"));
                }
                default ->
                    respond(ex, 405, "application/json", JsonParser.err("Método inválido"));
            }
        } catch (Exception e) {
            respond(ex, 500, "application/json", JsonParser.err(e.getMessage()));
        }
    }

    // ─── Especialidades ─────────────────────────────────────────────────────
    private void handleEspecialidades(HttpExchange ex) throws IOException {
        cors(ex);
        if ("OPTIONS".equals(ex.getRequestMethod())) {
            respond(ex, 204, "text/plain", "");
            return;
        }
        if (!autenticado(ex)) {
            respond(ex, 401, "application/json", JsonParser.err("Não autenticado"));
            return;
        }

        String method = ex.getRequestMethod();
        String[] parts = ex.getRequestURI().getPath().split("/");
        boolean temId = parts.length >= 4;
        int id = temId ? safeInt(parts[3]) : 0;

        try {
            switch (method) {
                case "GET" -> {
                    if (temId) {
                        Especialidade e = svc.buscarEsp(id);
                        if (e == null) {
                            respond(ex, 404, "application/json", JsonParser.err("Não encontrado")); 
                        }else {
                            respondJson(ex, e.toJson());
                        }
                    } else {
                        respondJson(ex, listJson(svc.listarEsps(), Especialidade::toJson));
                    }
                }
                case "POST" -> {
                    Map<String, String> b = JsonParser.parse(readBody(ex));
                    Especialidade e = new Especialidade(0, true, b.get("nome"), b.get("descricao"));
                    respondJson(ex, "{\"ok\":true,\"id\":" + svc.criarEsp(e) + "}");
                }
                case "PUT" -> {
                    if (!temId) {
                        respond(ex, 400, "application/json", JsonParser.err("ID obrigatório"));
                        return;
                    }
                    Map<String, String> b = JsonParser.parse(readBody(ex));
                    Especialidade e = new Especialidade(id, true, b.get("nome"), b.get("descricao"));
                    respondJson(ex, svc.atualizarEsp(e) ? JsonParser.ok("Atualizado") : JsonParser.err("Falha"));
                }
                case "DELETE" -> {
                    if (!temId) {
                        respond(ex, 400, "application/json", JsonParser.err("ID obrigatório"));
                        return;
                    }
                    respondJson(ex, svc.deletarEsp(id) ? JsonParser.ok("Removido") : JsonParser.err("Falha"));
                }
                default ->
                    respond(ex, 405, "application/json", JsonParser.err("Método inválido"));
            }
        } catch (Exception e) {
            respond(ex, 500, "application/json", JsonParser.err(e.getMessage()));
        }
    }

    // ─── Consultas ──────────────────────────────────────────────────────────
    private void handleConsultas(HttpExchange ex) throws IOException {
        cors(ex);
        if ("OPTIONS".equals(ex.getRequestMethod())) {
            respond(ex, 204, "text/plain", "");
            return;
        }
        if (!autenticado(ex)) {
            respond(ex, 401, "application/json", JsonParser.err("Não autenticado"));
            return;
        }

        String method = ex.getRequestMethod();
        String[] parts = ex.getRequestURI().getPath().split("/");
        boolean temId = parts.length >= 4;
        int id = temId ? safeInt(parts[3]) : 0;

        try {
            switch (method) {
                case "GET" -> {
                    String pacId = queryParam(ex, "paciente");
                    String medId = queryParam(ex, "medico");
                    if (pacId != null) {
                        respondJson(ex, listJson(svc.consultasPorPaciente(Integer.parseInt(pacId)), Consulta::toJson));
                    } else if (medId != null) {
                        respondJson(ex, listJson(svc.consultasPorMedico(Integer.parseInt(medId)), Consulta::toJson));
                    } else if (temId) {
                        Consulta c = svc.buscarConsulta(id);
                        if (c == null) {
                            respond(ex, 404, "application/json", JsonParser.err("Não encontrado")); 
                        }else {
                            respondJson(ex, c.toJson());
                        }
                    } else {
                        respondJson(ex, listJson(svc.listarConsultas(), Consulta::toJson));
                    }
                }
                case "POST" -> {
                    Map<String, String> b = JsonParser.parse(readBody(ex));
                    Consulta c = new Consulta(0, true,
                            safeInt(b.get("idPaciente")), safeInt(b.get("idMedico")),
                            b.get("data"), b.get("horario"),
                            safeDouble(b.get("valor")),
                            b.get("sintomas"), b.getOrDefault("status", "AGENDADA"),
                            b.getOrDefault("observacoes", ""));
                    respondJson(ex, "{\"ok\":true,\"id\":" + svc.criarConsulta(c) + "}");
                }
                case "PUT" -> {
                    if (!temId) {
                        respond(ex, 400, "application/json", JsonParser.err("ID obrigatório"));
                        return;
                    }
                    Map<String, String> b = JsonParser.parse(readBody(ex));
                    Consulta c = new Consulta(id, true,
                            safeInt(b.get("idPaciente")), safeInt(b.get("idMedico")),
                            b.get("data"), b.get("horario"),
                            safeDouble(b.get("valor")),
                            b.get("sintomas"), b.getOrDefault("status", "AGENDADA"),
                            b.getOrDefault("observacoes", ""));
                    respondJson(ex, svc.atualizarConsulta(c) ? JsonParser.ok("Atualizado") : JsonParser.err("Falha"));
                }
                case "DELETE" -> {
                    if (!temId) {
                        respond(ex, 400, "application/json", JsonParser.err("ID obrigatório"));
                        return;
                    }
                    respondJson(ex, svc.deletarConsulta(id) ? JsonParser.ok("Removido") : JsonParser.err("Falha"));
                }
                default ->
                    respond(ex, 405, "application/json", JsonParser.err("Método inválido"));
            }
        } catch (Exception e) {
            respond(ex, 500, "application/json", JsonParser.err(e.getMessage()));
        }
    }

    // ─── Vínculos Médico↔Especialidade ─────────────────────────────────────
    private void handleVinculos(HttpExchange ex) throws IOException {
        cors(ex);
        if ("OPTIONS".equals(ex.getRequestMethod())) {
            respond(ex, 204, "text/plain", "");
            return;
        }
        if (!autenticado(ex)) {
            respond(ex, 401, "application/json", JsonParser.err("Não autenticado"));
            return;
        }

        String method = ex.getRequestMethod();
        String medId = queryParam(ex, "medico");
        String espId = queryParam(ex, "especialidade");

        try {
            switch (method) {
                case "GET" -> {
                    if (medId != null) {
                        respondJson(ex, listJson(svc.especialidadesDeMedico(Integer.parseInt(medId)), Especialidade::toJson));
                    } else if (espId != null) {
                        respondJson(ex, listJson(svc.medicosPorEspecialidade(Integer.parseInt(espId)), Medico::toJson));
                    } else {
                        respond(ex, 400, "application/json", JsonParser.err("Informe medico ou especialidade"));
                    }
                }
                case "POST" -> {
                    Map<String, String> b = JsonParser.parse(readBody(ex));
                    int r = svc.vincularMedicoEsp(safeInt(b.get("idMedico")), safeInt(b.get("idEspecialidade")));
                    respondJson(ex, "{\"ok\":true,\"id\":" + r + "}");
                }
                case "DELETE" -> {
                    Map<String, String> b = JsonParser.parse(readBody(ex));
                    boolean ok = svc.desvincularMedicoEsp(safeInt(b.get("idMedico")), safeInt(b.get("idEspecialidade")));
                    respondJson(ex, ok ? JsonParser.ok("Desvinculado") : JsonParser.err("Falha"));
                }
                default ->
                    respond(ex, 405, "application/json", JsonParser.err("Método inválido"));
            }
        } catch (Exception e) {
            respond(ex, 500, "application/json", JsonParser.err(e.getMessage()));
        }
    }

    // ─── Ordenação Externa ──────────────────────────────────────────────────
    private void handleOrdenar(HttpExchange ex) throws IOException {
        cors(ex);
        if ("OPTIONS".equals(ex.getRequestMethod())) {
            respond(ex, 204, "text/plain", "");
            return;
        }
        if (!autenticado(ex)) {
            respond(ex, 401, "application/json", JsonParser.err("Não autenticado"));
            return;
        }

        // GET /api/ordenar?entidade=pacientes&campo=nome&ordem=asc
        String entidade = queryParam(ex, "entidade");   // pacientes | medicos | consultas
        String campo = queryParam(ex, "campo");       // nome | data | valor
        boolean desc = "desc".equalsIgnoreCase(queryParam(ex, "ordem"));

        try {
            String arquivo = switch (entidade + "." + campo) {
                case "pacientes.nome" ->
                    svc.ordenarPacientesPorNome(desc);
                case "medicos.nome" ->
                    svc.ordenarMedicosPorNome(desc);
                case "consultas.data" ->
                    svc.ordenarConsultasPorData(desc);
                case "consultas.valor" ->
                    svc.ordenarConsultasPorValor(desc);
                default ->
                    throw new IllegalArgumentException("Entidade/campo não suportado: " + entidade + "." + campo);
            };
            respondJson(ex, "{\"ok\":true,\"arquivo\":\"" + arquivo + "\",\"msg\":\"Ordenação concluída\"}");
        } catch (IllegalArgumentException e) {
            respond(ex, 400, "application/json", JsonParser.err(e.getMessage()));
        } catch (Exception e) {
            respond(ex, 500, "application/json", JsonParser.err(e.getMessage()));
        }
    }

    // ─── Usuários ────────────────────────────────────────────────────────────
    private void handleUsuarios(HttpExchange ex) throws IOException {
        cors(ex);
        if ("OPTIONS".equals(ex.getRequestMethod())) {
            respond(ex, 204, "text/plain", "");
            return;
        }
        if (!autenticado(ex)) {
            respond(ex, 401, "application/json", JsonParser.err("Não autenticado"));
            return;
        }

        String method = ex.getRequestMethod();
        String[] parts = ex.getRequestURI().getPath().split("/");
        boolean temId = parts.length >= 4;
        int id = temId ? safeInt(parts[3]) : 0;

        try {
            switch (method) {
                case "GET" ->
                    respondJson(ex, listJson(svc.listarUsuarios(), u -> u.toString()));
                case "POST" -> {
                    Map<String, String> b = JsonParser.parse(readBody(ex));
                    Usuario u = new Usuario(0, true, b.get("login"), b.get("senha"), b.getOrDefault("role", "RECEPCIONISTA"));
                    respondJson(ex, "{\"ok\":true,\"id\":" + svc.criarUsuario(u) + "}");
                }
                case "DELETE" -> {
                    if (!temId) {
                        respond(ex, 400, "application/json", JsonParser.err("ID obrigatório"));
                        return;
                    }
                    respondJson(ex, svc.deletarUsuario(id) ? JsonParser.ok("Removido") : JsonParser.err("Falha"));
                }
                default ->
                    respond(ex, 405, "application/json", JsonParser.err("Método inválido"));
            }
        } catch (Exception e) {
            respond(ex, 500, "application/json", JsonParser.err(e.getMessage()));
        }
    }

    // ─── Listagem ordenada via Árvore B+ (Req 4) ────────────────────────────
    /**
     * GET /api/listar-ordenado?entidade=medicos GET
     * /api/listar-ordenado?entidade=consultas
     *
     * Retorna os registros em ordem crescente de ID diretamente pela travessia
     * das folhas encadeadas da Árvore B+, sem ordenação em memória principal
     * (sem Collections.sort ou Arrays.sort).
     *
     * Demonstra o Requisito 4 da Fase III.
     */
    private void handleListarOrdenado(HttpExchange ex) throws IOException {
        cors(ex);
        if ("OPTIONS".equals(ex.getRequestMethod())) {
            respond(ex, 204, "text/plain", "");
            return;
        }
        if (!autenticado(ex)) {
            respond(ex, 401, "application/json", JsonParser.err("Não autenticado"));
            return;
        }

        String entidade = queryParam(ex, "entidade");

        try {
            String json = switch (entidade == null ? "" : entidade) {
                case "medicos" ->
                    listJson(svc.listarMedicosOrdenadosBPlus(), Medico::toJson);
                case "consultas" ->
                    listJson(svc.listarConsultasOrdenadas(), Consulta::toJson);
                default ->
                    throw new IllegalArgumentException(
                            "Entidade não suportada. Use: medicos | consultas");
            };
            respondJson(ex, json);
        } catch (IllegalArgumentException e) {
            respond(ex, 400, "application/json", JsonParser.err(e.getMessage()));
        } catch (Exception e) {
            respond(ex, 500, "application/json", JsonParser.err(e.getMessage()));
        }
    }

    // ─── Helpers ────────────────────────────────────────────────────────────
    private boolean autenticado(HttpExchange ex) {
        String auth = ex.getRequestHeaders().getFirst("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return false;
        }
        return sessoes.containsKey(auth.substring(7));
    }

    private void cors(HttpExchange ex) {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type,Authorization");
    }

    private void respondJson(HttpExchange ex, String json) throws IOException {
        respond(ex, 200, "application/json", json);
    }

    private void respond(HttpExchange ex, int code, String ct, String body) throws IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", ct + "; charset=utf-8");
        ex.sendResponseHeaders(code, b.length);
        ex.getResponseBody().write(b);
        ex.getResponseBody().close();
    }

    private String readBody(HttpExchange ex) throws IOException {
        return new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private String queryParam(HttpExchange ex, String key) {
        String q = ex.getRequestURI().getQuery();
        if (q == null) {
            return null;
        }
        for (String pair : q.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) {
                return kv[1];
            }
        }
        return null;
    }

    private int safeInt(String s) {
        try {
            return Integer.parseInt(s == null ? "0" : s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private double safeDouble(String s) {
        try {
            return Double.parseDouble(s == null ? "0" : s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    @FunctionalInterface
    interface ToJson<T> {

        String apply(T t);
    }

    private <T> String listJson(List<T> list, ToJson<T> fn) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(fn.apply(list.get(i)));
        }
        return sb.append("]").toString();
    }
}
