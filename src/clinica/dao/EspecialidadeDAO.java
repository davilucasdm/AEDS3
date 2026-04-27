package clinica.dao;

import clinica.index.HashExtensivel;
import clinica.model.Especialidade;

import java.io.*;

public class EspecialidadeDAO extends BaseDAO<Especialidade> {

    private static final int T_NOME = 100;
    private static final int T_DESC = 200;
    // total = 4+1+100+200 = 305

    private final HashExtensivel hashId;

    public EspecialidadeDAO(String dataDir) throws IOException {
        super(dataDir + "/especialidades.dat", 305);
        hashId = new HashExtensivel(dataDir + "/especialidades_hash");
    }

    @Override
    public int criar(Especialidade e) throws IOException {
        int id = super.criar(e);
        hashId.inserir(id, offsetReg(totalSlots() - 1));
        return id;
    }

    public Especialidade buscarPorIdHash(int id) throws IOException {
        long off = hashId.buscar(id);
        if (off < 0) return null;
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            raf.seek(off);
            byte[] buf = new byte[tamanhoReg];
            raf.readFully(buf);
            Especialidade e = desserializar(buf);
            return e.isAtivo() ? e : null;
        }
    }

    @Override
    public boolean deletar(int id) throws IOException {
        boolean ok = super.deletar(id);
        if (ok) hashId.remover(id);
        return ok;
    }

    @Override
    protected byte[] serializar(Especialidade e) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(tamanhoReg);
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(e.getId());
        dos.writeBoolean(e.isAtivo());
        writeStr(dos, e.getNome(),      T_NOME);
        writeStr(dos, e.getDescricao(), T_DESC);
        dos.flush();
        return baos.toByteArray();
    }

    @Override
    protected Especialidade desserializar(byte[] bytes) throws IOException {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes));
        int     id   = dis.readInt();
        boolean at   = dis.readBoolean();
        String nome  = readStr(dis, T_NOME);
        String desc  = readStr(dis, T_DESC);
        return new Especialidade(id, at, nome, desc);
    }

    @Override protected int     getId(Especialidade e)             { return e.getId(); }
    @Override protected void    atribuirId(Especialidade e, int i) { e.setId(i); }
    @Override protected boolean isAtivo(Especialidade e)           { return e.isAtivo(); }
    @Override protected void    setAtivo(Especialidade e, boolean a){ e.setAtivo(a); }
}
