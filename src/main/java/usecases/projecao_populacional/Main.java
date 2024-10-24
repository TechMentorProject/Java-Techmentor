package usecases.projecao_populacional;

import infrastructure.database.BancoOperacoes;
import infrastructure.processing.workbook.ManipularArquivo;
import org.apache.poi.util.IOUtils;

import java.sql.SQLException;
import java.util.List;

public class Main {

    public static void main(String[] args) throws SQLException {

        InserirDados banco = new InserirDados();
        BancoOperacoes bancoDeDados = new BancoOperacoes();
        ManipularArquivo manipularArquivo = new ManipularArquivo();

        try {
            // Aumentando limite de capacidade do apache poi
            IOUtils.setByteArrayMaxOverride(250_000_000);

            String caminhoArquivo = "src/main/java/resources/projecoes_2024_tab1_idade_simples (1).xlsx";

            List<List<Object>> dados = manipularArquivo.lerPlanilha(caminhoArquivo);

            bancoDeDados.conectar();
            banco.inserirDadosComTratamento(dados, bancoDeDados.getConexao(), bancoDeDados);

        } catch (SQLException | ClassNotFoundException e) {
            System.out.println("Erro: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            bancoDeDados.fecharConexao();
        }
    }
}