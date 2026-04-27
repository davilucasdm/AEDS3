package clinica.model;

public class Medico {
    private int id;
    private boolean ativo;
    private String nome;
    private String crm;
    private String email;
    private String telefones;   // multivalorado: separado por ";"

    public Medico() {}
    public Medico(int id, boolean ativo, String nome, String crm, String email, String telefones) {
        this.id = id; this.ativo = ativo; this.nome = nome;
        this.crm = crm; this.email = email; this.telefones = telefones;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCrm() { return crm; }
    public void setCrm(String crm) { this.crm = crm; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelefones() { return telefones; }
    public void setTelefones(String t) { this.telefones = t; }

    public String toJson() {
        return "{\"id\":" + id + ",\"ativo\":" + ativo +
               ",\"nome\":\"" + esc(nome) + "\",\"crm\":\"" + esc(crm) +
               "\",\"email\":\"" + esc(email) + "\",\"telefones\":\"" + esc(telefones) + "\"}";
    }
    private String esc(String s) { return s == null ? "" : s.replace("\\","\\\\").replace("\"","\\\""); }
}
