package infrastructure.s3;

import config.Configuracoes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AdicionarArquivoS3 {

   private final S3Provider s3Provider;

    public AdicionarArquivoS3(S3Provider s3Provider) {
        this.s3Provider = s3Provider;
    }

    public void adicionarLogsS3() {
        String nomeBucket = Configuracoes.NOME_BUCKET_S3.getValor();
        String diretorioLogs = Configuracoes.DIRETORIO_LOGS.getValor();

        File diretorio = new File(diretorioLogs);

        if (diretorio.exists() && diretorio.isDirectory()) {
            // Chamada recursiva para processar o diretório e suas subpastas
            Path raizPath = Paths.get(diretorioLogs);
            enviarArquivosRecursivamente(s3Provider.getS3Client(), diretorio, nomeBucket, raizPath);
        } else {
            System.out.println("O diretório de logs não existe ou não é um diretório válido.");
        }
    }

    // Método auxiliar recursivo para percorrer as pastas e enviar arquivos
    private void enviarArquivosRecursivamente(S3Client s3Client, File diretorio, String nomeBucket, Path raizPath) {
        for (File arquivo : diretorio.listFiles()) {
            if (arquivo.isFile()) {
                // Cria uma key que preserva a estrutura de pastas em relação ao diretório raiz
                String relativePath = raizPath.relativize(arquivo.toPath()).toString().replace(File.separator, "/");
                String key = "logs/" + relativePath;

                // Configura a solicitação de upload
                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(nomeBucket)
                        .key(key)
                        .build();

                s3Provider.getS3Client().putObject(putObjectRequest, RequestBody.fromFile(arquivo));
                System.out.println("Log enviado para o S3");
            } else if (arquivo.isDirectory()) {
                // Chamada recursiva para processar subdiretórios
                enviarArquivosRecursivamente(s3Client, arquivo, nomeBucket, raizPath);
            }
        }
    }
}
