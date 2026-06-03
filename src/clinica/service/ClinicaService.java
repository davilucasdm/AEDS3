package clinica.service;

import clinica.dao.*;
import clinica.index.OrdenacaoExterna;
import clinica.model.*;
import java.io.IOException;
import java.util.*;

/**
 * Camada de serviço da Clínica Médica.
 *
 * Centraliza a lógica de negócio e integra todos os DAOs: - PacienteDAO,
 * MedicoDAO, EspecialidadeDAO, ConsultaDAO, UsuarioDAO - MedicoEspecialidadeDAO
 * ← FASE III: tabela N:N
 *
 * Responsabilidades desta classe: 1. Operações CRUD de todas as entidades. 2.
 * Relacionamento N:N (Médico ↔ Especialidade) via tabela intermediária. 3.
 * Integridade referencial (cascata de exclusão). 4. Ordenação externa
 * (intercalação) e travessia B+ para listagens ordenadas. 5. Autenticação de
 * usuários.
 */
public class ClinicaService {

    private final PacienteDAO pacienteDAO;
    private final MedicoDAO medicoDAO;
    private final EspecialidadeDAO espDAO;
    private final ConsultaDAO consultaDAO;
    private final UsuarioDAO usuarioDAO;
    private final MedicoEspecialidadeDAO meDAO;   // FASE III

    private final String dataDir;

