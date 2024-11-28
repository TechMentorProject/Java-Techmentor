package infrastructure.s3;

import config.Configuracoes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class OperacoesS3 {

    private final S3Client s3Client;

    public OperacoesS3(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public void baixarArquivos() throws IOException {
        String nomeBucket = Configuracoes.NOME_BUCKET_S3.getValor();
        String caminhoArquivo = Configuracoes.CAMINHO_DIRETORIO_RAIZ.getValor();
        String nomeObjeto;
        Path caminhoObjeto;

        int arquivosRemovidos = 0;
        int arquivosBaixados = 0;

        ListObjectsRequest listObjects = ListObjectsRequest.builder().bucket(nomeBucket).build();
        List<S3Object> objects = s3Client.listObjects(listObjects).contents();

        for (S3Object object : objects) {
            nomeObjeto = object.key();

            if (nomeObjeto.equals("logs") || nomeObjeto.startsWith("logs/")) {
                System.out.println("Ignorando: " + nomeObjeto);
                continue;
            }

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(nomeBucket)
                    .key(nomeObjeto)
                    .build();

            caminhoObjeto = Paths.get(caminhoArquivo, nomeObjeto);

            try (InputStream objectContent = s3Client.getObject(getObjectRequest, ResponseTransformer.toInputStream())) {
                if (Files.exists(caminhoObjeto)) {
                    Files.delete(caminhoObjeto);
                    arquivosRemovidos++;
                }

                Files.createDirectories(caminhoObjeto.getParent());
                Files.copy(objectContent, caminhoObjeto, StandardCopyOption.REPLACE_EXISTING);
                arquivosBaixados++;
            }
        }

        System.out.println("-----------------------------------------");
        System.out.println("S3 - Total de arquivos removidos: " + arquivosRemovidos);
        System.out.println("S3 - Total de arquivos baixados: " + arquivosBaixados);
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
}

