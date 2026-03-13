package dao;

import entidades.Usuario;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {
    private RandomAccessFile arq;
    private final String PATH = "dados\\usuarios.db";

    public UsuarioDAO() throws IOException {
        File folder = new File("dados");
        if (!folder.exists()) folder.mkdir();
        File file = new File(PATH);
        arq = new RandomAccessFile(file, "rw");
        if (arq.length() == 0) {
            arq.writeInt(0); // Cabeçalho com o último ID utilizado
        }
    }

    public int create(Usuario u) throws IOException {
        arq.seek(0);
        int ultimoID = arq.readInt();
        int novoID = ultimoID + 1;
        u.setId(novoID);

        // Atualiza cabeçalho
        arq.seek(0);
        arq.writeInt(novoID);

        // Escreve registro no fim do arquivo
        arq.seek(arq.length());
        
        byte[] dados = u.toByteArray();
        
        arq.writeBoolean(false); // Lápide
        arq.writeInt(dados.length); // Tamanho do registro
        arq.write(dados); // Dados serializados

        return novoID;
    }

    public Usuario read(int id) throws IOException {
        arq.seek(4); // Pula cabeçalho
        while (arq.getFilePointer() < arq.length()) {
            boolean lapide = arq.readBoolean();
            int tamanho = arq.readInt();
            if (!registroCabeNoArquivo(tamanho)) break;
            if (!lapide) {
                byte[] b = new byte[tamanho];
                arq.readFully(b);
                Usuario u = new Usuario();
                u.fromByteArray(b);
                if (u.getId() == id) {
                    return u;
                }
            } else {
                arq.skipBytes(tamanho);
            }
        }
        return null;
    }

    public boolean update(Usuario u) throws IOException {
        arq.seek(4);
        while (arq.getFilePointer() < arq.length()) {
            long pos = arq.getFilePointer();
            boolean lapide = arq.readBoolean();
            int tamanho = arq.readInt();
            if (!registroCabeNoArquivo(tamanho)) break;
            if (!lapide) {
                byte[] b = new byte[tamanho];
                arq.readFully(b);
                Usuario temp = new Usuario();
                temp.fromByteArray(b);
                if (temp.getId() == u.getId()) {
                    byte[] novosDados = u.toByteArray();
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
            if (!registroCabeNoArquivo(tamanho)) break;
            if (!lapide) {
                byte[] b = new byte[tamanho];
                arq.readFully(b);
                Usuario u = new Usuario();
                u.fromByteArray(b);
                if (u.getId() == id) {
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

    public List<Usuario> list() throws IOException {
        List<Usuario> lista = new ArrayList<>();
        arq.seek(4);
        while (arq.getFilePointer() < arq.length()) {
            boolean lapide = arq.readBoolean();
            int tamanho = arq.readInt();
            if (!registroCabeNoArquivo(tamanho)) break;
            if (!lapide) {
                byte[] b = new byte[tamanho];
                arq.readFully(b);
                Usuario u = new Usuario();
                u.fromByteArray(b);
                lista.add(u);
            } else {
                arq.skipBytes(tamanho);
            }
        }
        return lista;
    }

    public Usuario buscarPorEmail(String email) throws IOException {
        arq.seek(4);
        while (arq.getFilePointer() < arq.length()) {
            boolean lapide = arq.readBoolean();
            int tamanho = arq.readInt();
            if (!registroCabeNoArquivo(tamanho)) break;
            if (!lapide) {
                byte[] b = new byte[tamanho];
                arq.readFully(b);
                Usuario u = new Usuario();
                u.fromByteArray(b);
                if (u.getEmail().equals(email)) {
                    return u;
                }
            } else {
                arq.skipBytes(tamanho);
            }
        }
        return null;
    }

    private boolean registroCabeNoArquivo(int tamanho) throws IOException {
        if (tamanho < 0) return false;
        long restantes = arq.length() - arq.getFilePointer();
        return tamanho <= restantes;
    }

    public void close() throws IOException {
        arq.close();
    }
}