    public ClinicaService(String dataDir) throws IOException {
        this.dataDir = dataDir;
        pacienteDAO = new PacienteDAO(dataDir);
        medicoDAO = new MedicoDAO(dataDir);
        espDAO = new EspecialidadeDAO(dataDir);
        consultaDAO = new ConsultaDAO(dataDir);
        usuarioDAO = new UsuarioDAO(dataDir);
        meDAO = new MedicoEspecialidadeDAO(dataDir);  // FASE III

        // Cria admin padrão se não existir nenhum usuário
        usuarioDAO.criarAdminSeNecessario();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // AUTENTICAÇÃO
    // ═══════════════════════════════════════════════════════════════════════
    public boolean login(String login, String senha) throws IOException {
        return usuarioDAO.autenticar(login, senha);
    }

    public Usuario buscarUsuarioPorLogin(String login) throws IOException {
        return usuarioDAO.buscarPorLogin(login);
    }

    public int criarUsuario(Usuario u) throws IOException {
        return usuarioDAO.criar(u);
    }

    public List<Usuario> listarUsuarios() throws IOException {
        return usuarioDAO.listarTodos();
    }

    public boolean deletarUsuario(int id) throws IOException {
        return usuarioDAO.deletar(id);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PACIENTES
    // ═══════════════════════════════════════════════════════════════════════
    public int criarPaciente(Paciente p) throws IOException {
        return pacienteDAO.criar(p);
    }

    public Paciente buscarPaciente(int id) throws IOException {
        Paciente p = pacienteDAO.buscarPorIdHash(id);
        return (p != null) ? p : pacienteDAO.buscarPorId(id);
    }

    public List<Paciente> listarPacientes() throws IOException {
        return pacienteDAO.listarTodos();
    }

    public List<Paciente> buscarPacienteNome(String padrao) throws IOException {
        return pacienteDAO.buscarPorNome(padrao);
    }

    public boolean atualizarPaciente(Paciente p) throws IOException {
        return pacienteDAO.atualizar(p);
    }

    /**
     * Exclui paciente e todas as consultas associadas (integridade referencial
     * 1:N).
     */
    public boolean deletarPaciente(int id) throws IOException {
        // Cascata: remove consultas do paciente
        for (Consulta c : consultaDAO.listarPorPaciente(id)) {
            consultaDAO.deletar(c.getId());
        }
        return pacienteDAO.deletar(id);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MÉDICOS
    // ═══════════════════════════════════════════════════════════════════════
    public int criarMedico(Medico m) throws IOException {
        return medicoDAO.criar(m);
    }

    public Medico buscarMedico(int id) throws IOException {
        Medico m = medicoDAO.buscarPorIdHash(id);
        return (m != null) ? m : medicoDAO.buscarPorId(id);
    }

    public List<Medico> listarMedicos() throws IOException {
        return medicoDAO.listarTodos();
    }

    public List<Medico> buscarMedicoNome(String padrao) throws IOException {
        List<Medico> res = new ArrayList<>();
        for (Medico m : medicoDAO.listarTodos()) {
            if (m.getNome() != null && m.getNome().toLowerCase().contains(padrao.toLowerCase())) {
                res.add(m);
            }
        }
        return res;
    }

    public boolean atualizarMedico(Medico m) throws IOException {
        return medicoDAO.atualizar(m);
    }

    /**
     * Exclui médico, suas consultas (1:N) e seus vínculos de especialidade
     * (N:N). Integridade referencial completa.
     */
    public boolean deletarMedico(int id) throws IOException {
        // Cascata 1:N → consultas do médico
        for (Consulta c : consultaDAO.listarPorMedico(id)) {
            consultaDAO.deletar(c.getId());
        }
        // Cascata N:N → vínculos de especialidade
        removerTodosVinculosMedico(id);
        return medicoDAO.deletar(id);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ESPECIALIDADES
    // ═══════════════════════════════════════════════════════════════════════
    public int criarEsp(Especialidade e) throws IOException {
        return espDAO.criar(e);
    }

    public Especialidade buscarEsp(int id) throws IOException {
        Especialidade e = espDAO.buscarPorIdHash(id);
        return (e != null) ? e : espDAO.buscarPorId(id);
    }

    public List<Especialidade> listarEsps() throws IOException {
        return espDAO.listarTodos();
    }

    public boolean atualizarEsp(Especialidade e) throws IOException {
        return espDAO.atualizar(e);
    }

    /**
     * Exclui especialidade e todos os vínculos N:N associados.
     */
    public boolean deletarEsp(int id) throws IOException {
        // Cascata N:N → remove vínculos da especialidade
        removerTodosVinculosEspecialidade(id);
        return espDAO.deletar(id);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CONSULTAS
    // ═══════════════════════════════════════════════════════════════════════
    public int criarConsulta(Consulta c) throws IOException {
        return consultaDAO.criar(c);
    }

    public Consulta buscarConsulta(int id) throws IOException {
        Consulta c = consultaDAO.buscarPorIdHash(id);
        return (c != null) ? c : consultaDAO.buscarPorId(id);
    }

    public List<Consulta> listarConsultas() throws IOException {
        return consultaDAO.listarTodos();
    }

    public List<Consulta> consultasPorPaciente(int idPaciente) throws IOException {
        return consultaDAO.listarPorPaciente(idPaciente);
    }

    public List<Consulta> consultasPorMedico(int idMedico) throws IOException {
        return consultaDAO.listarPorMedico(idMedico);
    }

    public boolean atualizarConsulta(Consulta c) throws IOException {
        return consultaDAO.atualizar(c);
    }

    public boolean deletarConsulta(int id) throws IOException {
        return consultaDAO.deletar(id);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // RELACIONAMENTO N:N — MÉDICO ↔ ESPECIALIDADE  (FASE III)
    // ═══════════════════════════════════════════════════════════════════════
    /**
     * Cria um vínculo entre médico e especialidade. Se o vínculo já existe,
     * retorna o ID existente sem duplicar.
     *
     * @return ID do registro em MedicoEspecialidade
     */
    public int vincularMedicoEsp(int idMedico, int idEspecialidade) throws IOException {
        MedicoEspecialidade me = new MedicoEspecialidade(0, true, idMedico, idEspecialidade);
        return meDAO.criar(me);
    }

    /**
     * Remove logicamente o vínculo entre médico e especialidade.
     */
    public boolean desvincularMedicoEsp(int idMedico, int idEspecialidade) throws IOException {
        return meDAO.removerVinculo(idMedico, idEspecialidade);
    }

    /**
     * Retorna a lista de Especialidades associadas a um médico. Navegação N:N:
     * Médico → Especialidades.
     */
    public List<Especialidade> especialidadesDeMedico(int idMedico) throws IOException {
        List<Especialidade> resultado = new ArrayList<>();
        for (int idEsp : meDAO.idEspecialidadesPorMedico(idMedico)) {
            Especialidade e = buscarEsp(idEsp);
            if (e != null) {
                resultado.add(e);
            }
        }
        return resultado;
    }

    /**
     * Retorna a lista de Médicos que possuem determinada especialidade.
     * Navegação N:N inversa: Especialidade → Médicos.
     */
    public List<Medico> medicosPorEspecialidade(int idEspecialidade) throws IOException {
        List<Medico> resultado = new ArrayList<>();
        for (int idMed : meDAO.idMedicosPorEspecialidade(idEspecialidade)) {
            Medico m = buscarMedico(idMed);
            if (m != null) {
                resultado.add(m);
            }
        }
        return resultado;
    }

    /**
     * Remove todos os vínculos N:N de um médico (cascata ao deletar médico).
     */
    private void removerTodosVinculosMedico(int idMedico) throws IOException {
        for (int idEsp : meDAO.idEspecialidadesPorMedico(idMedico)) {
            meDAO.removerVinculo(idMedico, idEsp);
        }
    }

    /**
     * Remove todos os vínculos N:N de uma especialidade (cascata ao deletar
     * especialidade).
     */
    private void removerTodosVinculosEspecialidade(int idEspecialidade) throws IOException {
        for (int idMed : meDAO.idMedicosPorEspecialidade(idEspecialidade)) {
            meDAO.removerVinculo(idMed, idEspecialidade);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ORDENAÇÃO EXTERNA (intercalação) + TRAVESSIA B+
    // ═══════════════════════════════════════════════════════════════════════
    /**
     * Ordena pacientes por nome via ordenação externa por intercalação. A
     * Árvore B+ indexa por ID; a ordenação por nome usa OrdenacaoExterna.
     *
     * @return caminho do arquivo gerado
     */
    public String ordenarPacientesPorNome(boolean desc) throws IOException {
        String saida = dataDir + "/pacientes_sorted.dat";
        // Paciente: offset do nome = 4(id)+1(ativo) = 5, tamanho = 100 bytes
        var cmp = OrdenacaoExterna.porCampoString(5, 100);
        if (desc) {
            cmp = OrdenacaoExterna.decrescente(cmp);
        }
        new OrdenacaoExterna(629, cmp).ordenar(dataDir + "/pacientes.dat", saida);
        return saida;
    }

    /**
     * Ordena médicos por nome via Árvore B+ (travessia em ordem) + fallback
     * para ordenação externa quando a B+ não indexa o campo nome.
     *
     * Como a B+ de médicos indexa por ID (não por nome), usamos
     * OrdenacaoExterna para ordenar pelo campo nome — demonstrando ambas as
     * funcionalidades.
     *
     * @return caminho do arquivo gerado
     */
    public String ordenarMedicosPorNome(boolean desc) throws IOException {
        String saida = dataDir + "/medicos_sorted.dat";
        // Medico: offset do nome = 4(id)+1(ativo) = 5, tamanho = 100 bytes
        var cmp = OrdenacaoExterna.porCampoString(5, 100);
        if (desc) {
            cmp = OrdenacaoExterna.decrescente(cmp);
        }
        new OrdenacaoExterna(375, cmp).ordenar(dataDir + "/medicos.dat", saida);
        return saida;
    }

    /**
     * Ordena consultas por data via Árvore B+ (listarOrdenadas usa B+). Para
     * fins de exportação, também gera arquivo ordenado via OrdenacaoExterna.
     *
     * @return caminho do arquivo gerado
     */
    public String ordenarConsultasPorData(boolean desc) throws IOException {
        String saida = dataDir + "/consultas_sorted_data.dat";
        // Consulta: offset da data = 4+1+4+4 = 13, tamanho = 10 bytes
        var cmp = OrdenacaoExterna.porCampoString(13, 10);
        if (desc) {
            cmp = OrdenacaoExterna.decrescente(cmp);
        }
        new OrdenacaoExterna(651, cmp).ordenar(dataDir + "/consultas.dat", saida);
        return saida;
    }

    /**
     * Ordena consultas por valor via ordenação externa.
     *
     * @return caminho do arquivo gerado
     */
    public String ordenarConsultasPorValor(boolean desc) throws IOException {
        String saida = dataDir + "/consultas_sorted_valor.dat";
        // Consulta: offset do valor = 4+1+4+4+10+5 = 28, tipo double (8 bytes)
        var cmp = OrdenacaoExterna.porCampoDouble(28);
        if (desc) {
            cmp = OrdenacaoExterna.decrescente(cmp);
        }
        new OrdenacaoExterna(651, cmp).ordenar(dataDir + "/consultas.dat", saida);
        return saida;
    }

    /**
     * Lista médicos em ordem crescente de ID usando travessia da Árvore B+. Sem
     * ordenação em memória principal — usa o encadeamento de folhas da B+.
     */
    public List<Medico> listarMedicosOrdenadosBPlus() throws IOException {
        return medicoDAO.listarOrdenados();
    }

    /**
     * Lista consultas em ordem via Árvore B+.
     */
    public List<Consulta> listarConsultasOrdenadas() throws IOException {
        return consultaDAO.listarOrdenadas();
    }
}
