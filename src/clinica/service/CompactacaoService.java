package clinica.service;

import clinica.util.Huffman;
import clinica.util.LZW;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Serviço de compactação dos arquivos de dados (Fase IV).
 *
 * Empacota TODOS os arquivos de dados do diretório {@code dataDir} em um único
 * arquivo compactado (Huffman ou LZW), funcionando como backup completo.
 *
 * São coletados os arquivos com as seguintes extensões, que correspondem a
 * TODOS os arquivos binários gerados pelo sistema (BaseDAO, HashExtensivel e
 * ArvoreBMais):
 *   .dat   → arquivos de dados (pacientes, medicos, consultas, etc.)
 *   .hdir  → diretório do Hash Extensível
 *   .hbkt  → buckets do Hash Extensível
 *   .btree → nós da Árvore B+
 *
 * Formato do arquivo compactado (.hbak):
 * ┌─────────────────────────────────────────────────────────┐
 * │ Header geral                                             │
 * │   [4 bytes] int  versão       = 1                        │
 * │   [4 bytes] int  algoritmo    (0=HUFFMAN, 1=LZW)          │
 * │   [4 bytes] int  nArquivos                                │
 * │ Para cada arquivo:                                        │
 * │   [4 bytes] int  tamanhoNome                              │
 * │   [n bytes] UTF  nomeArquivo (relativo a dataDir)         │
 * │   [4 bytes] int  tamanhoOriginal                          │
 * │   [4 bytes] int  tamanhoCompactado                        │
 * │   [k bytes] blob compactado (Huffman ou LZW)              │
 * └─────────────────────────────────────────────────────────┘
 */
public class CompactacaoService {

    public enum Algoritmo { HUFFMAN, LZW }

    private final String dataDir;

    public CompactacaoService(String dataDir) {
        this.dataDir = dataDir;
    }

    // ── Compactar ─────────────────────────────────────────────────────────────

    /**
     * Lê todos os arquivos .dat, .hdir, .hbkt, .btree de {@code dataDir}
     * e gera um único arquivo {@code nomeArquivo} compactado.
     *
     * @return resumo com taxas de compressão de cada arquivo
     */
    public CompactacaoResult compactar(String nomeArquivo, Algoritmo alg) throws IOException {
        List<Path> arquivos = coletarArquivos();
        if (arquivos.isEmpty()) throw new IOException("Nenhum arquivo de dados encontrado em: " + dataDir);

        CompactacaoResult resultado = new CompactacaoResult(alg.name());

        ByteArrayOutputStream corpo = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(corpo);

        dos.writeInt(1);                              // versão
        dos.writeInt(alg == Algoritmo.HUFFMAN ? 0 : 1); // algoritmo
        dos.writeInt(arquivos.size());                 // n arquivos

        for (Path p : arquivos) {
            byte[] original = Files.readAllBytes(p);
            byte[] compactado = (alg == Algoritmo.HUFFMAN)
                    ? Huffman.compress(original)
                    : LZW.compress(original);

            String nome = p.getFileName().toString();
            byte[] nomeBytes = nome.getBytes(java.nio.charset.StandardCharsets.UTF_8);

            dos.writeInt(nomeBytes.length);
            dos.write(nomeBytes);
            dos.writeInt(original.length);
            dos.writeInt(compactado.length);
            dos.write(compactado);

            resultado.adicionarArquivo(nome, original.length, compactado.length);
        }
        dos.flush();

        Files.write(Paths.get(nomeArquivo), corpo.toByteArray());
        resultado.arquivoGerado = nomeArquivo;
        return resultado;
    }

    // ── Descompactar ──────────────────────────────────────────────────────────

    /**
     * Lê o arquivo compactado e restaura todos os arquivos de dados no {@code dataDir}.
     *
     * @return lista dos arquivos restaurados
     */
    public List<String> descompactar(String nomeArquivo) throws IOException {
        byte[] raw = Files.readAllBytes(Paths.get(nomeArquivo));
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(raw));

        int versao = dis.readInt();
        if (versao != 1) throw new IOException("Versão de backup desconhecida: " + versao);

        int algCod = dis.readInt();
        Algoritmo alg = (algCod == 0) ? Algoritmo.HUFFMAN : Algoritmo.LZW;

