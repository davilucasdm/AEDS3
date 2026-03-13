package entidades;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Transacao {
    private int id;
    private int idUsuario;
    private int idAtivo;
    private int quantidade;
    private double precoUnitario;
    private String tipo; // COMPRA ou VENDA
    private long timestamp;

    public Transacao() {
        this.id = -1;
        this.idUsuario = -1;
        this.idAtivo = -1;
        this.quantidade = 0;
        this.precoUnitario = 0.0;
        this.tipo = "";
        this.timestamp = 0L;
    }

    public Transacao(int id, int idUsuario, int idAtivo, int quantidade, double precoUnitario, String tipo, long timestamp) {
        this.id = id;
        this.idUsuario = idUsuario;
        this.idAtivo = idAtivo;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
        this.tipo = tipo;
        this.timestamp = timestamp;
    }

    // Getters e Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    public int getIdAtivo() { return idAtivo; }
    public void setIdAtivo(int idAtivo) { this.idAtivo = idAtivo; }

    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }

    public double getPrecoUnitario() { return precoUnitario; }
    public void setPrecoUnitario(double precoUnitario) { this.precoUnitario = precoUnitario; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(id);
        dos.writeInt(idUsuario);
        dos.writeInt(idAtivo);
        dos.writeInt(quantidade);
        dos.writeDouble(precoUnitario);
        dos.writeUTF(tipo);
        dos.writeLong(timestamp);
        return baos.toByteArray();
    }

    public void fromByteArray(byte[] b) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(b);
        DataInputStream dis = new DataInputStream(bais);
        id = dis.readInt();
        idUsuario = dis.readInt();
        idAtivo = dis.readInt();
        quantidade = dis.readInt();
        precoUnitario = dis.readDouble();
        tipo = dis.readUTF();
        timestamp = dis.readLong();
    }

    @Override
    public String toString() {
        return String.format("ID: %d | Usuário: %d | Ativo: %d | Quantidade: %d | Preço: R$ %.2f | Tipo: %s | Data: %d",
                id, idUsuario, idAtivo, quantidade, precoUnitario, tipo, timestamp);
    }
}
