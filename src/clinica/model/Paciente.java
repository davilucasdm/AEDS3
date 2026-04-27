package clinica.model;

public class Paciente {
    private int id;
    private boolean ativo;
    private String nome;
    private String cpf;
    private String dataNascimento;   // yyyy-MM-dd
    private String email;
    private String telefones;        // multivalorado: separado por ";"
    private String endereco;

    public Paciente() {}
    public Paciente(int id, boolean ativo, String nome, String cpf, String dataNascimento,
                    String email, String telefones, String endereco) {
        this.id = id; this.ativo = ativo; this.nome = nome; this.cpf = cpf;
        this.dataNascimento = dataNascimento; this.email = email;
        this.telefones = telefones; this.endereco = endereco;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public String getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(String d) { this.dataNascimento = d; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelefones() { return telefones; }
    public void setTelefones(String telefones) { this.telefones = telefones; }
    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }

    public String toJson() {
        return "{\"id\":" + id + ",\"ativo\":" + ativo +
               ",\"nome\":\"" + esc(nome) + "\",\"cpf\":\"" + esc(cpf) +
               "\",\"dataNascimento\":\"" + esc(dataNascimento) +
               "\",\"email\":\"" + esc(email) + "\",\"telefones\":\"" + esc(telefones) +
               "\",\"endereco\":\"" + esc(endereco) + "\"}";
    }
    private String esc(String s) { return s == null ? "" : s.replace("\\","\\\\").replace("\"","\\\""); }
}
