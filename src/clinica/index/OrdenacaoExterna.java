package clinica.index;

import java.io.*;
import java.util.*;

/**
 * Ordenação Externa por Intercalação (External Merge Sort).
 *
 * Funciona sobre qualquer arquivo binário de registros de tamanho fixo.
 * O campo de ordenação é extraído via Comparator<byte[]> fornecido pelo chamador.
 *
 * Algoritmo em 2 fases:
 *   Fase 1 — Geração de Runs:
 *     Lê blocos de BLOCK_SIZE registros na memória, ordena-os internamente
 *     (selection sort para fins didáticos) e grava cada bloco em um arquivo
 *     temporário ("run").
 *
 *   Fase 2 — Intercalação k-way:
 *     Intercala todos os runs usando uma fila de prioridade (heap mínimo),
 *     lendo um registro de cada run por vez, gravando o menor no arquivo
 *     de saída. Repete rounds de intercalação até restar apenas 1 run.
 *
 * Uso típico:
 *   OrdenacaoExterna oe = new OrdenacaoExterna(tamanhoReg, comparator);
 *   oe.ordenar("data/pacientes.dat", "data/pacientes_sorted.dat");
 */
public class OrdenacaoExterna {

    /** Quantos registros cabem em memória por vez (ajustável) */
    private static final int BLOCK_SIZE = 8;

    private final int                  tamanhoReg;
    private final Comparator<byte[]>   cmp;

    public OrdenacaoExterna(int tamanhoReg, Comparator<byte[]> comparator) {
        this.tamanhoReg = tamanhoReg;
        this.cmp        = comparator;
    }

    // ─── API pública ─────────────────────────────────────────────────────────

    /**
     * Ordena o arquivo de origem e grava o resultado em destino.
     * O arquivo de origem NÃO é modificado.
     *
     * @param origem  caminho do arquivo .dat de entrada
     * @param destino caminho do arquivo .dat de saída ordenado
     */
    public void ordenar(String origem, String destino) throws IOException {
        // ── Fase 1: gera runs ────────────────────────────────────────────────
        List<File> runs = gerarRuns(origem);

        if (runs.isEmpty()) {
            // Arquivo vazio ou sem registros — copia cabeçalho apenas
            copiarCabecalho(origem, destino);
            return;
        }

        // ── Fase 2: intercalação iterativa até sobrar 1 run ──────────────────
        while (runs.size() > 1) {
            runs = intercalarRound(runs);
        }

        // O único run restante é o arquivo final ordenado; copia com cabeçalho
        montarSaida(origem, runs.get(0), destino);

        // Limpa run temporário
        runs.get(0).delete();

        System.out.println("[OrdenacaoExterna] Concluído → " + destino);
    }

    // ─── Fase 1: Geração de Runs ─────────────────────────────────────────────

    private List<File> gerarRuns(String origem) throws IOException {
        List<File> runs = new ArrayList<>();

        try (RandomAccessFile raf = new RandomAccessFile(origem, "r")) {
            // Pula o cabeçalho (12 bytes fixos do BaseDAO)
            long tamanhoArq = raf.length();
            if (tamanhoArq <= 12) return runs;
            raf.seek(12);

            int runIdx = 0;
            while (raf.getFilePointer() < tamanhoArq) {
                // Lê até BLOCK_SIZE registros
                List<byte[]> bloco = new ArrayList<>();
                for (int i = 0; i < BLOCK_SIZE && raf.getFilePointer() < tamanhoArq; i++) {
                    byte[] reg = new byte[tamanhoReg];
                    int lidos = raf.read(reg);
                    if (lidos == tamanhoReg) bloco.add(reg);
                }
                if (bloco.isEmpty()) break;

                // Ordena o bloco em memória (insertion sort — didático)
                bloco.sort(cmp);

                // Grava o run em arquivo temporário
                File tmpFile = File.createTempFile("run_" + runIdx + "_", ".tmp");
                tmpFile.deleteOnExit();
                try (FileOutputStream fos = new FileOutputStream(tmpFile)) {
                    for (byte[] reg : bloco) fos.write(reg);
                }
                runs.add(tmpFile);
                runIdx++;
            }
        }

        System.out.println("[OrdenacaoExterna] Fase 1: " + runs.size() + " run(s) gerado(s)");
        return runs;
    }

    // ─── Fase 2: Intercalação k-way (um round) ───────────────────────────────

    /**
     * Intercala todos os runs em pares (ou grupos), gerando uma nova lista
     * de runs menores. Cada chamada reduz o número de runs pela metade.
     */
    private List<File> intercalarRound(List<File> runs) throws IOException {
        List<File> novosRuns = new ArrayList<>();

        // Intercala de 2 em 2 (pode ser k-way maior; 2-way é suficiente e didático)
        for (int i = 0; i < runs.size(); i += 2) {
            if (i + 1 >= runs.size()) {
                // Último run sem par — passa adiante direto
                novosRuns.add(runs.get(i));
            } else {
                File merged = intercalarDois(runs.get(i), runs.get(i + 1));
                runs.get(i).delete();
                runs.get(i + 1).delete();
                novosRuns.add(merged);
            }
        }

        System.out.println("[OrdenacaoExterna] Intercalação → " + novosRuns.size() + " run(s)");
        return novosRuns;
    }

