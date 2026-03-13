package entidades;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Usuario {
    private int id;
    private String nome;
    private String email;
    private String senhaCriptografada;
    private long criadoEm;

    public Usuario() {
        this.id = -1;
        this.nome = "";
        this.email = "";
        this.senhaCriptografada = "";
        this.criadoEm = 0;
    }

    public Usuario(int id, String nome, String email, String senhaCriptografada, long criadoEm) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.senhaCriptografada = senhaCriptografada;
        this.criadoEm = criadoEm;
    }

    // Getters e Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSenhaCriptografada() { return senhaCriptografada; }
    public void setSenhaCriptografada(String senhaCriptografada) { this.senhaCriptografada = senhaCriptografada; }

    public long getCriadoEm() { return criadoEm; }
    public void setCriadoEm(long criadoEm) { this.criadoEm = criadoEm; }

    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(id);
        dos.writeUTF(nome);
        dos.writeUTF(email);
        dos.writeUTF(senhaCriptografada);
        dos.writeLong(criadoEm);
        return baos.toByteArray();
    }

    public void fromByteArray(byte[] b) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(b);
        DataInputStream dis = new DataInputStream(bais);
        id = dis.readInt();
        nome = dis.readUTF();
        email = dis.readUTF();
        senhaCriptografada = dis.readUTF();
        criadoEm = dis.readLong();
    }

    @Override
    public String toString() {
        return String.format("ID: %d | Nome: %s | Email: %s | Criado em: %tF %tT", 
                             id, nome, email, criadoEm, criadoEm);
    }
}
