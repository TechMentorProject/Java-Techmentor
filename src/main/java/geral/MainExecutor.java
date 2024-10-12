package geral;
import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainExecutor {
    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(1);

//        executor.submit(() -> {
//            try {
//                geral.BaixarArquivoS3.main(args); // Executa a primeira main
//            } catch (IOException e) { // Trata ambas as exceções
//                // Aqui você pode tratar a exceção, como logar o erro
//                e.printStackTrace(); // Para imprimir o stack trace
//            }
//        });

        executor.submit(() -> {
            try {
                censo.Main.main(args); // Executa a primeira main
            } catch (SQLException e) {
                // Aqui você pode tratar a exceção, como logar o erro
                e.printStackTrace(); // Para imprimir o stack trace
            }
        });

        executor.submit(() -> {
            try {
                estacoes_smp.Main.main(args); // Executa a primeira main
            } catch (SQLException e) {
                // Aqui você pode tratar a exceção, como logar o erro
                e.printStackTrace(); // Para imprimir o stack trace
            }
        });

        executor.submit(() -> {
            try {
                municipio.Main.main(args); // Executa a primeira main
            } catch (SQLException e) {
                // Aqui você pode tratar a exceção, como logar o erro
                e.printStackTrace(); // Para imprimir o stack trace
            }
        });
        executor.submit(() -> {
            try {
                projecao_populacional.Main.main(args); // Executa a primeira main
            } catch (SQLException e) {
                // Aqui você pode tratar a exceção, como logar o erro
                e.printStackTrace(); // Para imprimir o stack trace
            }
        });

        executor.shutdown();
    }
}

