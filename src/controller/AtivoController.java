package controller;

import dao.AtivoDAO;
import entidades.Ativo;
import java.io.IOException;
import java.util.List;

public class AtivoController {
    private AtivoDAO dao;

    public AtivoController() throws IOException {
        dao = new AtivoDAO();
    }

    public void cadastrar(String nome, String ticker, String tipo, double precoAtual) throws IOException {
        Ativo a = new Ativo(-1, nome, ticker, tipo, precoAtual);
        int id = dao.create(a);
        System.out.println("Ativo cadastrado com ID: " + id);
    }

    public void buscar(int id) throws IOException {
        Ativo a = dao.read(id);
        if (a != null) {
            System.out.println(a);
        } else {
            System.out.println("Ativo não encontrado.");
        }
    }

    public void atualizar(int id, String nome, String ticker, String tipo, double precoAtual) throws IOException {
        Ativo a = new Ativo(id, nome, ticker, tipo, precoAtual);
        if (dao.update(a)) {
            System.out.println("Ativo atualizado com sucesso.");
        } else {
            System.out.println("Falha ao atualizar ativo.");
        }
    }

    public void excluir(int id) throws IOException {
        if (dao.delete(id)) {
            System.out.println("Ativo excluído com sucesso.");
        } else {
            System.out.println("Falha ao excluir ativo.");
        }
    }

    public void listar() throws IOException {
        List<Ativo> ativos = dao.list();
        if (ativos.isEmpty()) {
            System.out.println("Nenhum ativo cadastrado.");
        } else {
            for (Ativo a : ativos) {
                System.out.println(a);
            }
        }
    }
}
