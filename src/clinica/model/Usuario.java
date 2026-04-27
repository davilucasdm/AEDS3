package clinica.model;

public class Usuario {
    private int id;
    private boolean ativo;
    private String login;
    private String senha;   // armazenada criptografada (XOR)
    private String role;    // ADMIN | RECEPCIONISTA

    public Usuario() {}
    public Usuario(int id, boolean ativo, String login, String senha, String role) {
        this.id = id; this.ativo = ativo; this.login = login;
        this.senha = senha; this.role = role;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    @Override public String toString() {
        return "{\"id\":" + id + ",\"login\":\"" + login + "\",\"role\":\"" + role + "\"}";
    }
}
