package infraestructure.s3;

import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class BaixarArquivoS3 {
    public static void main(String[] args) throws IOException {

        S3Client s3Client = new S3Provider().getS3Client();
        String caminhoArquivo = "./base de dados";
        String nomeObjeto;
        Path caminhoObjeto;

        List<Bucket> buckets =  s3Client.listBuckets().buckets();

        for (Bucket bucket : buckets) {
            System.out.println("Bucket: " + bucket.name());
        }
        ListObjectsRequest listObjects = ListObjectsRequest.builder().bucket("s3-bucket-atividade").build();

        List<S3Object> objects = s3Client.listObjects(listObjects).contents();
        for (S3Object object : objects) {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket("s3-bucket-atividade")
                    .key(object.key())
                    .build();

            nomeObjeto = object.key();
            caminhoObjeto = Paths.get(caminhoArquivo + nomeObjeto);
            System.out.println(object.key());

            InputStream objectContent = s3Client.getObject(getObjectRequest, ResponseTransformer.toInputStream());

            System.out.println(caminhoObjeto);
            if (Files.exists(caminhoObjeto)) {
                // Se o arquivo existir, apaga o arquivo
                System.out.println(caminhoObjeto);
                Files.delete(caminhoObjeto);
                System.out.println("Arquivo existente removido: " + caminhoObjeto);
            }
            Files.copy(objectContent, new File(caminhoArquivo + object.key()).toPath());
        }
    }
}

