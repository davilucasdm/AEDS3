package clinica.model;

public class Usuario {

    private int id;
    private boolean ativo;
    private String login;
    private String senha;   // armazenada cifrada (XOR + Base64)
    private String role;    // ADMIN | RECEPCIONISTA | MEDICO

    public Usuario() {
    }

    public Usuario(int id, boolean ativo, String login, String senha, String role) {
        this.id = id;
        this.ativo = ativo;
        this.login = login;
        this.senha = senha;
        this.role = role;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return "{\"id\":" + id + ",\"ativo\":" + ativo
                + ",\"login\":\"" + esc(login) + "\",\"role\":\"" + esc(role) + "\"}";
    }

    private String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
