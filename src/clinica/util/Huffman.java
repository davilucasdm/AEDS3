package clinica.util;

import java.io.*;
import java.util.*;

/**
 * Compactação e descompactação Huffman.
 *
 * Funciona em nível de arquivo (backup completo):
 *   compress(byte[] dados)  → byte[] compactado (cabeçalho + bits)
 *   decompress(byte[] comp) → byte[] original
 *
 * Formato do bloco compactado:
 *   [4 bytes] int  nSymbols  — quantos símbolos na tabela
 *   Para cada símbolo:
 *     [1 byte]  byte  symbol
 *     [4 bytes] int   freq
 *   [4 bytes] int  bitsTotal — total de bits válidos nos dados
 *   [restante] bits Huffman empacotados em bytes (último byte pode ter padding)
 */
public class Huffman {

    // ── Nó da árvore ────────────────────────────────────────────────────────
    private static class No implements Comparable<No> {
        int symbol;   // -1 = interno
        int freq;
        No esq, dir;

        No(int symbol, int freq) {
            this.symbol = symbol;
            this.freq = freq;
        }

        No(No esq, No dir) {
            this.symbol = -1;
            this.freq = esq.freq + dir.freq;
            this.esq = esq;
            this.dir = dir;
        }

        boolean isFolha() { return esq == null; }

        @Override
        public int compareTo(No o) { return Integer.compare(this.freq, o.freq); }
    }

    // ── Compactar ────────────────────────────────────────────────────────────
    /**
     * Compacta um array de bytes usando Huffman.
     * Caso especial: dados vazios retornam array de 4 bytes (int 0).
     */
    public static byte[] compress(byte[] dados) throws IOException {
        if (dados == null || dados.length == 0) {
            return new byte[]{0, 0, 0, 0};
        }

        // 1. Frequências
        int[] freq = new int[256];
        for (byte b : dados) freq[b & 0xFF]++;

        // 2. Construir árvore
        PriorityQueue<No> heap = new PriorityQueue<>();
        for (int i = 0; i < 256; i++) {
            if (freq[i] > 0) heap.add(new No(i, freq[i]));
        }
        // Caso especial: somente 1 símbolo distinto
        if (heap.size() == 1) {
            No unico = heap.poll();
            heap.add(new No(unico, new No(unico.symbol, 0)));
        }
        while (heap.size() > 1) {
            No a = heap.poll(), b = heap.poll();
            heap.add(new No(a, b));
        }
        No raiz = heap.poll();

        // 3. Gerar códigos
        Map<Integer, String> codigos = new HashMap<>();
        gerarCodigos(raiz, "", codigos);

        // 4. Codificar
        StringBuilder bits = new StringBuilder();
        for (byte b : dados) bits.append(codigos.get(b & 0xFF));

        // 5. Empacotar bits em bytes
        int bitsTotal = bits.length();
        byte[] bitBytes = new byte[(bitsTotal + 7) / 8];
        for (int i = 0; i < bitsTotal; i++) {
            if (bits.charAt(i) == '1') {
                bitBytes[i / 8] |= (byte)(1 << (7 - (i % 8)));
            }
        }

        // 6. Montar stream: tabela de frequências + bitsTotal + bitBytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // Tabela de símbolos
        int nSymbols = 0;
        for (int f : freq) if (f > 0) nSymbols++;
        dos.writeInt(nSymbols);
        for (int i = 0; i < 256; i++) {
            if (freq[i] > 0) {
                dos.writeByte(i);
                dos.writeInt(freq[i]);
            }
        }
        dos.writeInt(bitsTotal);
        dos.write(bitBytes);
        dos.flush();
        return baos.toByteArray();
    }

    // ── Descompactar ─────────────────────────────────────────────────────────
    /**
     * Reconstrói o array original a partir de dados Huffman.
     */
    public static byte[] decompress(byte[] comp) throws IOException {
        if (comp == null || comp.length < 4) return new byte[0];

        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(comp));

        // Ler tabela de frequências
        int nSymbols = dis.readInt();
        if (nSymbols == 0) return new byte[0];

        int[] freq = new int[256];
        for (int i = 0; i < nSymbols; i++) {
            int sym = dis.readUnsignedByte();
            freq[sym] = dis.readInt();
        }

        // Reconstruir árvore
        PriorityQueue<No> heap = new PriorityQueue<>();
        for (int i = 0; i < 256; i++) {
            if (freq[i] > 0) heap.add(new No(i, freq[i]));
        }
        if (heap.size() == 1) {
            No unico = heap.poll();
            heap.add(new No(unico, new No(unico.symbol, 0)));
        }
        while (heap.size() > 1) {
            No a = heap.poll(), b = heap.poll();
            heap.add(new No(a, b));
        }
        No raiz = heap.poll();

        // Ler bits
        int bitsTotal = dis.readInt();
        byte[] bitBytes = dis.readAllBytes();

        // Decodificar
        ByteArrayOutputStream resultado = new ByteArrayOutputStream();
        No atual = raiz;
        int bitsLidos = 0;
        outer:
        for (byte bb : bitBytes) {
            for (int bit = 7; bit >= 0; bit--) {
                if (bitsLidos >= bitsTotal) break outer;
                int b = (bb >> bit) & 1;
                atual = (b == 0) ? atual.esq : atual.dir;
                if (atual.isFolha()) {
                    resultado.write(atual.symbol);
                    atual = raiz;
                }
                bitsLidos++;
            }
        }
        return resultado.toByteArray();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private static void gerarCodigos(No no, String prefixo, Map<Integer, String> codigos) {
        if (no == null) return;
        if (no.isFolha()) {
            codigos.put(no.symbol, prefixo.isEmpty() ? "0" : prefixo);
        } else {
            gerarCodigos(no.esq, prefixo + "0", codigos);
            gerarCodigos(no.dir, prefixo + "1", codigos);
        }
    }
}
