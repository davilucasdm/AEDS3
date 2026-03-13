package controller;

import dao.TransacaoDAO;
import entidades.Transacao;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TransacaoController {
    private TransacaoDAO dao;

    public TransacaoController() throws IOException {
        dao = new TransacaoDAO();
    }

    public void registrarCompra(int idUsuario, int idAtivo, int quantidade, double precoUnitario) throws IOException {
        Transacao t = new Transacao(-1, idUsuario, idAtivo, quantidade, precoUnitario, "COMPRA", System.currentTimeMillis());
        int id = dao.criar(t);
        System.out.println("Compra registrada com ID: " + id);
    }

    public void registrarVenda(int idUsuario, int idAtivo, int quantidade, double precoUnitario) throws IOException {
        Transacao t = new Transacao(-1, idUsuario, idAtivo, quantidade, precoUnitario, "VENDA", System.currentTimeMillis());
        int id = dao.criar(t);
        System.out.println("Venda registrada com ID: " + id);
    }

    public List<Transacao> obterTransacoesUsuario(int idUsuario) throws IOException {
        List<Transacao> todas = dao.listarTodas();
        List<Transacao> doUsuario = new ArrayList<>();
        for (Transacao t : todas) {
            if (t.getIdUsuario() == idUsuario) {
                doUsuario.add(t);
            }
        }
        return doUsuario;
    }

    public void listarTransacoesUsuario(int idUsuario) throws IOException {
        List<Transacao> todas = dao.listarTodas();
        List<Transacao> doUsuario = new ArrayList<>();
        for (Transacao t : todas) {
            if (t.getIdUsuario() == idUsuario) {
                doUsuario.add(t);
            }
        }
        
        if (doUsuario.isEmpty()) {
            System.out.println("Nenhuma transação encontrada para o usuário ID: " + idUsuario);
        } else {
            for (Transacao t : doUsuario) {
                System.out.println(t);
            }
        }
    }

    public void listarTransacoes() throws IOException {
        List<Transacao> transacoes = dao.listarTodas();
        if (transacoes.isEmpty()) {
            System.out.println("Nenhuma transação registrada.");
        } else {
            for (Transacao t : transacoes) {
                System.out.println(t);
            }
        }
    }
}
