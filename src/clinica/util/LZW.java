package clinica.util;

import java.io.*;
import java.util.*;

/**
 * Compactação e descompactação LZW (Lempel–Ziv–Welch).
 *
 * Tabela de dicionário de 12 bits (4096 entradas).
 * Códigos reservados:
 *   0–255 : símbolos de 1 byte
 *   256   : CLEAR — reinicia o dicionário
 *   257   : STOP  — fim do stream
 *
 * Formato do bloco compactado:
 *   [4 bytes] int  nCodigos — total de códigos de 12 bits emitidos
 *   [restante] códigos de 12 bits empacotados em bytes
 */
public class LZW {

    private static final int BITS       = 12;
    private static final int MAX_TABLE  = 1 << BITS;   // 4096
    private static final int CLEAR_CODE = 256;
    private static final int STOP_CODE  = 257;
    private static final int FIRST_CODE = 258;

    // ── Compactar ────────────────────────────────────────────────────────────
    /**
     * Compacta um array de bytes usando LZW de 12 bits.
     */
    public static byte[] compress(byte[] dados) throws IOException {
        if (dados == null || dados.length == 0) {
            return new byte[]{0, 0, 0, 0};
        }

        // Dicionário: string → código
        Map<String, Integer> dict = new HashMap<>();
        for (int i = 0; i < 256; i++) {
            dict.put(String.valueOf((char) i), i);
        }
        int nextCode = FIRST_CODE;

        List<Integer> codigos = new ArrayList<>();
        codigos.add(CLEAR_CODE);

        String w = "";
        for (byte b : dados) {
            String wc = w + (char)(b & 0xFF);
            if (dict.containsKey(wc)) {
                w = wc;
            } else {
                codigos.add(dict.get(w));
                if (nextCode < MAX_TABLE) {
                    dict.put(wc, nextCode++);
                } else {
                    // Dicionário cheio: emite CLEAR e reinicia
                    codigos.add(CLEAR_CODE);
                    dict.clear();
                    for (int i = 0; i < 256; i++) {
                        dict.put(String.valueOf((char) i), i);
                    }
                    nextCode = FIRST_CODE;
                }
                w = String.valueOf((char)(b & 0xFF));
            }
        }
        if (!w.isEmpty()) codigos.add(dict.get(w));
        codigos.add(STOP_CODE);

        // Empacotar em 12 bits por código
        return empacotar(codigos);
    }

    // ── Descompactar ─────────────────────────────────────────────────────────
    /**
     * Reconstrói o array original a partir de dados LZW.
     */
    public static byte[] decompress(byte[] comp) throws IOException {
        if (comp == null || comp.length < 4) return new byte[0];

        List<Integer> codigos = desempacotar(comp);

        // Dicionário: código → string
        Map<Integer, String> dict = new HashMap<>();
        for (int i = 0; i < 256; i++) {
            dict.put(i, String.valueOf((char) i));
        }
        int nextCode = FIRST_CODE;

        ByteArrayOutputStream resultado = new ByteArrayOutputStream();
        int i = 0;

        // Pular CLEAR iniciais
        while (i < codigos.size() && codigos.get(i) == CLEAR_CODE) i++;
        if (i >= codigos.size()) return new byte[0];

        int codigo = codigos.get(i++);
        if (codigo == STOP_CODE) return new byte[0];
        String entrada = dict.get(codigo);
        writeStr(resultado, entrada);
        String w = entrada;

        for (; i < codigos.size(); i++) {
            codigo = codigos.get(i);
            if (codigo == STOP_CODE) break;
            if (codigo == CLEAR_CODE) {
                dict.clear();
                for (int j = 0; j < 256; j++) dict.put(j, String.valueOf((char) j));
                nextCode = FIRST_CODE;
                i++;
                if (i >= codigos.size()) break;
                codigo = codigos.get(i);
                if (codigo == STOP_CODE) break;
                entrada = dict.get(codigo);
                writeStr(resultado, entrada);
                w = entrada;
                continue;
            }
            if (dict.containsKey(codigo)) {
                entrada = dict.get(codigo);
            } else {
                entrada = w + w.charAt(0);
            }
            writeStr(resultado, entrada);
            if (nextCode < MAX_TABLE) {
                dict.put(nextCode++, w + entrada.charAt(0));
            }
            w = entrada;
        }
        return resultado.toByteArray();
    }

    // ── Empacotamento de 12 bits ─────────────────────────────────────────────
    private static byte[] empacotar(List<Integer> codigos) throws IOException {
        int bitsTotal = codigos.size() * BITS;
        byte[] buf = new byte[(bitsTotal + 7) / 8];

        int bitPos = 0;
        for (int cod : codigos) {
            for (int b = BITS - 1; b >= 0; b--) {
                if (((cod >> b) & 1) == 1) {
                    buf[bitPos / 8] |= (byte)(1 << (7 - (bitPos % 8)));
                }
                bitPos++;
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(codigos.size());  // número de códigos (não de bits)
        dos.write(buf);
        dos.flush();
        return baos.toByteArray();
    }

    private static List<Integer> desempacotar(byte[] comp) throws IOException {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(comp));
        int nCodigos = dis.readInt();
        byte[] buf = dis.readAllBytes();

        List<Integer> codigos = new ArrayList<>(nCodigos);
        int bitPos = 0;
        for (int c = 0; c < nCodigos; c++) {
            int cod = 0;
            for (int b = BITS - 1; b >= 0; b--) {
                int bit = (buf[bitPos / 8] >> (7 - (bitPos % 8))) & 1;
                cod |= (bit << b);
                bitPos++;
            }
            codigos.add(cod);
        }
        return codigos;
    }

    private static void writeStr(OutputStream out, String s) throws IOException {
        for (char c : s.toCharArray()) out.write((byte) c);
    }
}
