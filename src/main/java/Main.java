import config.Configuracoes;
import config.NomeArquivo;
import infrastructure.database.BancoInsert;
import infrastructure.database.BancoOperacoes;
import infrastructure.database.BancoSetup;
import infrastructure.logging.Logger;
import infrastructure.processing.workbook.ManipularArquivo;
import infrastructure.s3.BaixarArquivoS3;
import infrastructure.utils.ValidacoesLinha;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.util.IOUtils;
import application.baseDeDados.CensoIbge;
import application.baseDeDados.EstacoesSmp;
import application.baseDeDados.Municipio;
import application.baseDeDados.ProjecaoPopulacional;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class Main {

    // Modo desenvolvimento e seleção do processo (defina o nome da base para teste)
    private static final boolean modoDev = true;
    private static final String nomeDaBaseDeDados = "MUNICIPIO"; // Use "CENSO", "ESTACOES", "MUNICIPIO" ou "PROJECAO"

    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        BancoOperacoes bancoDeDados = new BancoOperacoes();
        ManipularArquivo manipularArquivo = new ManipularArquivo();
        ValidacoesLinha validacoesLinha = new ValidacoesLinha();
        BaixarArquivoS3 baixarArquivoS3 = new BaixarArquivoS3();
        BancoInsert bancoInsert = new BancoInsert(bancoDeDados, validacoesLinha);
        bancoDeDados.conectar();
        BancoSetup bancoSetup = new BancoSetup(bancoDeDados.getConexao(), bancoInsert, manipularArquivo);
        Logger loggerEventos = Logger.getLoggerEventos();
        Logger loggerErros = Logger.getLoggerErros();

        try {
            // Configura o limite de memória do Apache POI
            IOUtils.setByteArrayMaxOverride(900_000_000);
            ZipSecureFile.setMaxEntrySize(900_000_000);
            ZipSecureFile.setMinInflateRatio(0.0);

            bancoSetup.criarEstruturaBanco();

            if (modoDev) {
                executarProcesso(nomeDaBaseDeDados, bancoDeDados, manipularArquivo, loggerEventos, loggerErros);
            } else {
                try {
                    baixarArquivoS3.baixarArquivos(); // Método que encapsula a lógica de download
                    System.out.println("Arquivos baixados com sucesso do S3.");
                } catch (IOException e) {
                    System.out.println("Erro ao baixar arquivos do S3: " + e.getMessage());
                    loggerErros.gerarLog("❌ Erro ao baixar arquivos do S3. ❌");
                }
                executarTodosProcessos(bancoDeDados, manipularArquivo, loggerEventos);
            }

        } catch (SQLException | ClassNotFoundException e) {
            System.out.println("Erro: " + e.getMessage());
            loggerErros.gerarLog("❌ Erro durante o processamento. ❌");
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
        int linhasInseridas = 0;
        Logger logger = new Logger(Configuracoes.CAMINHO_DIRETORIO_RAIZ.getValor(), "insercoes");
        CensoIbge censo = new CensoIbge(logger);
        String diretorioBase = Configuracoes.CAMINHO_DIRETORIO_RAIZ.getValor();
        File pasta = new File(diretorioBase);
        File[] arquivos = pasta.listFiles((dir, nome) -> nome.contains(NomeArquivo.CENSOIBGE.getNome()) && nome.endsWith(".xlsx"));

        if (arquivos != null) {
            bancoDeDados.truncarTabela("baseCensoIBGE");

            System.out.println("Inserindo dados no banco...");
            for (File arquivo : arquivos) {
                List<List<Object>> dados = manipularArquivo.lerPlanilha(arquivo.toString(), true);
                censo.inserirDados(dados, bancoDeDados.getConexao());
                linhasInseridas++;
            }
            loggerEventos.gerarLog("✅ Dados de CENSO Inseridos com Sucesso! ✅");
            System.out.println("Linhas inseridas: " + linhasInseridas);
            System.out.println("Inserção da baseCensoIBGE concluída com sucesso!");        }
    }

    private static void processarEstacoes(BancoOperacoes bancoDeDados, ManipularArquivo manipularArquivo, Logger loggerEventos) throws Exception {
        ValidacoesLinha validacoesLinha = new ValidacoesLinha();
        Logger logger = new Logger(Configuracoes.CAMINHO_DIRETORIO_RAIZ.getValor(), "insercoes");
        EstacoesSmp estacoesSmp = new EstacoesSmp(validacoesLinha, logger);
        String nomeArquivo = NomeArquivo.ESTACOES_SMP.getNome();
        String caminhoArquivo = Configuracoes.CAMINHO_DIRETORIO_RAIZ.getValor() + "/" + nomeArquivo;
        List<List<Object>> dados = manipularArquivo.lerPlanilha(caminhoArquivo, true);

        estacoesSmp.inserirDadosComTratamento(dados, bancoDeDados.getConexao(), bancoDeDados);
        loggerEventos.gerarLog("✅ Dados de ESTAÇÕES Inseridos com Sucesso! ✅");
    }

    private static void processarMunicipio(BancoOperacoes bancoDeDados, ManipularArquivo manipularArquivo, Logger loggerEventos) throws Exception {
        ValidacoesLinha validacoesLinha = new ValidacoesLinha();
        Logger logger = new Logger(Configuracoes.CAMINHO_DIRETORIO_RAIZ.getValor(), "insercoes");
        Municipio municipio = new Municipio(validacoesLinha, logger);
        String nomeArquivo = NomeArquivo.MUNICIPIO.getNome();
        String caminhoArquivo = Configuracoes.CAMINHO_DIRETORIO_RAIZ.getValor() + "/" + nomeArquivo;
        List<List<Object>> dados = manipularArquivo.lerPlanilha(caminhoArquivo, false);

        municipio.inserirDadosComTratamento(dados, bancoDeDados.getConexao(), bancoDeDados);
        loggerEventos.gerarLog("✅ Dados de MUNICIPIO Inseridos com Sucesso! ✅");
    }

    private static void processarProjecao(BancoOperacoes bancoDeDados, ManipularArquivo manipularArquivo, Logger loggerEventos) throws Exception {
        ValidacoesLinha validacoesLinha = new ValidacoesLinha();
        Logger logger = new Logger(Configuracoes.CAMINHO_DIRETORIO_RAIZ.getValor(), "insercoes");
        ProjecaoPopulacional projecaoPopulacional = new ProjecaoPopulacional(validacoesLinha, logger);
        String nomeArquivo = NomeArquivo.PROJECAO.getNome();
        String caminhoArquivo = Configuracoes.CAMINHO_DIRETORIO_RAIZ.getValor() + "/" + nomeArquivo;
        List<List<Object>> dados = manipularArquivo.lerPlanilha(caminhoArquivo, true);

        projecaoPopulacional.inserirDadosComTratamento(dados, bancoDeDados.getConexao(), bancoDeDados);
        loggerEventos.gerarLog("✅ Dados de PROJEÇÃO POPULACIONAL Inseridos com Sucesso! ✅");
    }
}
