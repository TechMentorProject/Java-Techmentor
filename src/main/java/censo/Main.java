package censo;

import org.apache.poi.openxml4j.util.ZipSecureFile;

public class Main {

    public static void main(String[] args) {
        // Aumentar o limite de bytes para leitura de registros ZIP grandes (Excel)
        ZipSecureFile.setMinInflateRatio(0);

        // Instanciar as classes necessárias
        TratarArquivo tratarArquivo = new TratarArquivo();
        BancoDeDados bancoDeDados = new BancoDeDados();

        // Definir o diretório base onde os arquivos estão localizados
        String diretorioBase = "C:\\Users\\mathe\\Documents\\Techmentor\\Java - Techmentor\\Java-Techmentor\\base de dados";

        try {
            // Processar arquivos e inserir dados no banco
            tratarArquivo.processarArquivosEDados(diretorioBase, bancoDeDados);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                // Fechar a conexão ao banco de dados ao final
                bancoDeDados.fecharConexao();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
