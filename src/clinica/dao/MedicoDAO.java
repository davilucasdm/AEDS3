package clinica.dao;

import clinica.index.ArvoreBMais;
import clinica.index.HashExtensivel;
import clinica.model.Medico;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class MedicoDAO extends BaseDAO<Medico> {

    private static final int T_NOME  = 100;
    private static final int T_CRM   = 20;
    private static final int T_EMAIL = 100;
    private static final int T_TELS  = 150;
    // total = 4+1+100+20+100+150 = 375

    private final HashExtensivel hashId;
    private final ArvoreBMais    bPlus;

    public MedicoDAO(String dataDir) throws IOException {
        super(dataDir + "/medicos.dat", 375);
        hashId = new HashExtensivel(dataDir + "/medicos_hash");
        bPlus  = new ArvoreBMais(dataDir + "/medicos_btree");
    }

    @Override
    public int criar(Medico m) throws IOException {
        int id = super.criar(m);
        long off = offsetReg(totalSlots() - 1);
        hashId.inserir(id, off);
        bPlus.inserir(id, off);
        return id;
    }

    public Medico buscarPorIdHash(int id) throws IOException {
        long off = hashId.buscar(id);
        if (off < 0) return null;
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            raf.seek(off);
            byte[] buf = new byte[tamanhoReg];
            raf.readFully(buf);
            Medico m = desserializar(buf);
            return m.isAtivo() ? m : null;
        }
    }

    public List<Medico> listarOrdenados() throws IOException {
        List<Medico> lista = new ArrayList<>();
        for (long[] par : bPlus.listarOrdenado()) {
            try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
                raf.seek(par[1]);
                byte[] buf = new byte[tamanhoReg];
                raf.readFully(buf);
                Medico m = desserializar(buf);
                if (m.isAtivo()) lista.add(m);
            }
        }
        return lista;
    }

    public List<Medico> buscarPorNome(String padrao) throws IOException {
        List<Medico> res = new ArrayList<>();
        for (Medico m : listarTodos()) {
            if (m.getNome() != null && m.getNome().toLowerCase().contains(padrao.toLowerCase()))
                res.add(m);
        }
        return res;
    }

    @Override
    public boolean deletar(int id) throws IOException {
        boolean ok = super.deletar(id);
        if (ok) { hashId.remover(id); bPlus.remover(id); }
        return ok;
    }

    @Override
    protected byte[] serializar(Medico m) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(tamanhoReg);
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(m.getId());
        dos.writeBoolean(m.isAtivo());
        writeStr(dos, m.getNome(),      T_NOME);
        writeStr(dos, m.getCrm(),       T_CRM);
        writeStr(dos, m.getEmail(),     T_EMAIL);
        writeStr(dos, m.getTelefones(), T_TELS);
        dos.flush();
        return baos.toByteArray();
    }

    @Override
    protected Medico desserializar(byte[] bytes) throws IOException {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes));
        int id     = dis.readInt();
        boolean at = dis.readBoolean();
        String nome = readStr(dis, T_NOME);
        String crm  = readStr(dis, T_CRM);
        String email= readStr(dis, T_EMAIL);
        String tels = readStr(dis, T_TELS);
        return new Medico(id, at, nome, crm, email, tels);
    }

    @Override protected int     getId(Medico m)            { return m.getId(); }
    @Override protected void    atribuirId(Medico m, int i){ m.setId(i); }
    @Override protected boolean isAtivo(Medico m)          { return m.isAtivo(); }
    @Override protected void    setAtivo(Medico m, boolean a){ m.setAtivo(a); }
}
