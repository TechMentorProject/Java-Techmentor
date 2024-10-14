import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainExecutor {
    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(1);

//        executor.submit(() -> {
//            try {
//                geral.infraestructureS3.BaixarArquivoS3.main(args); // Executa a primeira main
//            } catch (IOException e) { // Trata ambas as exceções
//                // Aqui você pode tratar a exceção, como logar o erro
//                e.printStackTrace(); // Para imprimir o stack trace
//            }
//        });

        executor.submit(() -> {
            try {
                usecases.censo.Main.main(args);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        executor.submit(() -> {
            try {
                usecases.estacoes_smp.Main.main(args);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        executor.submit(() -> {
            try {
                usecases.municipio.Main.main(args);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        executor.submit(() -> {
            try {
                usecases.projecao_populacional.Main.main(args);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        executor.shutdown();
    }
}

