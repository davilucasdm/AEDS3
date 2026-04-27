package clinica.dao;

import clinica.index.ArvoreBMais;
import clinica.index.HashExtensivel;
import clinica.model.Consulta;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ConsultaDAO extends BaseDAO<Consulta> {

    private static final int T_DATA  = 10;
    private static final int T_HOR   = 5;
    private static final int T_SINT  = 300;
    private static final int T_STAT  = 20;
    private static final int T_OBS   = 295;
    // total = 4+1+4+4+10+5+8+300+20+295 = 651

    private final HashExtensivel hashId;
    private final ArvoreBMais    bPlusPorData;   // indexa por data (string comparável)

    public ConsultaDAO(String dataDir) throws IOException {
        super(dataDir + "/consultas.dat", 651);
        hashId       = new HashExtensivel(dataDir + "/consultas_hash");
        bPlusPorData = new ArvoreBMais(dataDir + "/consultas_btree_data");
    }

    @Override
    public int criar(Consulta c) throws IOException {
        int id = super.criar(c);
        long off = offsetReg(totalSlots() - 1);
        hashId.inserir(id, off);
        // Usa hash do campo data como chave no B+ (simplificação)
        bPlusPorData.inserir(id, off);
        return id;
    }

    public Consulta buscarPorIdHash(int id) throws IOException {
        long off = hashId.buscar(id);
        if (off < 0) return null;
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            raf.seek(off);
            byte[] buf = new byte[tamanhoReg];
            raf.readFully(buf);
            Consulta c = desserializar(buf);
            return c.isAtivo() ? c : null;
        }
    }

    /** Relacionamento 1:N → todas as consultas de um paciente */
    public List<Consulta> listarPorPaciente(int idPaciente) throws IOException {
        List<Consulta> res = new ArrayList<>();
        for (Consulta c : listarTodos()) {
            if (c.getIdPaciente() == idPaciente) res.add(c);
        }
        return res;
    }

    /** Relacionamento 1:N → todas as consultas de um médico */
    public List<Consulta> listarPorMedico(int idMedico) throws IOException {
        List<Consulta> res = new ArrayList<>();
        for (Consulta c : listarTodos()) {
            if (c.getIdMedico() == idMedico) res.add(c);
        }
        return res;
    }

    /** Lista ordenadas por ID (via B+) */
    public List<Consulta> listarOrdenadas() throws IOException {
        List<Consulta> lista = new ArrayList<>();
        for (long[] par : bPlusPorData.listarOrdenado()) {
            try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
                raf.seek(par[1]);
                byte[] buf = new byte[tamanhoReg];
                raf.readFully(buf);
                Consulta c = desserializar(buf);
                if (c.isAtivo()) lista.add(c);
            }
        }
        return lista;
    }

    @Override
    public boolean deletar(int id) throws IOException {
        boolean ok = super.deletar(id);
        if (ok) { hashId.remover(id); bPlusPorData.remover(id); }
        return ok;
    }

    @Override
    protected byte[] serializar(Consulta c) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(tamanhoReg);
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(c.getId());
        dos.writeBoolean(c.isAtivo());
        dos.writeInt(c.getIdPaciente());
        dos.writeInt(c.getIdMedico());
        writeStr(dos, c.getData(),       T_DATA);
        writeStr(dos, c.getHorario(),    T_HOR);
        dos.writeDouble(c.getValor());
        writeStr(dos, c.getSintomas(),   T_SINT);
        writeStr(dos, c.getStatus(),     T_STAT);
        writeStr(dos, c.getObservacoes(),T_OBS);
        dos.flush();
        return baos.toByteArray();
    }

    @Override
    protected Consulta desserializar(byte[] bytes) throws IOException {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes));
        int     id    = dis.readInt();
        boolean ativ  = dis.readBoolean();
        int     idPac = dis.readInt();
        int     idMed = dis.readInt();
        String data   = readStr(dis, T_DATA);
        String hor    = readStr(dis, T_HOR);
        double valor  = dis.readDouble();
        String sint   = readStr(dis, T_SINT);
        String stat   = readStr(dis, T_STAT);
        String obs    = readStr(dis, T_OBS);
        return new Consulta(id, ativ, idPac, idMed, data, hor, valor, sint, stat, obs);
    }

    @Override protected int     getId(Consulta c)             { return c.getId(); }
    @Override protected void    atribuirId(Consulta c, int i) { c.setId(i); }
    @Override protected boolean isAtivo(Consulta c)           { return c.isAtivo(); }
    @Override protected void    setAtivo(Consulta c, boolean a){ c.setAtivo(a); }
}
