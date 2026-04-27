package clinica;

import clinica.controller.ClinicaHttpServer;
import clinica.service.ClinicaService;

public class Main {

    public static void main(String[] args) throws Exception {
        // Diretório de dados (arquivos binários) e web (frontend)
        String dataDir = System.getProperty("dataDir", "data");
        String webDir  = System.getProperty("webDir",  "web");
        int    porta   = Integer.parseInt(System.getProperty("porta", "8080"));

        System.out.println("=== Clínica Médica - AED III ===");
        System.out.println("Data dir : " + dataDir);
        System.out.println("Web  dir : " + webDir);
        System.out.println("Porta    : " + porta);

        ClinicaService    service = new ClinicaService(dataDir);
        ClinicaHttpServer server  = new ClinicaHttpServer(service, webDir);

        server.iniciar(porta);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Encerrando servidor...");
            server.parar();
        }));

        // Mantém JVM viva
        Thread.currentThread().join();
    }
}
