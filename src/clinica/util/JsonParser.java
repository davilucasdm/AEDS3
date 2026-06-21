package clinica.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class JsonParser {

    public static Map<String, String> parse(String json) {
        Map<String, String> map = new LinkedHashMap<>();
        if (json == null || json.isBlank()) {
            return map;
        }

        json = json.trim();
        if (json.startsWith("{")) {
            json = json.substring(1);
        }
        if (json.endsWith("}")) {
            json = json.substring(0, json.length() - 1);
        }

        int i = 0;
        while (i < json.length()) {
            while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
                i++;
            }
            if (i >= json.length()) {
                break;
            }

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

            while (i < json.length() && json.charAt(i) != ':') {
                i++;
            }
            i++;
            while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
                i++;
            }

            String value;
            if (i >= json.length()) {
                break;
            }
            char c = json.charAt(i);
            if (c == '"') {
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
                int start = i;
                while (i < json.length() && json.charAt(i) != ',' && json.charAt(i) != '}') {
                    i++;
                }
                value = json.substring(start, i).trim();
            }

            map.put(key, value);

            while (i < json.length() && (json.charAt(i) == ',' || Character.isWhitespace(json.charAt(i)))) {
                i++;
            }
        }
        return map;
    }

    public static String ok(String msg) {
        return "{\"ok\":true,\"msg\":\"" + esc(msg) + "\"}";
    }

    public static String err(String msg) {
        return "{\"ok\":false,\"erro\":\"" + esc(msg == null ? "Erro desconhecido" : msg) + "\"}";
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }
}
