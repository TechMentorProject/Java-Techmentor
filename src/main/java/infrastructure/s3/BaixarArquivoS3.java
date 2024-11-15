package infrastructure.s3;
import config.Configuracoes;
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

        String nomeBucket = Configuracoes.NOME_BUCKET_S3.getValor();
        S3Client s3Client = new S3Provider().getS3Client();
        String caminhoArquivo = Configuracoes.CAMINHO_DIRETORIO_RAIZ.getValor();
        String nomeObjeto;
        Path caminhoObjeto;


        int arquivosRemovidos = 0;
        int arquivosBaixados = 0;

        List<Bucket> buckets =  s3Client.listBuckets().buckets();

        for (Bucket bucket : buckets) {
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

            InputStream objectContent = s3Client.getObject(getObjectRequest, ResponseTransformer.toInputStream());

            if (Files.exists(caminhoObjeto)) {
                Files.delete(caminhoObjeto);
                arquivosRemovidos++;
            }

            // Garanta que os diretórios existam antes de salvar o arquivo
            Files.createDirectories(caminhoObjeto.getParent());

            // Copie o arquivo, substituindo caso já exista
            Files.copy(objectContent, caminhoObjeto, StandardCopyOption.REPLACE_EXISTING);
            arquivosBaixados++;
        }
        System.out.println("S3 - Total de arquivos removidos: " + arquivosRemovidos);
        System.out.println("S3 - Total de arquivos baixados: " + arquivosBaixados);
    }
}