        int nArquivos = dis.readInt();
        List<String> restaurados = new ArrayList<>();

        for (int i = 0; i < nArquivos; i++) {
            int tamanhoNome = dis.readInt();
            byte[] nomeBytes = new byte[tamanhoNome];
            dis.readFully(nomeBytes);
            String nome = new String(nomeBytes, java.nio.charset.StandardCharsets.UTF_8);

            int tamanhoOriginal = dis.readInt();
            int tamanhoComp     = dis.readInt();
            byte[] compactado = new byte[tamanhoComp];
            dis.readFully(compactado);

            byte[] original = (alg == Algoritmo.HUFFMAN)
                    ? Huffman.decompress(compactado)
                    : LZW.decompress(compactado);

            Path dest = Paths.get(dataDir, nome);
            if (dest.getParent() != null) Files.createDirectories(dest.getParent());
            Files.write(dest, original);
            restaurados.add(nome);
        }
        return restaurados;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Coleta todos os arquivos de dados do sistema:
     *   pacientes.dat, medicos.dat, consultas.dat, especialidades.dat,
     *   medico_especialidade.dat, usuarios.dat
     *   *_hash.hdir / *_hash.hbkt (Hash Extensível)
     *   *_hash_med.hdir / *_hash_med.hbkt / *_hash_esp.hdir / *_hash_esp.hbkt
     *   *_btree.btree / *_btree_data.btree (Árvore B+)
     */
    private List<Path> coletarArquivos() throws IOException {
        Path dir = Paths.get(dataDir);
        if (!Files.isDirectory(dir)) return List.of();

        List<Path> resultado = new ArrayList<>();
        try (var stream = Files.walk(dir, 1)) {
            stream
                .filter(p -> !Files.isDirectory(p))
                .filter(p -> {
                    String n = p.getFileName().toString();
                    return n.endsWith(".dat") || n.endsWith(".btree")
                        || n.endsWith(".hdir") || n.endsWith(".hbkt");
                })
                .sorted()
                .forEach(resultado::add);
        }
        return resultado;
    }

    // ── DTO de Resultado ──────────────────────────────────────────────────────

    public static class CompactacaoResult {
        public final String algoritmo;
        public String arquivoGerado;
        public final List<ArquivoInfo> arquivos = new ArrayList<>();
        public long totalOriginal = 0;
        public long totalCompactado = 0;

        public CompactacaoResult(String algoritmo) {
            this.algoritmo = algoritmo;
        }

        void adicionarArquivo(String nome, long orig, long comp) {
            arquivos.add(new ArquivoInfo(nome, orig, comp));
            totalOriginal   += orig;
            totalCompactado += comp;
        }

        public double taxaGeral() {
            if (totalOriginal == 0) return 0;
            return 1.0 - (double) totalCompactado / totalOriginal;
        }

        public String toJson() {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"algoritmo\":\"").append(algoritmo).append("\"");
            sb.append(",\"arquivoGerado\":\"").append(esc(arquivoGerado)).append("\"");
            sb.append(",\"totalOriginalBytes\":").append(totalOriginal);
            sb.append(",\"totalCompactadoBytes\":").append(totalCompactado);
            sb.append(",\"taxaGeral\":").append(String.format(Locale.US, "%.4f", taxaGeral()));
            sb.append(",\"arquivos\":[");
            for (int i = 0; i < arquivos.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(arquivos.get(i).toJson());
            }
            sb.append("]}");
            return sb.toString();
        }

        private static String esc(String s) {
            return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
        }
    }

    public static class ArquivoInfo {
        public final String nome;
        public final long originalBytes;
        public final long compactadoBytes;

        ArquivoInfo(String nome, long orig, long comp) {
            this.nome = nome;
            this.originalBytes = orig;
            this.compactadoBytes = comp;
        }

        public double taxa() {
            if (originalBytes == 0) return 0;
            return 1.0 - (double) compactadoBytes / originalBytes;
        }

        public String toJson() {
            return String.format(Locale.US,
                "{\"nome\":\"%s\",\"originalBytes\":%d,\"compactadoBytes\":%d,\"taxa\":%.4f}",
                nome.replace("\\", "\\\\").replace("\"", "\\\""), originalBytes, compactadoBytes, taxa()
            );
        }
    }
}
