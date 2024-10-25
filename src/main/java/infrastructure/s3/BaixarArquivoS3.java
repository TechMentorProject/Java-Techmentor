package infrastructure.s3;
import io.github.cdimascio.dotenv.Dotenv;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import java.nio.file.StandardCopyOption; // Para substituir arquivos ao copiar, se necessário

public class BaixarArquivoS3 {
    public static void main(String[] args) throws IOException {

        Dotenv dotenv = Dotenv.load();

        // Obtém o valor da variável de ambiente S3_BUCKET
        String nomeBucket = dotenv.get("S3_BUCKET_NAME");
        S3Client s3Client = new S3Provider().getS3Client();
        String caminhoArquivo = dotenv.get("CAMINHO_BASE");
        String nomeObjeto;
        Path caminhoObjeto;

        List<Bucket> buckets =  s3Client.listBuckets().buckets();

        for (Bucket bucket : buckets) {
            System.out.println("Bucket: " + bucket.name());
        }
        ListObjectsRequest listObjects = ListObjectsRequest.builder().bucket(nomeBucket).build();

        List<S3Object> objects = s3Client.listObjects(listObjects).contents();
        for (S3Object object : objects) {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(nomeBucket)
                    .key(object.key())
                    .build();

            nomeObjeto = object.key();
            // Concatene corretamente o caminho e o nome do objeto
            caminhoObjeto = Paths.get(caminhoArquivo, nomeObjeto);
            System.out.println(object.key());

            InputStream objectContent = s3Client.getObject(getObjectRequest, ResponseTransformer.toInputStream());

            System.out.println(caminhoObjeto);
            if (Files.exists(caminhoObjeto)) {
                // Se o arquivo existir, apaga o arquivo
                System.out.println(caminhoObjeto);
                Files.delete(caminhoObjeto);
                System.out.println("Arquivo existente removido: " + caminhoObjeto);
            }

            // Garanta que os diretórios existam antes de salvar o arquivo
            Files.createDirectories(caminhoObjeto.getParent());

            // Copie o arquivo, substituindo caso já exista
            Files.copy(objectContent, caminhoObjeto, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Arquivo baixado: " + caminhoObjeto);
        }
    }
}
