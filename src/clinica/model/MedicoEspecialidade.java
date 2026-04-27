package clinica.model;

public class MedicoEspecialidade {
    private int id;
    private boolean ativo;
    private int idMedico;
    private int idEspecialidade;

    public MedicoEspecialidade() {}
    public MedicoEspecialidade(int id, boolean ativo, int idMedico, int idEspecialidade) {
        this.id = id; this.ativo = ativo;
        this.idMedico = idMedico; this.idEspecialidade = idEspecialidade;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public int getIdMedico() { return idMedico; }
    public void setIdMedico(int idMedico) { this.idMedico = idMedico; }
    public int getIdEspecialidade() { return idEspecialidade; }
    public void setIdEspecialidade(int idEspecialidade) { this.idEspecialidade = idEspecialidade; }

    public String toJson() {
        return "{\"id\":" + id + ",\"ativo\":" + ativo +
               ",\"idMedico\":" + idMedico + ",\"idEspecialidade\":" + idEspecialidade + "}";
    }
}
