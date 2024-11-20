package infrastructure.s3;

import config.Configuracoes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AdicionarArquivoS3 {

   private final S3Client s3Client;

    public AdicionarArquivoS3( S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public void adicionarLogsS3() {
        String nomeBucket = Configuracoes.NOME_BUCKET_S3.getValor();
        String diretorioLogs = Configuracoes.DIRETORIO_LOGS.getValor();

        File diretorio = new File(diretorioLogs);

        if (diretorio.exists() && diretorio.isDirectory()) {
            Path raizPath = Paths.get(diretorioLogs);
            enviarArquivosRecursivamente(s3Client, diretorio, nomeBucket, raizPath);
        } else {
            System.out.println("O diretório de logs não existe ou não é um diretório válido.");
        }
    }

    private void enviarArquivosRecursivamente(S3Client s3Client, File diretorio, String nomeBucket, Path raizPath) {
        for (File arquivo : diretorio.listFiles()) {
            if (arquivo.isFile()) {
                String relativePath = raizPath.relativize(arquivo.toPath()).toString().replace(File.separator, "/");
                String key = "logs/" + relativePath;

                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(nomeBucket)
                        .key(key)
                        .build();

                s3Client.putObject(putObjectRequest, RequestBody.fromFile(arquivo));
            } else if (arquivo.isDirectory()) {
                enviarArquivosRecursivamente(s3Client, arquivo, nomeBucket, raizPath);
            }
        }
    }
}
