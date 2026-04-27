package clinica.service;

import clinica.dao.*;
import clinica.index.OrdenacaoExterna;
import clinica.model.*;
import java.io.IOException;
import java.util.List;

/** Fachada de serviços — agrega todos os DAOs e expõe operações de negócio */
public class ClinicaService {

    private final String                dataDir;
    private final PacienteDAO           pacienteDAO;
    private final MedicoDAO             medicoDAO;
    private final EspecialidadeDAO      especialidadeDAO;
    private final ConsultaDAO           consultaDAO;
    private final MedicoEspecialidadeDAO meDAO;
    private final UsuarioDAO            usuarioDAO;

    public ClinicaService(String dataDir) throws IOException {
        this.dataDir     = dataDir;
        pacienteDAO      = new PacienteDAO(dataDir);
        medicoDAO        = new MedicoDAO(dataDir);
        especialidadeDAO = new EspecialidadeDAO(dataDir);
        consultaDAO      = new ConsultaDAO(dataDir);
        meDAO            = new MedicoEspecialidadeDAO(dataDir);
        usuarioDAO       = new UsuarioDAO(dataDir);
        usuarioDAO.criarAdminSeNecessario();
    }

    // ─── Autenticação ───────────────────────────────────────────────────────
    public boolean login(String login, String senha) throws IOException {
        return usuarioDAO.autenticar(login, senha);
    }
    public Usuario buscarUsuarioPorLogin(String login) throws IOException {
        return usuarioDAO.buscarPorLogin(login);
    }
    public List<Usuario> listarUsuarios() throws IOException {
        return usuarioDAO.listarTodos();
    }
    public int criarUsuario(Usuario u) throws IOException { return usuarioDAO.criar(u); }
    public boolean deletarUsuario(int id) throws IOException { return usuarioDAO.deletar(id); }

    // ─── Pacientes ──────────────────────────────────────────────────────────
    public int           criarPaciente(Paciente p)    throws IOException { return pacienteDAO.criar(p); }
    public Paciente      buscarPaciente(int id)        throws IOException { return pacienteDAO.buscarPorIdHash(id); }
    public List<Paciente>listarPacientes()             throws IOException { return pacienteDAO.listarOrdenados(); }
    public List<Paciente>buscarPacienteNome(String n)  throws IOException { return pacienteDAO.buscarPorNome(n); }
    public boolean       atualizarPaciente(Paciente p) throws IOException { return pacienteDAO.atualizar(p); }
    public boolean       deletarPaciente(int id)       throws IOException { return pacienteDAO.deletar(id); }

    // ─── Médicos ────────────────────────────────────────────────────────────
    public int         criarMedico(Medico m)       throws IOException { return medicoDAO.criar(m); }
    public Medico      buscarMedico(int id)         throws IOException { return medicoDAO.buscarPorIdHash(id); }
    public List<Medico>listarMedicos()              throws IOException { return medicoDAO.listarOrdenados(); }
    public List<Medico>buscarMedicoNome(String n)   throws IOException { return medicoDAO.buscarPorNome(n); }
    public boolean     atualizarMedico(Medico m)    throws IOException { return medicoDAO.atualizar(m); }
    public boolean     deletarMedico(int id)        throws IOException { return medicoDAO.deletar(id); }

    // ─── Especialidades ─────────────────────────────────────────────────────
    public int              criarEsp(Especialidade e)   throws IOException { return especialidadeDAO.criar(e); }
    public Especialidade    buscarEsp(int id)            throws IOException { return especialidadeDAO.buscarPorIdHash(id); }
    public List<Especialidade> listarEsps()             throws IOException { return especialidadeDAO.listarTodos(); }
    public boolean          atualizarEsp(Especialidade e)throws IOException { return especialidadeDAO.atualizar(e); }
    public boolean          deletarEsp(int id)           throws IOException { return especialidadeDAO.deletar(id); }

