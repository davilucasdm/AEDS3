package clinica.dao;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe abstrata que encapsula toda a lógica de persistência em arquivo binário.
 *
 * Formato do arquivo:
 *   Header (12 bytes):
 *     [0-3]  int numRegistros  — registros ativos
 *     [4-7]  int ultimoId      — último ID gerado
 *     [8-11] int tamanhoReg    — tamanho de cada registro em bytes
 *   A partir do byte 12: registros de tamanho fixo (tamanhoReg bytes cada)
 */
public abstract class BaseDAO<T> {

    protected static final int HEADER_SIZE = 12;

    protected final String filePath;
    protected final int    tamanhoReg;

    protected BaseDAO(String filePath, int tamanhoReg) throws IOException {
        this.filePath   = filePath;
        this.tamanhoReg = tamanhoReg;
        File f = new File(filePath);
        if (!f.getParentFile().exists()) f.getParentFile().mkdirs();
        if (!f.exists()) criarArquivo();
    }

    private void criarArquivo() throws IOException {
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(filePath))) {
            dos.writeInt(0);            // numRegistros
            dos.writeInt(0);            // ultimoId
            dos.writeInt(tamanhoReg);   // tamanhoReg
        }
    }

    // ─── Cabeçalho ──────────────────────────────────────────────────────────
    protected int[] lerHeader() throws IOException {
        try (DataInputStream dis = new DataInputStream(new FileInputStream(filePath))) {
            return new int[]{ dis.readInt(), dis.readInt(), dis.readInt() };
        }
    }

    protected void gravarHeader(int numReg, int ultimoId) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            raf.writeInt(numReg);
            raf.writeInt(ultimoId);
            raf.writeInt(tamanhoReg);
        }
    }

    protected int proximoId() throws IOException {
        int[] h = lerHeader();
        int novoId = h[1] + 1;
        gravarHeader(h[0], novoId);
        return novoId;
    }

    // ─── Posição do registro no arquivo ────────────────────────────────────
    /** Offset do registro de índice zero-based 'pos' dentro do arquivo */
    protected long offsetReg(int pos) {
        return (long) HEADER_SIZE + (long) pos * tamanhoReg;
    }

    /** Quantidade total de slots (ativos + excluídos logicamente) */
    protected int totalSlots() throws IOException {
        long tamanhoArq = new File(filePath).length();
        return (int) ((tamanhoArq - HEADER_SIZE) / tamanhoReg);
    }

    // ─── Helpers para strings de tamanho fixo ──────────────────────────────
    protected static void writeStr(DataOutputStream dos, String s, int maxBytes) throws IOException {
        byte[] src = (s == null ? "" : s).getBytes(StandardCharsets.UTF_8);
        byte[] buf = new byte[maxBytes];
        System.arraycopy(src, 0, buf, 0, Math.min(src.length, maxBytes));
        dos.write(buf);
    }

    protected static String readStr(DataInputStream dis, int maxBytes) throws IOException {
        byte[] buf = new byte[maxBytes];
        dis.readFully(buf);
        int end = buf.length;
        for (int i = 0; i < buf.length; i++) { if (buf[i] == 0) { end = i; break; } }
        return new String(buf, 0, end, StandardCharsets.UTF_8);
    }

    // ─── CRUD genérico ──────────────────────────────────────────────────────

    /** Persiste um novo registro e retorna o ID gerado */
    public int criar(T obj) throws IOException {
        int id = proximoId();
        atribuirId(obj, id);
        setAtivo(obj, true);

        byte[] bytes = serializar(obj);
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            raf.seek(raf.length());
            raf.write(bytes);
        }

        int[] h = lerHeader();
        gravarHeader(h[0] + 1, h[1]);
        return id;
    }

    /** Busca registro por ID (exclusão lógica é ignorada — retorna null se inativo) */
    public T buscarPorId(int id) throws IOException {
        int slots = totalSlots();
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            for (int i = 0; i < slots; i++) {
                raf.seek(offsetReg(i));
                byte[] buf = new byte[tamanhoReg];
                raf.readFully(buf);
                T obj = desserializar(buf);
                if (getId(obj) == id && isAtivo(obj)) return obj;
            }
        }
        return null;
    }

    /** Lista todos os registros ativos */
    public List<T> listarTodos() throws IOException {
        List<T> lista = new ArrayList<>();
        int slots = totalSlots();
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            for (int i = 0; i < slots; i++) {
                raf.seek(offsetReg(i));
                byte[] buf = new byte[tamanhoReg];
                raf.readFully(buf);
                T obj = desserializar(buf);
                if (isAtivo(obj)) lista.add(obj);
            }
        }
        return lista;
    }

    /** Atualiza registro: encontra pelo ID e substitui */
    public boolean atualizar(T obj) throws IOException {
        int slots = totalSlots();
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            for (int i = 0; i < slots; i++) {
                raf.seek(offsetReg(i));
                byte[] buf = new byte[tamanhoReg];
                raf.readFully(buf);
                T existente = desserializar(buf);
                if (getId(existente) == getId(obj) && isAtivo(existente)) {
                    setAtivo(obj, true);
                    raf.seek(offsetReg(i));
                    raf.write(serializar(obj));
                    return true;
                }
            }
        }
        return false;
    }

    /** Exclusão lógica: marca ativo=false no arquivo */
    public boolean deletar(int id) throws IOException {
        int slots = totalSlots();
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            for (int i = 0; i < slots; i++) {
                raf.seek(offsetReg(i));
                byte[] buf = new byte[tamanhoReg];
                raf.readFully(buf);
                T obj = desserializar(buf);
                if (getId(obj) == id && isAtivo(obj)) {
                    setAtivo(obj, false);
                    raf.seek(offsetReg(i));
                    raf.write(serializar(obj));
                    int[] h = lerHeader();
                    gravarHeader(Math.max(0, h[0] - 1), h[1]);
                    return true;
                }
            }
        }
        return false;
    }

    // ─── Métodos abstratos que cada DAO implementa ──────────────────────────
    protected abstract byte[] serializar(T obj) throws IOException;
    protected abstract T      desserializar(byte[] bytes) throws IOException;
    protected abstract int    getId(T obj);
    protected abstract void   atribuirId(T obj, int id);
    protected abstract boolean isAtivo(T obj);
    protected abstract void   setAtivo(T obj, boolean ativo);
}
