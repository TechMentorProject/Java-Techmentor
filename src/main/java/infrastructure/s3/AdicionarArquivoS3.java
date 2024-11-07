package infrastructure.s3;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AdicionarArquivoS3 {

    public void adicionarLogsS3() {
        String nomeBucket = "techmentor-bucket";
        String diretorioLogs = "app/logs/LogsTechMentor";

        S3Client s3Client = new S3Provider().getS3Client();
        File diretorio = new File(diretorioLogs);

        if (diretorio.exists() && diretorio.isDirectory()) {
            // Chamada recursiva para processar o diretório e suas subpastas
            Path raizPath = Paths.get(diretorioLogs);
            enviarArquivosRecursivamente(s3Client, diretorio, nomeBucket, raizPath);
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

                s3Client.putObject(putObjectRequest, RequestBody.fromFile(arquivo));
                System.out.println("Upload realizado para o arquivo: " + key);
            } else if (arquivo.isDirectory()) {
                // Chamada recursiva para processar subdiretórios
                enviarArquivosRecursivamente(s3Client, arquivo, nomeBucket, raizPath);
            }
        }
    }
}
