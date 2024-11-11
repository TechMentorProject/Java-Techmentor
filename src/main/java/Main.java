import config.Configuracoes;
import config.NomeArquivo;
import infrastructure.database.BancoInsert;
import infrastructure.database.BancoOperacoes;
import infrastructure.database.BancoSetup;
import infrastructure.logging.Logger;
import infrastructure.processing.workbook.ManipularArquivo;
import infrastructure.utils.ValidacoesLinha;
import org.apache.poi.util.IOUtils;
import usecases.censo.CensoIbge;
import usecases.estacoes_smp.EstacoesSmp;
import usecases.municipio.Municipio;
import usecases.projecao_populacional.ProjecaoPopulacional;

import java.io.File;
import java.sql.SQLException;
import java.util.List;

public class Main {

    // Modo desenvolvimento e seleção do processo (defina o nome da base para teste)
    private static final boolean modoDev = false;
    private static final String nomeDaBaseDeDados = "CENSO"; // Use "CENSO", "ESTACOES", "MUNICIPIO" ou "PROJECAO"

    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        BancoOperacoes bancoDeDados = new BancoOperacoes();
        ManipularArquivo manipularArquivo = new ManipularArquivo();
        ValidacoesLinha validacoesLinha = new ValidacoesLinha();
        BancoInsert bancoInsert = new BancoInsert(bancoDeDados, validacoesLinha);
        bancoDeDados.conectar();
        BancoSetup bancoSetup = new BancoSetup(bancoDeDados.getConexao(), bancoInsert, manipularArquivo);
        Logger loggerEventos = Logger.getLoggerEventos();
        Logger loggerErros = Logger.getLoggerErros();

        try {
            // Configura o limite de memória do Apache POI
            IOUtils.setByteArrayMaxOverride(250_000_000);
            bancoSetup.criarEstruturaBanco();

            // Se estiver em modoDev, executa apenas o processo especificado
            if (modoDev) {
                executarProcesso(nomeDaBaseDeDados, bancoDeDados, manipularArquivo, loggerEventos, loggerErros);
            } else {
                // Executa todos os processos sequencialmente
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
        Logger logger = new Logger(Configuracoes.CAMINHO_DIRETORIO_RAIZ.getValor(), "insercoes");
        CensoIbge censo = new CensoIbge(logger);
        String diretorioBase = Configuracoes.CAMINHO_DIRETORIO_RAIZ.getValor();
        File pasta = new File(diretorioBase);
        File[] arquivos = pasta.listFiles((dir, nome) -> nome.contains(NomeArquivo.CENSOIBGE.getNome()) && nome.endsWith(".xlsx"));

        if (arquivos != null) {
            bancoDeDados.truncarTabela("censoIBGE");

            for (File arquivo : arquivos) {
                List<List<Object>> dados = manipularArquivo.lerPlanilha(arquivo.toString(), true);
                System.out.println("Inserindo dados do arquivo: " + arquivo.getName());
                censo.inserirDados(dados, bancoDeDados.getConexao());
                loggerEventos.gerarLog("✅ Dados de CENSO Inseridos com Sucesso! ✅");
            }
        }
    }

    private static void processarEstacoes(BancoOperacoes bancoDeDados, ManipularArquivo manipularArquivo, Logger loggerEventos) throws Exception {
        ValidacoesLinha validacoesLinha = new ValidacoesLinha();
        Logger logger = new Logger(Configuracoes.CAMINHO_DIRETORIO_RAIZ.getValor(), "insercoes");
        EstacoesSmp estacoesSmp = new EstacoesSmp(validacoesLinha, logger);
        String nomeArquivo = NomeArquivo.ESTACOES_SMP.getNome();
        String caminhoArquivo = Configuracoes.CAMINHO_DIRETORIO_RAIZ.getValor() + "/" + nomeArquivo;
        List<List<Object>> dados = manipularArquivo.lerPlanilha(caminhoArquivo, false);

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
