package clinica.util;

import java.util.LinkedHashMap;
import java.util.Map;

/** Parser JSON plano (sem aninhamento) para uso nas rotas HTTP */
public class JsonParser {

    public static Map<String, String> parse(String json) {
        Map<String, String> map = new LinkedHashMap<>();
        if (json == null) return map;
        json = json.trim();
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}")) json = json.substring(0, json.length() - 1);

        // state machine simples
        int i = 0, len = json.length();
        while (i < len) {
            // pula espaços
            while (i < len && Character.isWhitespace(json.charAt(i))) i++;
            if (i >= len) break;

            // lê chave
            if (json.charAt(i) != '"') { i++; continue; }
            i++;
            StringBuilder key = new StringBuilder();
            while (i < len && json.charAt(i) != '"') {
                if (json.charAt(i) == '\\') i++;
                key.append(json.charAt(i++));
            }
            i++; // fecha aspas da chave

            // pula ':'
            while (i < len && json.charAt(i) != ':') i++;
            i++;
            while (i < len && Character.isWhitespace(json.charAt(i))) i++;

            // lê valor
            StringBuilder val = new StringBuilder();
            if (i < len && json.charAt(i) == '"') {
                i++;
                while (i < len && json.charAt(i) != '"') {
                    if (json.charAt(i) == '\\') { i++; }
                    if (i < len) val.append(json.charAt(i++));
                }
                i++; // fecha aspas
            } else {
                // número, boolean ou null
                while (i < len && json.charAt(i) != ',' && json.charAt(i) != '}') {
                    val.append(json.charAt(i++));
                }
            }
            map.put(key.toString().trim(), val.toString().trim());

            // pula vírgula
            while (i < len && (json.charAt(i) == ',' || Character.isWhitespace(json.charAt(i)))) i++;
        }
        return map;
    }

    /** Monta array JSON a partir de vários toJson() */
    public static String array(java.util.List<String> items) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(items.get(i));
        }
        return sb.append("]").toString();
    }

    public static String ok(String msg) {
        return "{\"ok\":true,\"msg\":\"" + msg + "\"}";
    }

    public static String err(String msg) {
        return "{\"ok\":false,\"msg\":\"" + msg + "\"}";
    }
}
