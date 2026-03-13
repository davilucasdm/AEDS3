//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
package app;

import controller.AtivoController;
import controller.UsuarioController;
import controller.TransacaoController;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        try {
            UsuarioController userController = new UsuarioController();
            AtivoController ativoController = new AtivoController();
            TransacaoController transacaoController = new TransacaoController();
            Scanner sc = new Scanner(System.in);
            int menuPrincipal = -1;

            while (menuPrincipal != 0) {
                System.out.println("\n--- SISTEMA DE GESTÃO ---");
                System.out.println("1 - Gerenciar Usuários");
                System.out.println("2 - Gerenciar Ativos");
                System.out.println("3 - Gerenciar Transações");
                System.out.println("0 - Sair");
                System.out.print("Opção: ");
                menuPrincipal = sc.nextInt();
                sc.nextLine();

                if (menuPrincipal == 1) {
                    exibirMenuUsuarios(sc, userController);
                } else if (menuPrincipal == 2) {
                    exibirMenuAtivos(sc, ativoController);
                } else if (menuPrincipal == 3) {
                    exibirMenuTransacoes(sc, transacaoController);
                }
            }
            sc.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void exibirMenuUsuarios(Scanner sc, UsuarioController controller) throws Exception {
        int opcao = -1;
        while (opcao != 0) {
            System.out.println("\n--- CRUD USUÁRIOS ---");
            System.out.println("1 - Cadastrar");
            System.out.println("2 - Listar");
            System.out.println("3 - Buscar por ID");
            System.out.println("4 - Atualizar");
            System.out.println("5 - Excluir");
            System.out.println("6 - Login");
            System.out.println("0 - Voltar");
            System.out.print("Opção: ");
            opcao = sc.nextInt();
            sc.nextLine();

            switch (opcao) {
                case 1:
                    System.out.print("Nome: ");
                    String nome = sc.nextLine();
                    System.out.print("Email: ");
                    String email = sc.nextLine();
                    System.out.print("Senha: ");
                    String senha = sc.nextLine();
                    controller.cadastrar(nome, email, senha);
                    break;
                case 2:
                    controller.listar();
                    break;
                case 3:
                    System.out.print("ID: ");
                    int idBusca = sc.nextInt();
                    controller.buscar(idBusca);
                    break;
                case 4:
                    System.out.print("ID do usuário a atualizar: ");
                    int idUpdate = sc.nextInt();
                    sc.nextLine();
                    System.out.print("Novo Nome: ");
                    String novoNome = sc.nextLine();
                    System.out.print("Novo Email: ");
                    String novoEmail = sc.nextLine();
                    System.out.print("Nova Senha: ");
                    String novaSenha = sc.nextLine();
                    controller.atualizar(idUpdate, novoNome, novoEmail, novaSenha);
                    break;
                case 5:
                    System.out.print("ID do usuário a excluir: ");
                    int idExcluir = sc.nextInt();
                    controller.excluir(idExcluir);
                    break;
                case 6:
                    System.out.print("Email: ");
                    String emailLogin = sc.nextLine();
                    System.out.print("Senha: ");
                    String senhaLogin = sc.nextLine();
                    controller.login(emailLogin, senhaLogin);
                    break;
            }
        }
    }

    private static void exibirMenuAtivos(Scanner sc, AtivoController controller) throws Exception {
        int opcao = -1;
        while (opcao != 0) {
            System.out.println("\n--- CRUD ATIVOS ---");
            System.out.println("1 - Cadastrar");
            System.out.println("2 - Listar");
            System.out.println("3 - Buscar por ID");
            System.out.println("4 - Atualizar");
            System.out.println("5 - Excluir");
            System.out.println("0 - Voltar");
            System.out.print("Opção: ");
            opcao = sc.nextInt();
            sc.nextLine();

            switch (opcao) {
                case 1:
                    System.out.print("Nome: ");
                    String nome = sc.nextLine();
                    System.out.print("Ticker: ");
                    String ticker = sc.nextLine();
                    System.out.print("Tipo: ");
                    String tipo = sc.nextLine();
                    System.out.print("Preço Atual: ");
                    double preco = sc.nextDouble();
                    controller.cadastrar(nome, ticker, tipo, preco);
                    break;
                case 2:
                    controller.listar();
                    break;
                case 3:
                    System.out.print("ID: ");
                    int idBusca = sc.nextInt();
                    controller.buscar(idBusca);
                    break;
                case 4:
                    System.out.print("ID do ativo a atualizar: ");
                    int idUpdate = sc.nextInt();
                    sc.nextLine();
                    System.out.print("Novo Nome: ");
                    String novoNome = sc.nextLine();
                    System.out.print("Novo Ticker: ");
                    String novoTicker = sc.nextLine();
                    System.out.print("Novo Tipo: ");
                    String novoTipo = sc.nextLine();
                    System.out.print("Novo Preço: ");
                    double novoPreco = sc.nextDouble();
                    controller.atualizar(idUpdate, novoNome, novoTicker, novoTipo, novoPreco);
                    break;
                case 5:
                    System.out.print("ID do ativo a excluir: ");
                    int idExcluir = sc.nextInt();
                    controller.excluir(idExcluir);
                    break;
            }
        }
    }

    private static void exibirMenuTransacoes(Scanner sc, TransacaoController controller) throws Exception {
        int opcao = -1;
        while (opcao != 0) {
            System.out.println("\n--- GESTÃO DE TRANSAÇÕES ---");
            System.out.println("1 - Registrar Compra");
            System.out.println("2 - Registrar Venda");
            System.out.println("3 - Listar Todas");
            System.out.println("4 - Listar por Usuário");
            System.out.println("0 - Voltar");
            System.out.print("Opção: ");
            opcao = sc.nextInt();
            sc.nextLine();

            switch (opcao) {
                case 1:
                    System.out.print("ID Usuário: ");
                    int idUComp = sc.nextInt();
                    System.out.print("ID Ativo: ");
                    int idAComp = sc.nextInt();
                    System.out.print("Quantidade: ");
                    int qtdComp = sc.nextInt();
                    System.out.print("Preço Unitário: ");
                    double precoComp = sc.nextDouble();
                    controller.registrarCompra(idUComp, idAComp, qtdComp, precoComp);
                    break;
                case 2:
                    System.out.print("ID Usuário: ");
                    int idUVend = sc.nextInt();
                    System.out.print("ID Ativo: ");
                    int idAVend = sc.nextInt();
                    System.out.print("Quantidade: ");
                    int qtdVend = sc.nextInt();
                    System.out.print("Preço Unitário: ");
                    double precoVend = sc.nextDouble();
                    controller.registrarVenda(idUVend, idAVend, qtdVend, precoVend);
                    break;
                case 3:
                    controller.listarTransacoes();
                    break;
                case 4:
                    System.out.print("ID Usuário: ");
                    int idULis = sc.nextInt();
                    controller.listarTransacoesUsuario(idULis);
                    break;
            }
        }
    }
}