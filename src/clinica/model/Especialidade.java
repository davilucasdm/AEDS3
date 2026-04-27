package clinica.model;

public class Especialidade {
    private int id;
    private boolean ativo;
    private String nome;
    private String descricao;

    public Especialidade() {}
    public Especialidade(int id, boolean ativo, String nome, String descricao) {
        this.id = id; this.ativo = ativo; this.nome = nome; this.descricao = descricao;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String toJson() {
        return "{\"id\":" + id + ",\"ativo\":" + ativo +
               ",\"nome\":\"" + esc(nome) + "\",\"descricao\":\"" + esc(descricao) + "\"}";
    }
    private String esc(String s) { return s == null ? "" : s.replace("\\","\\\\").replace("\"","\\\""); }
}
