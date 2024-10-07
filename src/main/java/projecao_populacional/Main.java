package projecao_populacional;

import projecao_populacional.BancoDeDados;
import projecao_populacional.TratarArquivo;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.util.IOUtils;

import java.sql.SQLException;
import java.util.List;

public class Main {

    public static void main(String[] args) throws SQLException {

        projecao_populacional.BancoDeDados banco = new BancoDeDados();
        projecao_populacional.TratarArquivo tratarArquivo = new TratarArquivo();

        try {

            IOUtils.setByteArrayMaxOverride(250_000_000); // Defina um limite maior conforme necessário
            // Ajustando o limite da razão de descompressão
            ZipSecureFile.setMinInflateRatio(0.000001); // Diminui o valor padrão para permitir arquivos mais comprimidos
            // Conectar ao banco

            // Caminho do arquivo .xlsx
            String caminhoArquivoOriginal = "C:\\Users\\mathe\\Documents\\Techmentor\\Java - Techmentor\\Java-Techmentor\\base de dados\\projecoes_2024_tab1_idade_simples (1).xlsx";


            // Remover as colunas e inserir os dados no banco
            List<List<Object>> dados = tratarArquivo.LerArquivo(caminhoArquivoOriginal);
            banco.conectar();
            banco.inserirDados(dados);

        } catch (SQLException | ClassNotFoundException e) {
            System.out.println("Erro: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            // Fechar a conexão
            banco.fecharConexao();
        }
    }
}

// Aumentar o limite de bytes para leitura de registros



