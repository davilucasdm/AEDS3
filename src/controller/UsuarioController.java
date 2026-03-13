package controller;

import dao.UsuarioDAO;
import entidades.Usuario;
import seguranca.Criptografia;
import java.io.IOException;
import java.util.List;

public class UsuarioController {
    private UsuarioDAO dao;

    public UsuarioController() throws IOException {
        dao = new UsuarioDAO();
    }

    public void cadastrar(String nome, String email, String senha) throws IOException {
        String senhaCrip = Criptografia.criptografar(senha);
        Usuario u = new Usuario(-1, nome, email, senhaCrip, System.currentTimeMillis());
        int id = dao.create(u);
        System.out.println("Usuário cadastrado com ID: " + id);
    }

    public Usuario login(String email, String senha) throws IOException {
        Usuario u = dao.buscarPorEmail(email);
        if (u != null) {
            String senhaDigitadaCrip = Criptografia.criptografar(senha);
            if (u.getSenhaCriptografada().equals(senhaDigitadaCrip)) {
                return u;
            }
        }
        return null;
    }

    public boolean cadastrarUsuario(String nome, String email, String senha) throws IOException {
        if (dao.buscarPorEmail(email) != null) {
            return false;
        }
        String senhaCrip = Criptografia.criptografar(senha);
        Usuario u = new Usuario(-1, nome, email, senhaCrip, System.currentTimeMillis());
        dao.create(u);
        return true;
    }

    public void buscar(int id) throws IOException {
        Usuario u = dao.read(id);
        if (u != null) {
            System.out.println(u);
        } else {
            System.out.println("Usuário não encontrado.");
        }
    }

    public void atualizar(int id, String nome, String email, String senha) throws IOException {
        String senhaCrip = Criptografia.criptografar(senha);
        Usuario u = dao.read(id);
        if (u != null) {
            u.setNome(nome);
            u.setEmail(email);
            u.setSenhaCriptografada(senhaCrip);
            if (dao.update(u)) {
                System.out.println("Usuário atualizado com sucesso.");
            } else {
                System.out.println("Falha ao atualizar usuário.");
            }
        } else {
            System.out.println("Usuário não encontrado.");
        }
    }

    public void excluir(int id) throws IOException {
        if (dao.delete(id)) {
            System.out.println("Usuário excluído com sucesso.");
        } else {
            System.out.println("Falha ao excluir usuário.");
        }
    }

    public void listar() throws IOException {
        List<Usuario> usuarios = dao.list();
        if (usuarios.isEmpty()) {
            System.out.println("Nenhum usuário cadastrado.");
        } else {
            for (Usuario u : usuarios) {
                System.out.println(u);
            }
        }
    }
}
