package app;

public class Main {
    public static void main(String[] args) {
        try {
            ServidorWeb.iniciar();
        } catch (Exception e) {
            System.err.println("Erro fatal ao iniciar o sistema: " + e.getMessage());
            e.printStackTrace();
        }
    }
}