    /**
     * Intercala dois runs ordenados em um único run ordenado (2-way merge).
     * Usa heap mínimo para generalizar facilmente para k-way.
     */
    private File intercalarDois(File runA, File runB) throws IOException {
        File saida = File.createTempFile("merged_", ".tmp");
        saida.deleteOnExit();

        try (RandomAccessFile rafA   = new RandomAccessFile(runA, "r");
             RandomAccessFile rafB   = new RandomAccessFile(runB, "r");
             FileOutputStream fos    = new FileOutputStream(saida)) {

            // Heap mínimo: cada entrada é [registro, origem (0=A, 1=B)]
            PriorityQueue<RunEntry> heap = new PriorityQueue<>(
                (x, y) -> cmp.compare(x.reg, y.reg)
            );

            // Abastece o heap com o primeiro registro de cada run
            byte[] regA = lerProximo(rafA);
            byte[] regB = lerProximo(rafB);
            if (regA != null) heap.offer(new RunEntry(regA, rafA));
            if (regB != null) heap.offer(new RunEntry(regB, rafB));

            // Intercalação
            while (!heap.isEmpty()) {
                RunEntry menor = heap.poll();
                fos.write(menor.reg);

                byte[] prox = lerProximo(menor.fonte);
                if (prox != null) heap.offer(new RunEntry(prox, menor.fonte));
            }
        }

        return saida;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /** Lê o próximo registro de tamanhoReg bytes de um RandomAccessFile */
    private byte[] lerProximo(RandomAccessFile raf) throws IOException {
        if (raf.getFilePointer() >= raf.length()) return null;
        byte[] buf = new byte[tamanhoReg];
        int lidos = raf.read(buf);
        return (lidos == tamanhoReg) ? buf : null;
    }

    /** Copia apenas o cabeçalho de origem para destino (caso sem registros) */
    private void copiarCabecalho(String origem, String destino) throws IOException {
        try (RandomAccessFile in  = new RandomAccessFile(origem, "r");
             FileOutputStream out = new FileOutputStream(destino)) {
            byte[] header = new byte[12];
            in.readFully(header);
            out.write(header);
        }
    }

    /**
     * Monta o arquivo de saída final:
     *   - Cabeçalho original do arquivo de origem
     *   - Registros do run ordenado
     */
    private void montarSaida(String origem, File runFinal, String destino) throws IOException {
        try (RandomAccessFile src  = new RandomAccessFile(origem, "r");
             FileInputStream  run  = new FileInputStream(runFinal);
             FileOutputStream out  = new FileOutputStream(destino)) {

            // Copia cabeçalho
            byte[] header = new byte[12];
            src.readFully(header);
            out.write(header);

            // Copia registros ordenados
            byte[] buf = new byte[4096];
            int lidos;
            while ((lidos = run.read(buf)) != -1) out.write(buf, 0, lidos);
        }
    }

    // ─── Inner class ─────────────────────────────────────────────────────────

    private static class RunEntry {
        final byte[]          reg;
        final RandomAccessFile fonte;
        RunEntry(byte[] reg, RandomAccessFile fonte) { this.reg = reg; this.fonte = fonte; }
    }

    // ─── Comparadores prontos para as entidades ───────────────────────────────

    /**
     * Comparador por campo String de tamanho fixo dentro de um registro.
     * @param offset  byte de início do campo no registro
     * @param tamanho tamanho máximo do campo em bytes
     */
    public static Comparator<byte[]> porCampoString(int offset, int tamanho) {
        return (a, b) -> {
            String sa = extrairString(a, offset, tamanho);
            String sb = extrairString(b, offset, tamanho);
            return sa.compareToIgnoreCase(sb);
        };
    }

    /**
     * Comparador por campo int (4 bytes) dentro de um registro.
     * @param offset byte de início do campo int no registro
     */
    public static Comparator<byte[]> porCampoInt(int offset) {
        return (a, b) -> {
            int ia = toInt(a, offset);
            int ib = toInt(b, offset);
            return Integer.compare(ia, ib);
        };
    }

    /**
     * Comparador por campo double (8 bytes) dentro de um registro.
     * @param offset byte de início do campo double no registro
     */
    public static Comparator<byte[]> porCampoDouble(int offset) {
        return (a, b) -> {
            double da = toDouble(a, offset);
            double db = toDouble(b, offset);
            return Double.compare(da, db);
        };
    }

    // Inverte qualquer comparador para ordem decrescente
    public static Comparator<byte[]> decrescente(Comparator<byte[]> cmp) {
        return cmp.reversed();
    }

    // ─── Utilitários de bytes ─────────────────────────────────────────────────

    private static String extrairString(byte[] buf, int offset, int tamanho) {
        int end = offset + tamanho;
        if (end > buf.length) end = buf.length;
        int len = end - offset;
        for (int i = offset; i < end; i++) {
            if (buf[i] == 0) { len = i - offset; break; }
        }
        return new String(buf, offset, len, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static int toInt(byte[] buf, int offset) {
        return ((buf[offset]     & 0xFF) << 24)
             | ((buf[offset + 1] & 0xFF) << 16)
             | ((buf[offset + 2] & 0xFF) <<  8)
             |  (buf[offset + 3] & 0xFF);
    }

    private static double toDouble(byte[] buf, int offset) {
        long bits = 0;
        for (int i = 0; i < 8; i++) bits = (bits << 8) | (buf[offset + i] & 0xFFL);
        return Double.longBitsToDouble(bits);
    }
}
