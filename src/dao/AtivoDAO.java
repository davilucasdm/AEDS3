package dao;

import entidades.Ativo;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AtivoDAO {
    private RandomAccessFile arq;
    private final String PATH = "dados/ativos.db";

    public AtivoDAO() throws IOException {
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

    public int create(Ativo a) throws IOException {
        arq.seek(0);
        int ultimoID = arq.readInt();
        int novoID = ultimoID + 1;
        a.setId(novoID);

        // Atualiza cabeçalho
        arq.seek(0);
        arq.writeInt(novoID);

        // Escreve registro no fim do arquivo
        arq.seek(arq.length());
        
        byte[] dados = a.toByteArray();
        
        arq.writeBoolean(false); // Lápide
        arq.writeInt(dados.length); // Tamanho do registro
        arq.write(dados); // Dados serializados

        return novoID;
    }

    public Ativo read(int id) throws IOException {
        arq.seek(4); // Pula cabeçalho
        while (arq.getFilePointer() < arq.length()) {
            boolean lapide = arq.readBoolean();
            int tamanho = arq.readInt();
            if (!lapide) {
                byte[] b = new byte[tamanho];
                arq.read(b);
                Ativo a = new Ativo();
                a.fromByteArray(b);
                if (a.getId() == id) {
                    return a;
                }
            } else {
                arq.skipBytes(tamanho);
            }
        }
        return null;
    }

    public boolean update(Ativo a) throws IOException {
        arq.seek(4);
        while (arq.getFilePointer() < arq.length()) {
            long pos = arq.getFilePointer();
            boolean lapide = arq.readBoolean();
            int tamanho = arq.readInt();
            if (!lapide) {
                byte[] b = new byte[tamanho];
                arq.read(b);
                Ativo temp = new Ativo();
                temp.fromByteArray(b);
                if (temp.getId() == a.getId()) {
                    byte[] novosDados = a.toByteArray();
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

    public boolean delete(int id) throws IOException {
        arq.seek(4);
        while (arq.getFilePointer() < arq.length()) {
            long pos = arq.getFilePointer();
            boolean lapide = arq.readBoolean();
            int tamanho = arq.readInt();
            if (!lapide) {
                byte[] b = new byte[tamanho];
                arq.read(b);
                Ativo a = new Ativo();
                a.fromByteArray(b);
                if (a.getId() == id) {
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

    public List<Ativo> list() throws IOException {
        List<Ativo> lista = new ArrayList<>();
        arq.seek(4);
        while (arq.getFilePointer() < arq.length()) {
            boolean lapide = arq.readBoolean();
            int tamanho = arq.readInt();
            if (!lapide) {
                byte[] b = new byte[tamanho];
                arq.read(b);
                Ativo a = new Ativo();
                a.fromByteArray(b);
                lista.add(a);
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
