package infrastructure.s3;

import config.Configuracoes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class OperacoesS3 implements Closeable {

    private final S3Client s3Client;

    public OperacoesS3(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public void baixarArquivos() throws IOException {
        String nomeBucket = Configuracoes.NOME_BUCKET_S3.getValor();
        String caminhoArquivo = Configuracoes.CAMINHO_DIRETORIO_RAIZ.getValor();

        String continuationToken = null;
        ExecutorService executor = Executors.newFixedThreadPool(10); // 10 threads

        do {
            ListObjectsV2Request.Builder listObjectsBuilder = ListObjectsV2Request.builder()
                    .bucket(nomeBucket)
                    .maxKeys(1000);

            if (continuationToken != null) {
                listObjectsBuilder.continuationToken(continuationToken);
            }

            ListObjectsV2Response listObjectsResponse = s3Client.listObjectsV2(listObjectsBuilder.build());
            continuationToken = listObjectsResponse.nextContinuationToken();

            List<S3Object> objects = listObjectsResponse.contents();

            for (S3Object object : objects) {
                executor.submit(() -> {
                    try {
                        String nomeObjeto = object.key();

                        // Ignorar diretórios chamados "logs" ou seus conteúdos
                        if (nomeObjeto.startsWith("logs/")) {
                            return; // Ignorar objetos ou diretórios dentro de "logs/"
                        }

                        // Filtrar apenas arquivos com extensão ".xlsx"
                        if (!nomeObjeto.endsWith(".xlsx")) {
                            return; // Ignorar arquivos que não sejam ".xlsx"
                        }

                        Path caminhoObjeto = Paths.get(caminhoArquivo, nomeObjeto);

                        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                                .bucket(nomeBucket)
                                .key(nomeObjeto)
                                .build();

                        try (InputStream objectContent = s3Client.getObject(getObjectRequest, ResponseTransformer.toInputStream())) {
                            Files.createDirectories(caminhoObjeto.getParent());
                            Files.copy(objectContent, caminhoObjeto, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (Exception e) {
                        System.err.println("Erro ao processar arquivo: " + object.key());
                        e.printStackTrace();
                    }
                });
            }
        } while (continuationToken != null);

        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void adicionarLogsS3() {
        String nomeBucket = Configuracoes.NOME_BUCKET_S3.getValor();
        String diretorioLogs = Configuracoes.DIRETORIO_LOGS.getValor();

        File diretorio = new File(diretorioLogs);

        if (diretorio.exists() && diretorio.isDirectory()) {
            Path raizPath = Paths.get(diretorioLogs);
            enviarArquivosRecursivamente(diretorio, nomeBucket, raizPath);
        } else {
            System.out.println("O diretório de logs não existe ou não é um diretório válido.");
        }
    }

    private void enviarArquivosRecursivamente(File diretorio, String nomeBucket, Path raizPath) {
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
                enviarArquivosRecursivamente(arquivo, nomeBucket, raizPath);
            }
        }
    }

    @Override
    public void close() {
        // Fechar o cliente S3 para liberar recursos
        if (s3Client != null) {
            s3Client.close();
        }
    }
}
