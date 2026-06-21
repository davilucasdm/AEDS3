package clinica.service;

import clinica.dao.*;
import clinica.index.OrdenacaoExterna;
import clinica.model.*;
import clinica.util.CasamentoPadroes;
import java.io.IOException;
import java.util.*;

public class ClinicaService {

    private final PacienteDAO pacienteDAO;
    private final MedicoDAO medicoDAO;
    private final EspecialidadeDAO espDAO;
    private final ConsultaDAO consultaDAO;
    private final UsuarioDAO usuarioDAO;
    private final MedicoEspecialidadeDAO meDAO;   // FASE III
    private final CompactacaoService compactacaoService; // FASE IV

    private final String dataDir;

    public ClinicaService(String dataDir) throws IOException {
        this.dataDir = dataDir;
        pacienteDAO = new PacienteDAO(dataDir);
        medicoDAO = new MedicoDAO(dataDir);
        espDAO = new EspecialidadeDAO(dataDir);
        consultaDAO = new ConsultaDAO(dataDir);
        usuarioDAO = new UsuarioDAO(dataDir);
        meDAO = new MedicoEspecialidadeDAO(dataDir);  // FASE III
        compactacaoService = new CompactacaoService(dataDir); // FASE IV

        // Cria admin padrão se não existir nenhum usuário
        usuarioDAO.criarAdminSeNecessario();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // AUTENTICAÇÃO
    // ═══════════════════════════════════════════════════════════════════════
    public String getDataDir() { return dataDir; }

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

    public boolean deletarPaciente(int id) throws IOException {
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

    public boolean deletarMedico(int id) throws IOException {
        for (Consulta c : consultaDAO.listarPorMedico(id)) {
            consultaDAO.deletar(c.getId());
        }
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

    public boolean deletarEsp(int id) throws IOException {
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
    public int vincularMedicoEsp(int idMedico, int idEspecialidade) throws IOException {
        MedicoEspecialidade me = new MedicoEspecialidade(0, true, idMedico, idEspecialidade);
        return meDAO.criar(me);
    }

    public boolean desvincularMedicoEsp(int idMedico, int idEspecialidade) throws IOException {
        return meDAO.removerVinculo(idMedico, idEspecialidade);
    }

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

    private void removerTodosVinculosMedico(int idMedico) throws IOException {
        for (int idEsp : meDAO.idEspecialidadesPorMedico(idMedico)) {
            meDAO.removerVinculo(idMedico, idEsp);
        }
    }

    private void removerTodosVinculosEspecialidade(int idEspecialidade) throws IOException {
        for (int idMed : meDAO.idMedicosPorEspecialidade(idEspecialidade)) {
            meDAO.removerVinculo(idMed, idEspecialidade);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ORDENAÇÃO EXTERNA (intercalação) + TRAVESSIA B+
    // ═══════════════════════════════════════════════════════════════════════
    public String ordenarPacientesPorNome(boolean desc) throws IOException {
        String saida = dataDir + "/pacientes_sorted.dat";
        var cmp = OrdenacaoExterna.porCampoString(5, 100);
        if (desc) {
            cmp = OrdenacaoExterna.decrescente(cmp);
        }
        new OrdenacaoExterna(629, cmp).ordenar(dataDir + "/pacientes.dat", saida);
        return saida;
    }

    public String ordenarMedicosPorNome(boolean desc) throws IOException {
        String saida = dataDir + "/medicos_sorted.dat";
        var cmp = OrdenacaoExterna.porCampoString(5, 100);
        if (desc) {
            cmp = OrdenacaoExterna.decrescente(cmp);
        }
        new OrdenacaoExterna(375, cmp).ordenar(dataDir + "/medicos.dat", saida);
        return saida;
    }

    public String ordenarConsultasPorData(boolean desc) throws IOException {
        String saida = dataDir + "/consultas_sorted_data.dat";
        var cmp = OrdenacaoExterna.porCampoString(13, 10);
        if (desc) {
            cmp = OrdenacaoExterna.decrescente(cmp);
        }
        new OrdenacaoExterna(651, cmp).ordenar(dataDir + "/consultas.dat", saida);
        return saida;
    }

    public String ordenarConsultasPorValor(boolean desc) throws IOException {
        String saida = dataDir + "/consultas_sorted_valor.dat";
        var cmp = OrdenacaoExterna.porCampoDouble(28);
        if (desc) {
            cmp = OrdenacaoExterna.decrescente(cmp);
        }
        new OrdenacaoExterna(651, cmp).ordenar(dataDir + "/consultas.dat", saida);
        return saida;
    }

    public List<Medico> listarMedicosOrdenadosBPlus() throws IOException {
        return medicoDAO.listarOrdenados();
    }

    public List<Consulta> listarConsultasOrdenadas() throws IOException {
        return consultaDAO.listarOrdenadas();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FASE IV — COMPACTAÇÃO (Huffman e LZW)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Gera backup compactado de todos os arquivos de dados do sistema
     * (.dat, .hdir, .hbkt, .btree).
     *
     * @param nomeArquivo caminho do arquivo de saída (ex.: "data/backup_huffman.hbak")
     * @param algoritmo   HUFFMAN ou LZW
     * @return DTO com taxas de compressão por arquivo e total
     */
    public CompactacaoService.CompactacaoResult compactar(String nomeArquivo,
            CompactacaoService.Algoritmo algoritmo) throws IOException {
        return compactacaoService.compactar(nomeArquivo, algoritmo);
    }

    /**
     * Restaura todos os arquivos de dados a partir de um backup compactado.
     *
     * @return lista dos nomes de arquivo restaurados
     */
    public List<String> descompactar(String nomeArquivo) throws IOException {
        return compactacaoService.descompactar(nomeArquivo);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FASE IV — CASAMENTO DE PADRÕES (KMP e Boyer-Moore)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Busca pacientes cujo nome contém o padrão usando KMP (case-insensitive).
     */
    public List<Paciente> buscarPacienteKMP(String padrao) throws IOException {
        List<Paciente> resultado = new ArrayList<>();
        for (Paciente p : pacienteDAO.listarTodos()) {
            if (p.getNome() != null
                    && !CasamentoPadroes.kmpIgnoreCase(p.getNome(), padrao).isEmpty()) {
                resultado.add(p);
            }
        }
        return resultado;
    }

    /**
     * Busca pacientes cujo nome contém o padrão usando Boyer-Moore (case-insensitive).
     */
    public List<Paciente> buscarPacienteBM(String padrao) throws IOException {
        List<Paciente> resultado = new ArrayList<>();
        for (Paciente p : pacienteDAO.listarTodos()) {
            if (p.getNome() != null
                    && !CasamentoPadroes.bmIgnoreCase(p.getNome(), padrao).isEmpty()) {
                resultado.add(p);
            }
        }
        return resultado;
    }

    /**
     * Busca médicos cujo nome contém o padrão usando KMP (case-insensitive).
     */
    public List<Medico> buscarMedicoKMP(String padrao) throws IOException {
        List<Medico> resultado = new ArrayList<>();
        for (Medico m : medicoDAO.listarTodos()) {
            if (m.getNome() != null
                    && !CasamentoPadroes.kmpIgnoreCase(m.getNome(), padrao).isEmpty()) {
                resultado.add(m);
            }
        }
        return resultado;
    }

    /**
     * Busca médicos cujo nome contém o padrão usando Boyer-Moore (case-insensitive).
     */
    public List<Medico> buscarMedicoBM(String padrao) throws IOException {
        List<Medico> resultado = new ArrayList<>();
        for (Medico m : medicoDAO.listarTodos()) {
            if (m.getNome() != null
                    && !CasamentoPadroes.bmIgnoreCase(m.getNome(), padrao).isEmpty()) {
                resultado.add(m);
            }
        }
        return resultado;
    }
}
