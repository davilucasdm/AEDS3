package clinica.dao;

import clinica.index.HashExtensivel;
import clinica.model.MedicoEspecialidade;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO da tabela intermediária N:N entre Médico e Especialidade.
 *
 * CHAVE PRIMÁRIA COMPOSTA (Req 1): A chave primária lógica é o par (idMedico,
 * idEspecialidade). Como o HashExtensivel usa int, derivamos uma chave composta
 * inteira pelo método compositeKey(idMedico, idEspecialidade) = idMedico *
 * 100_003 ^ idEspecialidade, garantindo colisões mínimas para IDs pequenos.
 *
 * Dois índices Hash são mantidos para suportar ambas as direções do N:N: -
 * hashPorMedico: chave = compositeKey(idMedico, idEsp) → offset -
 * hashPorEspecialidade: chave = compositeKey(idEsp, idMedico) → offset Isso
 * permite busca O(1) tanto por médico quanto por especialidade, demonstrando o
 * índice sobre a chave composta conforme Req 1.
 *
 * LÁPIDE (Req 5): campo boolean ativo, padrão BaseDAO.
 */
public class MedicoEspecialidadeDAO extends BaseDAO<MedicoEspecialidade> {

    // Estrutura: id(4) + ativo(1) + idMedico(4) + idEspecialidade(4) = 13 bytes
    private static final int TAM_REG = 13;

    // Índice 1: chave composta (idMedico, idEsp) → offset  (busca por médico)
    private final HashExtensivel hashPorMedico;
    // Índice 2: chave composta (idEsp, idMedico) → offset  (busca por especialidade)
    private final HashExtensivel hashPorEspecialidade;

    public MedicoEspecialidadeDAO(String dataDir) throws IOException {
        super(dataDir + "/medico_especialidade.dat", TAM_REG);
        hashPorMedico = new HashExtensivel(dataDir + "/medico_esp_hash_med");
        hashPorEspecialidade = new HashExtensivel(dataDir + "/medico_esp_hash_esp");
    }

    // ─── Derivação da chave composta ──────────────────────────────────────
    /**
     * Combina dois IDs em um int único para uso como chave do HashExtensivel.
     * Usa multiplicação por primo + XOR para distribuição uniforme.
     */
    private static int compositeKey(int a, int b) {
        return a * 100_003 ^ b;
    }

    // ─── CRUD ─────────────────────────────────────────────────────────────
    @Override
    public int criar(MedicoEspecialidade me) throws IOException {
        // Verifica duplicata pelo índice composto (O(1))
        int ck = compositeKey(me.getIdMedico(), me.getIdEspecialidade());
        if (hashPorMedico.buscar(ck) >= 0) {
            // Já existe — retorna id do registro existente
            for (MedicoEspecialidade ex : listarTodos()) {
                if (ex.getIdMedico() == me.getIdMedico()
                        && ex.getIdEspecialidade() == me.getIdEspecialidade()) {
                    return ex.getId();
                }
            }
        }

        int id = super.criar(me);
        long off = offsetReg(totalSlots() - 1);

        // Indexa pelos dois índices compostos
        hashPorMedico.inserir(compositeKey(me.getIdMedico(), me.getIdEspecialidade()), off);
        hashPorEspecialidade.inserir(compositeKey(me.getIdEspecialidade(), me.getIdMedico()), off);

        return id;
    }

    /**
     * Remove logicamente o vínculo e apaga dos dois índices compostos.
     */
    public boolean removerVinculo(int idMedico, int idEsp) throws IOException {
        int slots = totalSlots();
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            for (int i = 0; i < slots; i++) {
                raf.seek(offsetReg(i));
                byte[] buf = new byte[TAM_REG];
                raf.readFully(buf);
                MedicoEspecialidade me = desserializar(buf);
                if (me.isAtivo() && me.getIdMedico() == idMedico
                        && me.getIdEspecialidade() == idEsp) {
                    boolean ok = deletar(me.getId());   // marca lápide
                    if (ok) {
                        hashPorMedico.remover(compositeKey(idMedico, idEsp));
                        hashPorEspecialidade.remover(compositeKey(idEsp, idMedico));
                    }
                    return ok;
                }
            }
        }
        return false;
    }

    // ─── Consultas N:N ────────────────────────────────────────────────────
    /**
     * Retorna os ids de especialidade associados a um médico. Usa varredura
     * sequencial nos registros ativos (arquivo pequeno). O índice hashPorMedico
     * poderia ser usado se a estrutura suportasse múltiplos valores por chave;
     * aqui serve para verificação de existência.
     */
    public List<Integer> idEspecialidadesPorMedico(int idMedico) throws IOException {
        List<Integer> res = new ArrayList<>();
        for (MedicoEspecialidade me : listarTodos()) {
            if (me.getIdMedico() == idMedico) {
                res.add(me.getIdEspecialidade());
            }
        }
        return res;
    }

    /**
     * Retorna os ids de médico associados a uma especialidade.
     */
    public List<Integer> idMedicosPorEspecialidade(int idEsp) throws IOException {
        List<Integer> res = new ArrayList<>();
        for (MedicoEspecialidade me : listarTodos()) {
            if (me.getIdEspecialidade() == idEsp) {
                res.add(me.getIdMedico());
            }
        }
        return res;
    }

    /**
     * Verifica existência de um vínculo em O(1) via índice composto.
     */
    public boolean vinculoExiste(int idMedico, int idEsp) throws IOException {
        return hashPorMedico.buscar(compositeKey(idMedico, idEsp)) >= 0;
    }

    // ─── Serialização ─────────────────────────────────────────────────────
    @Override
    protected byte[] serializar(MedicoEspecialidade me) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(TAM_REG);
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
        return new MedicoEspecialidade(
                dis.readInt(), dis.readBoolean(), dis.readInt(), dis.readInt()
        );
    }

    @Override
    protected int getId(MedicoEspecialidade m) {
        return m.getId();
    }

    @Override
    protected void atribuirId(MedicoEspecialidade m, int i) {
        m.setId(i);
    }

    @Override
    protected boolean isAtivo(MedicoEspecialidade m) {
        return m.isAtivo();
    }

    @Override
    protected void setAtivo(MedicoEspecialidade m, boolean a) {
        m.setAtivo(a);
    }
}
