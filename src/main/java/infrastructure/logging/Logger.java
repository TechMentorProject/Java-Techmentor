package infrastructure.logging;



import config.Configuracoes;
import infrastructure.s3.AdicionarArquivoS3;
import infrastructure.s3.S3Provider;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {

    // Instâncias estáticas dos loggers
    private static Logger loggerEventos;
    private static Logger loggerErros;
    public static Logger loggerInsercoes;

    // Atributos privados
    private String logFileName;
    private FileWriter logFileWriter;
    private DateTimeFormatter logFormatter;

    // Construtor privado para inicializar o logger
    public Logger(String directoryName, String logType) {
        try {
            // Cria o diretório caso não exista
            File directory = new File(directoryName);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Formata a data e hora para o nome do arquivo
            DateTimeFormatter fileNameFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
            String timestamp = fileNameFormatter.format(LocalDateTime.now());

            // Define o nome do arquivo de log
            String fileName = directoryName + "/techMentorLog_" + logType + "_" + timestamp + ".txt";
            this.logFileName = fileName;

            // Abre o arquivo de log para escrita
            this.logFileWriter = new FileWriter(this.logFileName, true);
            this.logFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Método estático para obter a instância do Logger de Eventos
    public static Logger getLoggerEventos() {
        if (loggerEventos == null) {
            loggerEventos = new Logger("app/logs/LogsTechMentor/Eventos", "eventos");
        }
        return loggerEventos;
    }

    // Método público para obter a instância do Logger de Erros
    public static Logger getLoggerErros() {
        if (loggerErros == null) {
            loggerErros = new Logger("app/logs/LogsTechMentor/Erros", "erros");
        }
        return loggerErros;
    }

    // Método estático para obter a instância do Logger de Inserções
    public static Logger getLoggerInsercoes() {
        if (loggerInsercoes == null) {
            loggerInsercoes = new Logger("app/logs/LogsTechMentor/Insercoes", "insercoes");
        }
        return loggerInsercoes;
    }

    // Método para gerar um log com a mensagem fornecida
    public void gerarLog(String message) {
        try {
            // Formata a data e hora para o log
            String timestamp = logFormatter.format(LocalDateTime.now());
            // Escreve a mensagem no arquivo de log
            this.logFileWriter.write(timestamp + " - " + message + "\n");
            // Força a escrita no arquivo
            this.logFileWriter.flush();

            if(!Configuracoes.AMBIENTE.getValor().equals("DEV")) {
                S3Provider s3Provider = new S3Provider();
                AdicionarArquivoS3 adicionarArquivoS3 = new AdicionarArquivoS3(s3Provider);
                adicionarArquivoS3.adicionarLogsS3();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Método para fechar o arquivo de log
    public void fecharLog() {
        try {
            if (this.logFileWriter != null) {
                // Fecha o arquivo de log
                this.logFileWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}