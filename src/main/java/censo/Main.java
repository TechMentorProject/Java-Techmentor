package censo;

import geral.BancoOperacoes;
import geral.ManipularArquivo;
import org.apache.poi.util.IOUtils;

import java.io.File;
import java.sql.SQLException;
import java.util.List;

public class Main {

    public static void main(String[] args) throws SQLException {

        InserirDados banco = new InserirDados();
        BancoOperacoes bancoDeDados = new BancoOperacoes();
        ManipularArquivo manipularArquivo = new ManipularArquivo();

        try {
            // Aumentando limite de capacidade do Apache POI
            IOUtils.setByteArrayMaxOverride(250_000_000);

            String diretorioBase = "./base de dados";
            File pasta = new File(diretorioBase);
            File[] arquivos = pasta.listFiles((dir, nome) -> nome.contains("Território -") && nome.endsWith(".xlsx"));

            if (arquivos != null) {
                bancoDeDados.conectar();  // Conecta uma vez
                bancoDeDados.truncarTabela("censoIBGE");  // Trunca a tabela

                for (File arquivo : arquivos) {
                    List<List<Object>> dados = manipularArquivo.lerPlanilha(arquivo.toString());
                    System.out.println("Inserindo dados do arquivo: " + arquivo.getName());
                    banco.inserirDados(dados, bancoDeDados.getConexao());  // Reutiliza a conexão
                }

                bancoDeDados.fecharConexao();  // Fecha a conexão no final
            }

        } catch (SQLException | ClassNotFoundException e) {
            System.out.println("Erro: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (bancoDeDados.getConexao() != null && !bancoDeDados.getConexao().isClosed()) {
                bancoDeDados.fecharConexao();
            }
        }
    }
}
