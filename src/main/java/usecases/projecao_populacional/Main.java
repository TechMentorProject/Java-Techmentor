package usecases.projecao_populacional;

import infrastructure.database.BancoOperacoes;
import infrastructure.processing.workbook.ManipularArquivo;
import org.apache.poi.util.IOUtils;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;

public class Main {

    public static void main(String[] args) throws SQLException {
        InserirDados banco = new InserirDados();
        BancoOperacoes bancoDeDados = new BancoOperacoes();
        ManipularArquivo manipularArquivo = new ManipularArquivo();

        try {
            // Aumentando limite de capacidade do apache poi
            IOUtils.setByteArrayMaxOverride(250_000_000);

            String caminhoBase = "/app/base-dados";

            File diretorio = new File(caminhoBase);
            Pattern padraoArquivo = Pattern.compile("projecoes_\\d{4}_tab1_idade_simples\\.xlsx");

            // Busca um arquivo que corresponda ao padrão no diretório
            String nomeArquivo = null;
            for (File arquivo : diretorio.listFiles()) {
                if (padraoArquivo.matcher(arquivo.getName()).matches()) {
                    nomeArquivo = arquivo.getName();
                    break;
                }
            }

            if (nomeArquivo == null) {
                throw new RuntimeException("Nenhum arquivo encontrado com o padrão especificado.");
            }
            String caminhoArquivo = caminhoBase + "/" + nomeArquivo;

            List<List<Object>> dados = manipularArquivo.lerPlanilha(caminhoArquivo, true);

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