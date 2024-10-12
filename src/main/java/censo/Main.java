package censo;

import geral.ManipularArquivo;
import org.apache.poi.util.IOUtils;

import java.io.File;
import java.sql.SQLException;
import java.util.List;

public class Main {

    public static void main(String[] args) throws SQLException {

        BancoDeDados banco = new BancoDeDados();
        ManipularArquivo manipularArquivo = new ManipularArquivo();

        try {
            // Aumentando limite de capacidade do apache poi
            IOUtils.setByteArrayMaxOverride(250_000_000);

            String diretorioBase = "./base de dados";
            File pasta = new File(diretorioBase);
            File[] arquivos = pasta.listFiles((dir, nome) -> nome.contains("Território -") && nome.endsWith(".xlsx"));

            if(arquivos != null) {
                banco.conectar();
                banco.truncarTabela();
                banco.fecharConexao();
                for (File arquivo : arquivos) {
                    List<List<Object>> dados = manipularArquivo.lerPlanilha(arquivo.toString());
                    System.out.println("Inserindo dados de novo");
                    banco.conectar();
                    banco.inserirDados(dados);
                }
            }

        } catch (SQLException | ClassNotFoundException e) {
            System.out.println("Erro: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            banco.fecharConexao();
        }
    }
}