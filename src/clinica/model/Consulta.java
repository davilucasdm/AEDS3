package clinica.model;

public class Consulta {
    private int id;
    private boolean ativo;
    private int idPaciente;
    private int idMedico;
    private String data;         // yyyy-MM-dd
    private String horario;      // HH:mm
    private double valor;
    private String sintomas;     // multivalorado: separado por ";"
    private String status;       // AGENDADA | REALIZADA | CANCELADA
    private String observacoes;

    public Consulta() {}
    public Consulta(int id, boolean ativo, int idPaciente, int idMedico, String data,
                    String horario, double valor, String sintomas, String status, String observacoes) {
        this.id = id; this.ativo = ativo; this.idPaciente = idPaciente;
        this.idMedico = idMedico; this.data = data; this.horario = horario;
        this.valor = valor; this.sintomas = sintomas; this.status = status;
        this.observacoes = observacoes;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public int getIdPaciente() { return idPaciente; }
    public void setIdPaciente(int idPaciente) { this.idPaciente = idPaciente; }
    public int getIdMedico() { return idMedico; }
    public void setIdMedico(int idMedico) { this.idMedico = idMedico; }
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
    public String getHorario() { return horario; }
    public void setHorario(String horario) { this.horario = horario; }
    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }
    public String getSintomas() { return sintomas; }
    public void setSintomas(String sintomas) { this.sintomas = sintomas; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    public String toJson() {
        return "{\"id\":" + id + ",\"ativo\":" + ativo +
               ",\"idPaciente\":" + idPaciente + ",\"idMedico\":" + idMedico +
               ",\"data\":\"" + esc(data) + "\",\"horario\":\"" + esc(horario) +
               "\",\"valor\":" + valor + ",\"sintomas\":\"" + esc(sintomas) +
               "\",\"status\":\"" + esc(status) + "\",\"observacoes\":\"" + esc(observacoes) + "\"}";
    }
    private String esc(String s) { return s == null ? "" : s.replace("\\","\\\\").replace("\"","\\\""); }
}
