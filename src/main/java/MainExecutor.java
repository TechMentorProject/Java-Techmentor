import infrastructure.database.BancoOperacoes;
import infrastructure.database.BancoSetup;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

public class MainExecutor {
    public static void main(String[] args) {
        BancoOperacoes bancoOperacoes = new BancoOperacoes();
        // Conecta ao banco e cria a estrutura
        try {
            bancoOperacoes.conectar();
            Connection conexao = bancoOperacoes.getConexao();
            BancoSetup bancoSetup = new BancoSetup(conexao);
            bancoSetup.criarEstruturaBanco();
            System.out.println("Estrutura de banco de dados verificada e criada se necessário.");
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("Erro ao configurar o banco de dados: " + e.getMessage());
            e.printStackTrace();
            return;  // Encerra se falhar a criação do banco/tabelas
        }

        // Tarefa para baixar arquivo do S3
        try {
            infrastructure.s3.BaixarArquivoS3.main(args);  // Executa a primeira main e aguarda a finalização
            System.out.println("Download do arquivo S3 concluído com sucesso.");
        } catch (IOException e) {
            System.err.println("Falha ao baixar o arquivo S3: " + e.getMessage());
            e.printStackTrace();
            return;  // Encerra o programa se o download falhar
        }

        // Executa cada tarefa de forma sequencial
        executarTarefa("Estações", args, usecases.estacoes_smp.Main::main);
        executarTarefa("Censo", args, usecases.censo.Main::main);
        executarTarefa("Município", args, usecases.municipio.Main::main);
        executarTarefa("Projeção Populacional", args, usecases.projecao_populacional.Main::main);
    }

    private static void executarTarefa(String nomeTarefa, String[] args, MainRunnable main) {
        System.out.println("Entrando no módulo: " + nomeTarefa);
        try {
            main.run(args);  // Executa a tarefa
            System.out.println("Módulo concluído com sucesso: " + nomeTarefa);
        } catch (Exception e) {
            System.err.println("Erro no módulo " + nomeTarefa + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FunctionalInterface
    private interface MainRunnable {
        void run(String[] args) throws Exception;  // Captura SQLException, IOException, etc.
    }
}
