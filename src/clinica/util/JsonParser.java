package clinica.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parser JSON mínimo para leitura de objetos planos {"chave":"valor",...}. Não
 * depende de bibliotecas externas. Suporta strings, números e booleans como
 * valores — tudo convertido para String pelo chamador.
 */
public class JsonParser {

    /**
     * Parseia um objeto JSON simples e retorna um Map<String,String>. Valores
     * numéricos e booleanos são retornados como String. Não suporta arrays nem
     * objetos aninhados.
     */
    public static Map<String, String> parse(String json) {
        Map<String, String> map = new LinkedHashMap<>();
        if (json == null || json.isBlank()) {
            return map;
        }

        json = json.trim();
        // Remove chaves externas { }
        if (json.startsWith("{")) {
            json = json.substring(1);
        }
        if (json.endsWith("}")) {
            json = json.substring(0, json.length() - 1);
        }

        int i = 0;
        while (i < json.length()) {
            // Pula espaços
            while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
                i++;
            }
            if (i >= json.length()) {
                break;
            }

            // Lê chave (string entre aspas)
            if (json.charAt(i) != '"') {
                i++;
                continue;
            }
            int keyStart = ++i;
            while (i < json.length() && json.charAt(i) != '"') {
                if (json.charAt(i) == '\\') {
                    i++; // skip escaped char

                                }i++;
            }
            String key = json.substring(keyStart, i);
            i++; // fecha "

            // Pula ':'
            while (i < json.length() && json.charAt(i) != ':') {
                i++;
            }
            i++;
            while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
                i++;
            }

            // Lê valor
            String value;
            if (i >= json.length()) {
                break;
            }
            char c = json.charAt(i);
            if (c == '"') {
                // Valor string
                i++;
                StringBuilder sb = new StringBuilder();
                while (i < json.length() && json.charAt(i) != '"') {
                    if (json.charAt(i) == '\\' && i + 1 < json.length()) {
                        char esc = json.charAt(i + 1);
                        switch (esc) {
                            case '"' ->
                                sb.append('"');
                            case '\\' ->
                                sb.append('\\');
                            case 'n' ->
                                sb.append('\n');
                            case 'r' ->
                                sb.append('\r');
                            case 't' ->
                                sb.append('\t');
                            default -> {
                                sb.append('\\');
                                sb.append(esc);
                            }
                        }
                        i += 2;
                    } else {
                        sb.append(json.charAt(i++));
                    }
                }
                value = sb.toString();
                i++; // fecha "
            } else {
                // Valor não-string (número, boolean, null)
                int start = i;
                while (i < json.length() && json.charAt(i) != ',' && json.charAt(i) != '}') {
                    i++;
                }
                value = json.substring(start, i).trim();
            }

            map.put(key, value);

            // Pula vírgula separadora
            while (i < json.length() && (json.charAt(i) == ',' || Character.isWhitespace(json.charAt(i)))) {
                i++;
            }
        }
        return map;
    }

    /**
     * Gera resposta JSON de sucesso simples: {"ok":true,"msg":"..."}
     */
    public static String ok(String msg) {
        return "{\"ok\":true,\"msg\":\"" + esc(msg) + "\"}";
    }

    /**
     * Gera resposta JSON de erro simples: {"ok":false,"erro":"..."}
     */
    public static String err(String msg) {
        return "{\"ok\":false,\"erro\":\"" + esc(msg == null ? "Erro desconhecido" : msg) + "\"}";
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }
}
