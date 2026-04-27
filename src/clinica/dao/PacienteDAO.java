package clinica.dao;

import clinica.index.ArvoreBMais;
import clinica.index.HashExtensivel;
import clinica.model.Paciente;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class PacienteDAO extends BaseDAO<Paciente> {

    // Tamanhos dos campos (bytes): total = 629
    private static final int T_ID    = 4;
    private static final int T_ATIVO = 1;
    private static final int T_NOME  = 100;
    private static final int T_CPF   = 15;
    private static final int T_DNASC = 10;
    private static final int T_EMAIL = 100;
    private static final int T_TELS  = 200;
    private static final int T_END   = 199;
    // total = 4+1+100+15+10+100+200+199 = 629

    private final HashExtensivel hashId;
    private final ArvoreBMais    bPlusPorId;

    public PacienteDAO(String dataDir) throws IOException {
        super(dataDir + "/pacientes.dat", 629);
        hashId     = new HashExtensivel(dataDir + "/pacientes_hash");
        bPlusPorId = new ArvoreBMais(dataDir + "/pacientes_btree");
    }

    @Override
    public int criar(Paciente p) throws IOException {
        int id = super.criar(p);
        long offset = offsetReg(totalSlots() - 1);
        hashId.inserir(id, offset);
        bPlusPorId.inserir(id, offset);
        return id;
    }

    /** Busca rápida por ID usando Hash Extensível */
    public Paciente buscarPorIdHash(int id) throws IOException {
        long offset = hashId.buscar(id);
        if (offset < 0) return null;
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            raf.seek(offset);
            byte[] buf = new byte[tamanhoReg];
            raf.readFully(buf);
            Paciente p = desserializar(buf);
            return p.isAtivo() ? p : null;
        }
    }

    /** Lista todos em ordem crescente de ID usando Árvore B+ */
    public List<Paciente> listarOrdenados() throws IOException {
        List<Paciente> lista = new ArrayList<>();
        for (long[] par : bPlusPorId.listarOrdenado()) {
            try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
                raf.seek(par[1]);
                byte[] buf = new byte[tamanhoReg];
                raf.readFully(buf);
                Paciente p = desserializar(buf);
                if (p.isAtivo()) lista.add(p);
            }
        }
        return lista;
    }

    /** Busca por nome (casamento de padrão — simples contains para fase 1) */
    public List<Paciente> buscarPorNome(String padrao) throws IOException {
        List<Paciente> res = new ArrayList<>();
        for (Paciente p : listarTodos()) {
            if (p.getNome() != null && p.getNome().toLowerCase().contains(padrao.toLowerCase()))
                res.add(p);
        }
        return res;
    }

    @Override
    public boolean deletar(int id) throws IOException {
        boolean ok = super.deletar(id);
        if (ok) { hashId.remover(id); bPlusPorId.remover(id); }
        return ok;
    }

    // ─── Serialização/desserialização ───────────────────────────────────────
    @Override
    protected byte[] serializar(Paciente p) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(tamanhoReg);
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(p.getId());
        dos.writeBoolean(p.isAtivo());
        writeStr(dos, p.getNome(),            T_NOME);
        writeStr(dos, p.getCpf(),             T_CPF);
        writeStr(dos, p.getDataNascimento(),  T_DNASC);
        writeStr(dos, p.getEmail(),           T_EMAIL);
        writeStr(dos, p.getTelefones(),       T_TELS);
        writeStr(dos, p.getEndereco(),        T_END);
        dos.flush();
        return baos.toByteArray();
    }

    @Override
    protected Paciente desserializar(byte[] bytes) throws IOException {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes));
        int    id    = dis.readInt();
        boolean ativ = dis.readBoolean();
        String nome  = readStr(dis, T_NOME);
        String cpf   = readStr(dis, T_CPF);
        String dnasc = readStr(dis, T_DNASC);
        String email = readStr(dis, T_EMAIL);
        String tels  = readStr(dis, T_TELS);
        String end   = readStr(dis, T_END);
        return new Paciente(id, ativ, nome, cpf, dnasc, email, tels, end);
    }

    @Override protected int     getId(Paciente p)         { return p.getId(); }
    @Override protected void    atribuirId(Paciente p, int id) { p.setId(id); }
    @Override protected boolean isAtivo(Paciente p)        { return p.isAtivo(); }
    @Override protected void    setAtivo(Paciente p, boolean a) { p.setAtivo(a); }
}
