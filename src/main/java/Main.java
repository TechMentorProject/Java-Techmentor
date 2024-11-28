import application.BaseDeDados;
import config.Configuracoes;
import config.NomeArquivo;
import infrastructure.database.BancoInsert;
import infrastructure.database.BancoOperacoes;
import infrastructure.database.BancoSetup;
import infrastructure.logging.Logger;
import infrastructure.processing.workbook.ManipularArquivo;
import infrastructure.s3.OperacoesS3;
import infrastructure.s3.S3Provider;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.util.IOUtils;
import application.basedados.CensoIbge;
import application.basedados.EstacoesSmp;
import application.basedados.Municipio;
import application.basedados.ProjecaoPopulacional;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class Main {

    // Modo desenvolvimento e seleção do processo (defina o nome da base para teste)
    private static final boolean modoDev = true;
    private static Integer linhasCensoIbge = 0;
    private static final String nomeDaBaseDeDados = "ESTACOES"; // Use "CENSO", "ESTACOES", "MUNICIPIO" ou "PROJECAO"

    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        BancoOperacoes bancoDeDados = new BancoOperacoes();
        ManipularArquivo manipularArquivo = new ManipularArquivo();
        Logger logger = new Logger(Configuracoes.CAMINHO_DIRETORIO_RAIZ.getValor(), "insercoes");
        BaseDeDados baseDeDados = new Municipio(logger);
        BancoInsert bancoInsert = new BancoInsert(bancoDeDados, baseDeDados);
        bancoDeDados.conectar();
        BancoSetup bancoSetup = new BancoSetup(bancoDeDados.getConexao(), bancoInsert, manipularArquivo);


        try {
            // Configura o limite de memória do Apache POI
            IOUtils.setByteArrayMaxOverride(900_000_000);
            ZipSecureFile.setMaxEntrySize(900_000_000);
            ZipSecureFile.setMinInflateRatio(0.0);

            if (modoDev) {
                bancoSetup.criarEstruturaBanco();
                executarProcesso(nomeDaBaseDeDados, bancoDeDados, manipularArquivo, logger.getLoggerEventos(), logger.getLoggerErros());
            } else {
                try {
                    System.out.println("Ambiente configurado: " + Configuracoes.AMBIENTE.getValor());
                    if(!Configuracoes.AMBIENTE.getValor().equals("DEV")) {
                        S3Client s3Client = new S3Provider().getS3Client();
                        OperacoesS3 operacoesS3 = new OperacoesS3(s3Client);
                        operacoesS3.baixarArquivos();
                        System.out.println("Arquivos baixados com sucesso do S3.");
                    }
                } catch (IOException e) {
                    System.out.println("Erro ao baixar arquivos do S3: " + e.getMessage());
                    logger.getLoggerErros().gerarLog("❌ Erro ao baixar arquivos do S3. ❌");
                }
                bancoSetup.criarEstruturaBanco();
                executarTodosProcessos(bancoDeDados, manipularArquivo, logger.getLoggerEventos());
            }

        } catch (SQLException | ClassNotFoundException e) {
            System.out.println("Erro: " + e.getMessage());
            logger.getLoggerErros().gerarLog("❌ Erro durante o processamento. ❌");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            bancoDeDados.fecharConexao();
        }
    }

    private static void executarTodosProcessos(BancoOperacoes bancoDeDados, ManipularArquivo manipularArquivo, Logger loggerEventos) throws Exception {
        processarCenso(bancoDeDados, manipularArquivo, loggerEventos);
        processarEstacoes(bancoDeDados, manipularArquivo, loggerEventos);
        processarMunicipio(bancoDeDados, manipularArquivo, loggerEventos);
        processarProjecao(bancoDeDados, manipularArquivo, loggerEventos);
    }

    private static void executarProcesso(String nomeDaBaseDeDados, BancoOperacoes bancoDeDados, ManipularArquivo manipularArquivo, Logger loggerEventos, Logger loggerErros) throws Exception {
        switch (nomeDaBaseDeDados.toUpperCase()) {
            case "CENSO":
                processarCenso(bancoDeDados, manipularArquivo, loggerEventos);
                break;
            case "ESTACOES":
                processarEstacoes(bancoDeDados, manipularArquivo, loggerEventos);
                break;
            case "MUNICIPIO":
                processarMunicipio(bancoDeDados, manipularArquivo, loggerEventos);
                break;
            case "PROJECAO":
                processarProjecao(bancoDeDados, manipularArquivo, loggerEventos);
                break;
            default:
                System.out.println("Erro: Nome da base de dados inválido. Use CENSO, ESTACOES, MUNICIPIO ou PROJECAO.");
        }
    }

    private static void processarCenso(BancoOperacoes bancoDeDados, ManipularArquivo manipularArquivo, Logger loggerEventos) throws Exception {
        Logger logger = new Logger(Configuracoes.CAMINHO_DIRETORIO_RAIZ.getValor(), "insercoes");
        CensoIbge censo = new CensoIbge(logger);
        String diretorioBase = Configuracoes.CAMINHO_DIRETORIO_RAIZ.getValor() + "/Censo";
        File pastaBase = new File(diretorioBase);

        if (!pastaBase.isDirectory()) {
            throw new IllegalArgumentException("O caminho base não é um diretório válido: " + diretorioBase);
        }

        bancoDeDados.truncarTabela("baseCensoIBGE");

        System.out.println("Inserindo dados no banco...");
        processarDiretorios(pastaBase, bancoDeDados, manipularArquivo, censo, loggerEventos);
        System.out.println("Linhas inseridas: " + linhasCensoIbge);
        System.out.println("Inserção da baseCensoIBGE concluída com sucesso!");

    }
    private static void processarDiretorios(File pasta, BancoOperacoes bancoDeDados, ManipularArquivo manipularArquivo, CensoIbge censo, Logger loggerEventos) throws Exception {
        File[] conteudo = pasta.listFiles();
        if (conteudo == null) return;

        for (File arquivoOuDiretorio : conteudo) {
            if (arquivoOuDiretorio.isDirectory()) {
                processarDiretorios(arquivoOuDiretorio, bancoDeDados, manipularArquivo, censo, loggerEventos);
            } else if (arquivoOuDiretorio.isFile() && arquivoOuDiretorio.getName().contains(NomeArquivo.CENSOIBGE.getNome()) && arquivoOuDiretorio.getName().endsWith(".xlsx")) {
                List<List<Object>> dados = manipularArquivo.lerPlanilha(arquivoOuDiretorio.toString(), true);
                censo.inserirDadosComTratamento(dados, bancoDeDados.getConexao(), bancoDeDados);
                linhasCensoIbge++;
            }
        }
        loggerEventos.gerarLog("✅ Diretório processado: " + pasta.getAbsolutePath());
    }

    private static void processarEstacoes(BancoOperacoes bancoDeDados, ManipularArquivo manipularArquivo, Logger loggerEventos) throws Exception {
        Logger logger = new Logger(Configuracoes.CAMINHO_DIRETORIO_RAIZ.getValor(), "insercoes");
        EstacoesSmp estacoesSmp = new EstacoesSmp(logger);
        String nomeArquivo = NomeArquivo.ESTACOES_SMP.getNome();
        String caminhoArquivo = Configuracoes.CAMINHO_DIRETORIO_RAIZ.getValor() + "/" + nomeArquivo;
        List<List<Object>> dados = manipularArquivo.lerPlanilha(caminhoArquivo, true);

        estacoesSmp.inserirDadosComTratamento(dados, bancoDeDados.getConexao(), bancoDeDados);
        loggerEventos.gerarLog("✅ Dados de ESTAÇÕES Inseridos com Sucesso! ✅");
    }

    private static void processarMunicipio(BancoOperacoes bancoDeDados, ManipularArquivo manipularArquivo, Logger loggerEventos) throws Exception {
        Logger logger = new Logger(Configuracoes.CAMINHO_DIRETORIO_RAIZ.getValor(), "insercoes");
        Municipio municipio = new Municipio(logger);
        String nomeArquivo = NomeArquivo.MUNICIPIO.getNome();
        String caminhoArquivo = Configuracoes.CAMINHO_DIRETORIO_RAIZ.getValor() + "/" + nomeArquivo;
        List<List<Object>> dados = manipularArquivo.lerPlanilha(caminhoArquivo, false);

        municipio.inserirDadosComTratamento(dados, bancoDeDados.getConexao(), bancoDeDados);
        loggerEventos.gerarLog("✅ Dados de MUNICIPIO Inseridos com Sucesso! ✅");
    }

    private static void processarProjecao(BancoOperacoes bancoDeDados, ManipularArquivo manipularArquivo, Logger loggerEventos) throws Exception {
        Logger logger = new Logger(Configuracoes.CAMINHO_DIRETORIO_RAIZ.getValor(), "insercoes");
        ProjecaoPopulacional projecaoPopulacional = new ProjecaoPopulacional(logger);
        String nomeArquivo = NomeArquivo.PROJECAO.getNome();
        String caminhoArquivo = Configuracoes.CAMINHO_DIRETORIO_RAIZ.getValor() + "/" + nomeArquivo;
        List<List<Object>> dados = manipularArquivo.lerPlanilha(caminhoArquivo, true);

        projecaoPopulacional.inserirDadosComTratamento(dados, bancoDeDados.getConexao(), bancoDeDados);
        loggerEventos.gerarLog("✅ Dados de PROJEÇÃO POPULACIONAL Inseridos com Sucesso! ✅");
    }
}
