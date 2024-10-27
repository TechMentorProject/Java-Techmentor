package usecases.projecao_populacional;

import infrastructure.database.BancoOperacoes;
import infrastructure.logging.Logger;
import infrastructure.processing.workbook.ManipularArquivo;
import org.apache.poi.util.IOUtils;

import java.sql.SQLException;
import java.util.List;

public class Main {

    public static void main(String[] args) throws SQLException {
        InserirDados banco = new InserirDados();
        BancoOperacoes bancoDeDados = new BancoOperacoes();
        ManipularArquivo manipularArquivo = new ManipularArquivo();
        Logger loggerEventos = Logger.getLoggerEventos();
        Logger loggerErros = Logger.getLoggerErros();

        try {
            // Aumentando limite de capacidade do apache poi
            IOUtils.setByteArrayMaxOverride(250_000_000);

            String nomeArquivo = "projecoes_2024_tab1_idade_simples.xlsx";
            String caminhoArquivo = "/app/base-dados" + "/" + nomeArquivo;

            List<List<Object>> dados = manipularArquivo.lerPlanilha(caminhoArquivo, false);

            bancoDeDados.conectar();
            banco.inserirDadosComTratamento(dados, bancoDeDados.getConexao(), bancoDeDados);
            loggerEventos.gerarLog("✅ Dados de PROJEÇÃO POPULACIONAL Inseridos com Sucesso! ✅");

        } catch (SQLException | ClassNotFoundException e) {
            System.out.println("Erro: " + e.getMessage());
            loggerErros.gerarLog("❌ Erro ao Inserir Dados de PROJEÇÃO POPULACIONAL. ❌");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            bancoDeDados.fecharConexao();
        }
    }
}