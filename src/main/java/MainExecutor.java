import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainExecutor {
    public static void main(String[] args) {
        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        // Tarefa para baixar arquivo do S3
        try {
            infrastructure.s3.BaixarArquivoS3.main(args);  // Executa a primeira main e aguarda a finalização
            System.out.println("Download do arquivo S3 concluído com sucesso.");
        } catch (IOException e) {
            System.err.println("Falha ao baixar o arquivo S3: " + e.getMessage());
            e.printStackTrace();
            executor.shutdown();  // Finaliza o executor se o download falhar
            return;  // Encerra o programa se o download falhar
        }

        // Outras tarefas
        executor.submit(() -> executarTarefa("Censo", args, usecases.censo.Main::main));
        executor.submit(() -> executarTarefa("Estações", args, usecases.estacoes_smp.Main::main));
        executor.submit(() -> executarTarefa("Município", args, usecases.municipio.Main::main));
        executor.submit(() -> executarTarefa("Projeção Populacional", args, usecases.projecao_populacional.Main::main));

        executor.shutdown();
        try {

            if (!executor.awaitTermination(460, TimeUnit.SECONDS)) {
                System.err.println("Algumas tarefas não terminaram no tempo esperado e serão forçadas a encerrar.");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            System.err.println("Executor interrompido: " + e.getMessage());
            executor.shutdownNow();
        }
    }

    private static void executarTarefa(String nomeTarefa, String[] args, MainRunnable main) {
        System.out.println("Entrando no módulo: " + nomeTarefa);
        try {
            main.run(args);
            System.out.println("Módulo concluído com sucesso: " + nomeTarefa);
        } catch (Exception e) {
            System.err.println("Erro no módulo " + nomeTarefa + ": " + e.getMessage());
        }
    }

    @FunctionalInterface
    private interface MainRunnable {
        void run(String[] args) throws Exception;  // Captura SQLException, IOException, etc.
    }
}