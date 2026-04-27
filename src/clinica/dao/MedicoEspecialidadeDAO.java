package clinica.dao;

import clinica.index.HashExtensivel;
import clinica.model.MedicoEspecialidade;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class MedicoEspecialidadeDAO extends BaseDAO<MedicoEspecialidade> {

    // total = 4+1+4+4 = 13
    private final HashExtensivel hashId;

    public MedicoEspecialidadeDAO(String dataDir) throws IOException {
        super(dataDir + "/medico_especialidade.dat", 13);
        hashId = new HashExtensivel(dataDir + "/medico_esp_hash");
    }

    @Override
    public int criar(MedicoEspecialidade me) throws IOException {
        // Verifica duplicata
        for (MedicoEspecialidade ex : listarTodos()) {
            if (ex.getIdMedico() == me.getIdMedico() &&
                ex.getIdEspecialidade() == me.getIdEspecialidade()) {
                return ex.getId(); // já existe
            }
        }
        int id = super.criar(me);
        hashId.inserir(id, offsetReg(totalSlots() - 1));
        return id;
    }

    /** Relacionamento N:N → especialidades de um médico */
    public List<Integer> idEspecialidadesPorMedico(int idMedico) throws IOException {
        List<Integer> res = new ArrayList<>();
        for (MedicoEspecialidade me : listarTodos()) {
            if (me.getIdMedico() == idMedico) res.add(me.getIdEspecialidade());
        }
        return res;
    }

    /** Relacionamento N:N → médicos de uma especialidade */
    public List<Integer> idMedicosPorEspecialidade(int idEsp) throws IOException {
        List<Integer> res = new ArrayList<>();
        for (MedicoEspecialidade me : listarTodos()) {
            if (me.getIdEspecialidade() == idEsp) res.add(me.getIdMedico());
        }
        return res;
    }

    /** Remove vínculo entre médico e especialidade */
    public boolean removerVinculo(int idMedico, int idEsp) throws IOException {
        int slots = totalSlots();
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            for (int i = 0; i < slots; i++) {
                raf.seek(offsetReg(i));
                byte[] buf = new byte[tamanhoReg];
                raf.readFully(buf);
                MedicoEspecialidade me = desserializar(buf);
                if (me.isAtivo() && me.getIdMedico() == idMedico && me.getIdEspecialidade() == idEsp) {
                    return deletar(me.getId());
                }
            }
        }
        return false;
    }

    @Override
    protected byte[] serializar(MedicoEspecialidade me) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(tamanhoReg);
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(me.getId());
        dos.writeBoolean(me.isAtivo());
        dos.writeInt(me.getIdMedico());
        dos.writeInt(me.getIdEspecialidade());
        dos.flush();
        return baos.toByteArray();
    }

    @Override
    protected MedicoEspecialidade desserializar(byte[] bytes) throws IOException {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes));
        int id    = dis.readInt();
        boolean at= dis.readBoolean();
        int idM   = dis.readInt();
        int idE   = dis.readInt();
        return new MedicoEspecialidade(id, at, idM, idE);
    }

    @Override protected int     getId(MedicoEspecialidade m)             { return m.getId(); }
    @Override protected void    atribuirId(MedicoEspecialidade m, int i) { m.setId(i); }
    @Override protected boolean isAtivo(MedicoEspecialidade m)           { return m.isAtivo(); }
    @Override protected void    setAtivo(MedicoEspecialidade m, boolean a){ m.setAtivo(a); }
}
