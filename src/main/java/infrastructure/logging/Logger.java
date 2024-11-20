package infrastructure.logging;

import config.Configuracoes;
import infrastructure.s3.AdicionarArquivoS3;
import infrastructure.s3.S3Provider;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {

    public static Logger loggerEventos;
    public static Logger loggerErros;
    public static Logger loggerInsercoes;

    private String logFileName;
    private DateTimeFormatter logFormatter;

    public Logger(String directoryName, String logType) {
        try {
            File directory = new File(directoryName);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            DateTimeFormatter fileNameFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
            String timestamp = fileNameFormatter.format(LocalDateTime.now());

            this.logFileName = directoryName + "/techMentorLog_" + logType + "_" + timestamp + ".txt";
            this.logFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Logger getLoggerEventos() {
        if (loggerEventos == null) {
            loggerEventos = new Logger("app/logs/LogsTechMentor/Eventos", "eventos");
        }
        return loggerEventos;
    }

    public Logger getLoggerErros() {
        if (loggerErros == null) {
            loggerErros = new Logger("app/logs/LogsTechMentor/Erros", "erros");
        }
        return loggerErros;
    }

    public Logger getLoggerInsercoes() {
        if (loggerInsercoes == null) {
            loggerInsercoes = new Logger("app/logs/LogsTechMentor/Insercoes", "insercoes");
        }
        return loggerInsercoes;
    }

    public void gerarLog(String message) {
        try (FileWriter writer = new FileWriter(this.logFileName, true)) {
            String timestamp = logFormatter.format(LocalDateTime.now());
            writer.write(timestamp + " - " + message + "\n");

            if (!Configuracoes.AMBIENTE.getValor().equals("DEV")) {
                S3Client s3Client = new S3Provider().getS3Client();
                AdicionarArquivoS3 adicionarArquivoS3 = new AdicionarArquivoS3(s3Client);
                adicionarArquivoS3.adicionarLogsS3();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
