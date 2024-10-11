package municipio;

import geral.ManipularArquivo;
import org.apache.poi.util.IOUtils;

import java.sql.SQLException;
import java.util.List;

public class Main {

    public static void main(String[] args) throws SQLException {

        BancoDeDados banco = new BancoDeDados();
        ManipularArquivo manipularArquivo = new ManipularArquivo();

        try {
            // Aumentando limite de capacidade do apache poi
            IOUtils.setByteArrayMaxOverride(250_000_000);

            String caminhoArquivo = "./base de dados/Meu_Municipio_Cobertura.xlsx";

            List<List<Object>> dados = manipularArquivo.lerPlanilha(caminhoArquivo);

            banco.conectar();
            banco.inserirDadosComTratamento(dados);

        } catch (SQLException | ClassNotFoundException e) {
            System.out.println("Erro: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            banco.fecharConexao();
        }
    }
}