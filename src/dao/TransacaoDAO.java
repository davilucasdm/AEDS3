package dao;

import entidades.Transacao;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TransacaoDAO {
    private RandomAccessFile arq;
    private final String PATH = "dados/transacoes.db";

    public TransacaoDAO() throws IOException {
        File folder = new File("dados");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File file = new File(PATH);
        arq = new RandomAccessFile(file, "rw");
        if (arq.length() == 0) {
            arq.writeInt(0); // Cabeçalho com o último ID utilizado
        }
    }

    public int criar(Transacao t) throws IOException {
        arq.seek(0);
        int ultimoID = arq.readInt();
        int novoID = ultimoID + 1;
        t.setId(novoID);

        // Atualiza cabeçalho
        arq.seek(0);
        arq.writeInt(novoID);

        // Escreve registro no fim do arquivo
        arq.seek(arq.length());
        
        byte[] dados = t.toByteArray();
        
        arq.writeBoolean(false); // Lápide
        arq.writeInt(dados.length); // Tamanho do registro
        arq.write(dados); // Dados serializados

        return novoID;
    }

    public Transacao buscarPorId(int id) throws IOException {
        arq.seek(4); // Pula cabeçalho
        while (arq.getFilePointer() < arq.length()) {
            boolean lapide = arq.readBoolean();
            int tamanho = arq.readInt();
            if (!lapide) {
                byte[] b = new byte[tamanho];
                arq.read(b);
                Transacao t = new Transacao();
                t.fromByteArray(b);
                if (t.getId() == id) {
                    return t;
                }
            } else {
                arq.skipBytes(tamanho);
            }
        }
        return null;
    }

    public boolean atualizar(Transacao t) throws IOException {
        arq.seek(4);
        while (arq.getFilePointer() < arq.length()) {
            long pos = arq.getFilePointer();
            boolean lapide = arq.readBoolean();
            int tamanho = arq.readInt();
            if (!lapide) {
                byte[] b = new byte[tamanho];
                arq.read(b);
                Transacao temp = new Transacao();
                temp.fromByteArray(b);
                if (temp.getId() == t.getId()) {
                    byte[] novosDados = t.toByteArray();
                    if (novosDados.length <= tamanho) {
                        // Sobrescreve no mesmo lugar
                        arq.seek(pos + 1 + 4); // Lápide + Tamanho
                        arq.write(novosDados);
                    } else {
                        // Marca como excluído e escreve no fim
                        arq.seek(pos);
                        arq.writeBoolean(true); // Lápide
                        
                        arq.seek(arq.length());
                        arq.writeBoolean(false);
                        arq.writeInt(novosDados.length);
                        arq.write(novosDados);
                    }
                    return true;
                }
            } else {
                arq.skipBytes(tamanho);
            }
        }
        return false;
    }

    public boolean excluir(int id) throws IOException {
        arq.seek(4);
        while (arq.getFilePointer() < arq.length()) {
            long pos = arq.getFilePointer();
            boolean lapide = arq.readBoolean();
            int tamanho = arq.readInt();
            if (!lapide) {
                byte[] b = new byte[tamanho];
                arq.read(b);
                Transacao t = new Transacao();
                t.fromByteArray(b);
                if (t.getId() == id) {
                    arq.seek(pos);
                    arq.writeBoolean(true); // Marca lápide como true
                    return true;
                }
            } else {
                arq.skipBytes(tamanho);
            }
        }
        return false;
    }

    public List<Transacao> listarTodas() throws IOException {
        List<Transacao> lista = new ArrayList<>();
        arq.seek(4);
        while (arq.getFilePointer() < arq.length()) {
            boolean lapide = arq.readBoolean();
            int tamanho = arq.readInt();
            if (!lapide) {
                byte[] b = new byte[tamanho];
                arq.read(b);
                Transacao t = new Transacao();
                t.fromByteArray(b);
                lista.add(t);
            } else {
                arq.skipBytes(tamanho);
            }
        }
        return lista;
    }

    public void close() throws IOException {
        arq.close();
    }
}
