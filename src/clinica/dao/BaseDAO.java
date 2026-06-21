package clinica.dao;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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
            dos.writeInt(0);
            dos.writeInt(0);
            dos.writeInt(tamanhoReg);
        }
    }

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

    protected long offsetReg(int pos) {
        return (long) HEADER_SIZE + (long) pos * tamanhoReg;
    }

    protected int totalSlots() throws IOException {
        long tamanhoArq = new File(filePath).length();
        return (int) ((tamanhoArq - HEADER_SIZE) / tamanhoReg);
    }

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

    protected abstract byte[] serializar(T obj) throws IOException;
    protected abstract T      desserializar(byte[] bytes) throws IOException;
    protected abstract int    getId(T obj);
    protected abstract void   atribuirId(T obj, int id);
    protected abstract boolean isAtivo(T obj);
    protected abstract void   setAtivo(T obj, boolean ativo);
}
