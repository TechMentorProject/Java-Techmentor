import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainExecutor {
    public static void main(String[] args) {

        try {
            System.out.println("Baixando arquivos do S3...");
            infrastructure.s3.BaixarArquivoS3.main(args);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ExecutorService executor = Executors.newFixedThreadPool(1);

        executor.submit(() -> {
            System.out.println("Entrando no censo");
            try {
                usecases.censo.Main.main(args);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        executor.submit(() -> {
            try {
                System.out.println("Entrando nas estações");
                usecases.estacoes_smp.Main.main(args);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        executor.submit(() -> {
            try {
                System.out.println("Entrando no município");
                usecases.municipio.Main.main(args);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        executor.submit(() -> {
            try {
                System.out.println("Entrando na projeção populacional");
                usecases.projecao_populacional.Main.main(args);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        executor.shutdown();  // Finaliza o executor após todas as tarefas serem submetidas
    }
}