    // ─── Vínculos Médico ↔ Especialidade (N:N) ────────────────────────────
    public int     vincularMedicoEsp(int idMedico, int idEsp) throws IOException {
        return meDAO.criar(new MedicoEspecialidade(0, true, idMedico, idEsp));
    }
    public boolean desvincularMedicoEsp(int idMedico, int idEsp) throws IOException {
        return meDAO.removerVinculo(idMedico, idEsp);
    }
    public List<Especialidade> especialidadesDeMedico(int idMedico) throws IOException {
        java.util.List<Especialidade> res = new java.util.ArrayList<>();
        for (int idE : meDAO.idEspecialidadesPorMedico(idMedico)) {
            Especialidade e = especialidadeDAO.buscarPorIdHash(idE);
            if (e != null) res.add(e);
        }
        return res;
    }
    public List<Medico> medicosPorEspecialidade(int idEsp) throws IOException {
        java.util.List<Medico> res = new java.util.ArrayList<>();
        for (int idM : meDAO.idMedicosPorEspecialidade(idEsp)) {
            Medico m = medicoDAO.buscarPorIdHash(idM);
            if (m != null) res.add(m);
        }
        return res;
    }

    // ─── Consultas ──────────────────────────────────────────────────────────
    public int           criarConsulta(Consulta c)    throws IOException { return consultaDAO.criar(c); }
    public Consulta      buscarConsulta(int id)        throws IOException { return consultaDAO.buscarPorIdHash(id); }
    public List<Consulta>listarConsultas()             throws IOException { return consultaDAO.listarOrdenadas(); }
    public List<Consulta>consultasPorPaciente(int id)  throws IOException { return consultaDAO.listarPorPaciente(id); }
    public List<Consulta>consultasPorMedico(int id)    throws IOException { return consultaDAO.listarPorMedico(id); }
    public boolean       atualizarConsulta(Consulta c) throws IOException { return consultaDAO.atualizar(c); }
    public boolean       deletarConsulta(int id)       throws IOException { return consultaDAO.deletar(id); }

    // ─── Ordenação Externa por Intercalação ─────────────────────────────────

    /**
     * Ordena pacientes.dat por nome (campo string, offset=5, tamanho=100)
     * e grava em pacientes_sorted.dat.
     * Estrutura do registro Paciente:
     *   [0-3]  int  id
     *   [4]    bool ativo
     *   [5-104] String nome (100 bytes)
     */
    public String ordenarPacientesPorNome(boolean decrescente) throws IOException {
        // offset 5 = 4 (int id) + 1 (boolean ativo)
        var cmp = OrdenacaoExterna.porCampoString(5, 100);
        if (decrescente) cmp = OrdenacaoExterna.decrescente(cmp);
        String saida = dataDir + "/pacientes_sorted.dat";
        new OrdenacaoExterna(629, cmp).ordenar(dataDir + "/pacientes.dat", saida);
        return saida;
    }

    /**
     * Ordena medicos.dat por nome (offset=5, tamanho=100).
     * Estrutura Medico: [0-3] id | [4] ativo | [5-104] nome (100 bytes)
     */
    public String ordenarMedicosPorNome(boolean decrescente) throws IOException {
        var cmp = OrdenacaoExterna.porCampoString(5, 100);
        if (decrescente) cmp = OrdenacaoExterna.decrescente(cmp);
        String saida = dataDir + "/medicos_sorted.dat";
        new OrdenacaoExterna(375, cmp).ordenar(dataDir + "/medicos.dat", saida);
        return saida;
    }

    /**
     * Ordena consultas.dat por data (campo string, offset=17, tamanho=10).
     * Estrutura Consulta:
     *   [0-3]  int  id
     *   [4]    bool ativo
     *   [5-8]  int  idPaciente
     *   [9-12] int  idMedico
     *   [13-22] String data (10 bytes, yyyy-MM-dd → ordenável lexicograficamente)
     */
    public String ordenarConsultasPorData(boolean decrescente) throws IOException {
        // offset: 4(id)+1(ativo)+4(idPac)+4(idMed) = 13
        var cmp = OrdenacaoExterna.porCampoString(13, 10);
        if (decrescente) cmp = OrdenacaoExterna.decrescente(cmp);
        String saida = dataDir + "/consultas_sorted.dat";
        new OrdenacaoExterna(651, cmp).ordenar(dataDir + "/consultas.dat", saida);
        return saida;
    }

    /**
     * Ordena consultas.dat por valor (double, offset=23).
     * Offset: 4(id)+1(ativo)+4(idPac)+4(idMed)+10(data)+5(horario) = 28
     */
    public String ordenarConsultasPorValor(boolean decrescente) throws IOException {
        var cmp = OrdenacaoExterna.porCampoDouble(28);
        if (decrescente) cmp = OrdenacaoExterna.decrescente(cmp);
        String saida = dataDir + "/consultas_sorted_valor.dat";
        new OrdenacaoExterna(651, cmp).ordenar(dataDir + "/consultas.dat", saida);
        return saida;
    }
}
