package entidades;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Ativo {
    private int id;
    private String nome;
    private String ticker;
    private String tipo;
    private double precoAtual;

    public Ativo() {
        this.id = -1;
        this.nome = "";
        this.ticker = "";
        this.tipo = "";
        this.precoAtual = 0.0;
    }

    public Ativo(int id, String nome, String ticker, String tipo, double precoAtual) {
        this.id = id;
        this.nome = nome;
        this.ticker = ticker;
        this.tipo = tipo;
        this.precoAtual = precoAtual;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public double getPrecoAtual() { return precoAtual; }
    public void setPrecoAtual(double precoAtual) { this.precoAtual = precoAtual; }

    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(id);
        dos.writeUTF(nome);
        dos.writeUTF(ticker);
        dos.writeUTF(tipo);
        dos.writeDouble(precoAtual);
        return baos.toByteArray();
    }

    public void fromByteArray(byte[] b) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(b);
        DataInputStream dis = new DataInputStream(bais);
        id = dis.readInt();
        nome = dis.readUTF();
        ticker = dis.readUTF();
        tipo = dis.readUTF();
        precoAtual = dis.readDouble();
    }

    @Override
    public String toString() {
        return String.format("ID: %d | Nome: %s | Ticker: %s | Tipo: %s | Preço Atual: R$ %.2f",
                id, nome, ticker, tipo, precoAtual);
    }
}
