package clinica.index;

import java.io.*;
import java.util.*;

public class OrdenacaoExterna {

    private static final int BLOCK_SIZE = 8;

    private final int                  tamanhoReg;
    private final Comparator<byte[]>   cmp;

    public OrdenacaoExterna(int tamanhoReg, Comparator<byte[]> comparator) {
        this.tamanhoReg = tamanhoReg;
        this.cmp        = comparator;
    }

    public void ordenar(String origem, String destino) throws IOException {
        List<File> runs = gerarRuns(origem);

        if (runs.isEmpty()) {
            copiarCabecalho(origem, destino);
            return;
        }

        while (runs.size() > 1) {
            runs = intercalarRound(runs);
        }

        montarSaida(origem, runs.get(0), destino);

        runs.get(0).delete();

        System.out.println("[OrdenacaoExterna] Concluído → " + destino);
    }

    private List<File> gerarRuns(String origem) throws IOException {
        List<File> runs = new ArrayList<>();

        try (RandomAccessFile raf = new RandomAccessFile(origem, "r")) {
            long tamanhoArq = raf.length();
            if (tamanhoArq <= 12) return runs;
            raf.seek(12);

            int runIdx = 0;
            while (raf.getFilePointer() < tamanhoArq) {
                List<byte[]> bloco = new ArrayList<>();
                for (int i = 0; i < BLOCK_SIZE && raf.getFilePointer() < tamanhoArq; i++) {
                    byte[] reg = new byte[tamanhoReg];
                    int lidos = raf.read(reg);
                    if (lidos == tamanhoReg) bloco.add(reg);
                }
                if (bloco.isEmpty()) break;

                bloco.sort(cmp);

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

    private List<File> intercalarRound(List<File> runs) throws IOException {
        List<File> novosRuns = new ArrayList<>();

        for (int i = 0; i < runs.size(); i += 2) {
            if (i + 1 >= runs.size()) {
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

    private File intercalarDois(File runA, File runB) throws IOException {
        File saida = File.createTempFile("merged_", ".tmp");
        saida.deleteOnExit();

        try (RandomAccessFile rafA   = new RandomAccessFile(runA, "r");
             RandomAccessFile rafB   = new RandomAccessFile(runB, "r");
             FileOutputStream fos    = new FileOutputStream(saida)) {

            PriorityQueue<RunEntry> heap = new PriorityQueue<>(
                (x, y) -> cmp.compare(x.reg, y.reg)
            );

            byte[] regA = lerProximo(rafA);
            byte[] regB = lerProximo(rafB);
            if (regA != null) heap.offer(new RunEntry(regA, rafA));
            if (regB != null) heap.offer(new RunEntry(regB, rafB));

            while (!heap.isEmpty()) {
                RunEntry menor = heap.poll();
                fos.write(menor.reg);

                byte[] prox = lerProximo(menor.fonte);
                if (prox != null) heap.offer(new RunEntry(prox, menor.fonte));
            }
        }

        return saida;
    }

    private byte[] lerProximo(RandomAccessFile raf) throws IOException {
        if (raf.getFilePointer() >= raf.length()) return null;
        byte[] buf = new byte[tamanhoReg];
        int lidos = raf.read(buf);
        return (lidos == tamanhoReg) ? buf : null;
    }

    private void copiarCabecalho(String origem, String destino) throws IOException {
        try (RandomAccessFile in  = new RandomAccessFile(origem, "r");
             FileOutputStream out = new FileOutputStream(destino)) {
            byte[] header = new byte[12];
            in.readFully(header);
            out.write(header);
        }
    }

    private void montarSaida(String origem, File runFinal, String destino) throws IOException {
        try (RandomAccessFile src  = new RandomAccessFile(origem, "r");
             FileInputStream  run  = new FileInputStream(runFinal);
             FileOutputStream out  = new FileOutputStream(destino)) {

            byte[] header = new byte[12];
            src.readFully(header);
            out.write(header);

            byte[] buf = new byte[4096];
            int lidos;
            while ((lidos = run.read(buf)) != -1) out.write(buf, 0, lidos);
        }
    }

    private static class RunEntry {
        final byte[]          reg;
        final RandomAccessFile fonte;
        RunEntry(byte[] reg, RandomAccessFile fonte) { this.reg = reg; this.fonte = fonte; }
    }

    public static Comparator<byte[]> porCampoString(int offset, int tamanho) {
        return (a, b) -> {
            String sa = extrairString(a, offset, tamanho);
            String sb = extrairString(b, offset, tamanho);
            return sa.compareToIgnoreCase(sb);
        };
    }

    public static Comparator<byte[]> porCampoInt(int offset) {
        return (a, b) -> {
            int ia = toInt(a, offset);
            int ib = toInt(b, offset);
            return Integer.compare(ia, ib);
        };
    }

    public static Comparator<byte[]> porCampoDouble(int offset) {
        return (a, b) -> {
            double da = toDouble(a, offset);
            double db = toDouble(b, offset);
            return Double.compare(da, db);
        };
    }

    public static Comparator<byte[]> decrescente(Comparator<byte[]> cmp) {
        return cmp.reversed();
    }

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
