package usecases.censo;

import infrastructure.database.BancoOperacoes;
import infrastructure.logging.Logger;
import infrastructure.processing.workbook.ManipularArquivo;
import org.apache.poi.util.IOUtils;

import java.io.File;
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
            // Aumentando limite de capacidade do Apache POI
            IOUtils.setByteArrayMaxOverride(250_000_000);

            String diretorioBase = "/app/base-dados";
            File pasta = new File(diretorioBase);
            File[] arquivos = pasta.listFiles((dir, nome) -> nome.contains("Território -") && nome.endsWith(".xlsx"));

            if (arquivos != null) {
                bancoDeDados.conectar();
                bancoDeDados.truncarTabela("censoIBGE");

                for (File arquivo : arquivos) {
                    List<List<Object>> dados = manipularArquivo.lerPlanilha(arquivo.toString(), true);
                    System.out.println("Inserindo dados do arquivo: " + arquivo.getName());
                    banco.inserirDados(dados, bancoDeDados.getConexao());
//                    loggerEventos.gerarLog("✅ Dados de CENSO Inseridos com Sucesso! ✅");
                }

                bancoDeDados.fecharConexao();
            }

        } catch (SQLException | ClassNotFoundException e) {
            System.out.println("Erro: " + e.getMessage());
//            loggerErros.gerarLog("❌ Erro ao Inserir Dados de CENSO. ❌");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (bancoDeDados.getConexao() != null && !bancoDeDados.getConexao().isClosed()) {
                bancoDeDados.fecharConexao();
            }
        }
    }
}
