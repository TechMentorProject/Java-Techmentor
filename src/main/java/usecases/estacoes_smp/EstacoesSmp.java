package usecases.estacoes_smp;

import infrastructure.database.BancoOperacoes;
import infrastructure.logging.Logger;
import infrastructure.utils.ValidacoesLinha;
import usecases.BaseDeDados;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EstacoesSmp extends BaseDeDados {

    private String cidade;
    private String operadora;
    private long latitude;
    private long longitude;
    private String codigoIBGE;
    private String tecnologia;
    private ValidacoesLinha validacoesLinha;
    private Logger loggerInsercoes;


    public EstacoesSmp(ValidacoesLinha validacoesLinha, Logger loggerInsercoes) {
        this.validacoesLinha = validacoesLinha;
        this.loggerInsercoes = loggerInsercoes;
    }

    public void inserirDadosComTratamento(List<List<Object>> dadosExcel, Connection conexao, BancoOperacoes bancoDeDados) throws SQLException, ClassNotFoundException {
        bancoDeDados.validarConexao();
        bancoDeDados.truncarTabela("estacoesSMP");

        System.out.println("Inserindo dados...");
        loggerInsercoes.gerarLog("💻 Iniciando inserção de dados na tabela estacoesSMP... 💻");

        String query = "INSERT INTO estacoesSMP (fkCidade, operadora, codigoIBGE, tecnologia) VALUES (?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = conexao.prepareStatement(query)) {
            processarEInserirDados(dadosExcel, preparedStatement, bancoDeDados);
            System.out.println("Dados inseridos com sucesso!");
        }
    }

    private void processarEInserirDados(List<List<Object>> dadosExcel, PreparedStatement preparedStatement, BancoOperacoes bancoDeDados) {
        // Cache de índices das colunas para otimizar o código
        Map<String, Integer> indiceColunas = new HashMap<>();
        indiceColunas.put("Municipio", obterIndiceColuna(dadosExcel, "Município-UF"));
        indiceColunas.put("Operadora", obterIndiceColuna(dadosExcel, "Empresa Fistel"));
        indiceColunas.put("CodigoIBGE", obterIndiceColuna(dadosExcel, "Código IBGE"));
        indiceColunas.put("Tecnologia", obterIndiceColuna(dadosExcel, "Tecnologia"));

        for (int i = 1; i < dadosExcel.size(); i++) {
            List<Object> linha = dadosExcel.get(i);
            String[] valores = processarLinha(linha);

            try {
                // Tenta extrair e inserir os valores; se falhar, loga o erro e continua
                if (extraindoValoresDoApache(preparedStatement, valores, linha, indiceColunas)) {
                    bancoDeDados.adicionarBatch(preparedStatement, i);
                }
            } catch (SQLException e) {
                System.err.println("Erro ao processar a linha " + i + ": " + e.getMessage());
                // Opcional: loggerInsercoes.gerarLog("Erro ao processar a linha " + i + ": " + e.getMessage());
            } catch (Exception e) {
                System.err.println("Erro inesperado na linha " + i + ": " + e.getMessage());
                // Opcional: loggerInsercoes.gerarLog("Erro inesperado na linha " + i + ": " + e.getMessage());
            }
        }
    }


    public int obterIndiceColuna(List<List<Object>> dadosExcel, String nomeColuna) {
        if (dadosExcel == null || dadosExcel.isEmpty() || dadosExcel.get(0).isEmpty()) {
            throw new IllegalArgumentException("O cabeçalho está vazio ou mal formado.");
        }

        String cabecalho = dadosExcel.get(0).toString();

        // Remover o BOM (Byte Order Mark) da primeira célula do cabeçalho, se presente
        if (cabecalho.charAt(0) == '\uFEFF') {
            cabecalho = cabecalho.substring(1);
        }

        String[] colunas = cabecalho.split(";");
        for (int i = 0; i < colunas.length; i++) {
            if (colunas[i].trim().equalsIgnoreCase(nomeColuna)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Coluna '" + nomeColuna + "' não encontrada no cabeçalho.");
    }

    private boolean extraindoValoresDoApache(PreparedStatement preparedStatement, String[] valores, List<Object> linha, Map<String, Integer> indiceColunas) throws SQLException {
        if (valores.length < 29) {
            System.err.println("Linha inválida, ignorando: " + linha);
            return false;
        }

        // Aplica o método formatarCidade para remover o sufixo do estado
        setCidade(formatarCidade(validacoesLinha.buscarValorValido(valores, indiceColunas.get("Municipio") + 2)));
        if (getCidade().matches("\\d+")) {
            return false;
        }

        setOperadora(validacoesLinha.buscarValorValido(valores, indiceColunas.get("Operadora")));
        setCodigoIBGE(validacoesLinha.buscarValorValido(valores, indiceColunas.get("CodigoIBGE")));
        setTecnologia(validacoesLinha.buscarValorValido(valores, indiceColunas.get("Tecnologia")));

        if (validacoesLinha.algumCampoInvalido(
                getCidade(),
                getOperadora(),
                getCodigoIBGE(),
                getTecnologia()
        )) {
            return false;
        }
        guardarValorProBanco(preparedStatement, getCidade(), getOperadora(), getCodigoIBGE(), getTecnologia());
        return true;
    }


    private String formatarCidade(String cidade) {
        if (cidade != null && cidade.contains("-")) {
            return cidade.split("-")[0].trim(); // Remove o estado após o "-"
        }
        return cidade;
    }

    private String[] processarLinha(List<Object> linha) {
        // Usando StringBuilder para manipulação de strings
        StringBuilder linhaBuilder = new StringBuilder();
        for (Object valor : linha) {
            linhaBuilder.append(valor).append(";");
        }
        return linhaBuilder.toString().split(";");
    }

    private void guardarValorProBanco(PreparedStatement preparedStatement, String cidade, String operadora, String codigoIBGE, String tecnologia) throws SQLException {
        preparedStatement.setString(1, cidade);
        preparedStatement.setString(2, operadora);
        preparedStatement.setString(3, codigoIBGE);
        preparedStatement.setString(4, tecnologia);
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public String getOperadora() {
        return operadora;
    }

    public void setOperadora(String operadora) {
        this.operadora = operadora;
    }

    public long getLatitude() {
        return latitude;
    }

    public void setLatitude(long latitude) {
        this.latitude = latitude;
    }

    public long getLongitude() {
        return longitude;
    }

    public void setLongitude(long longitude) {
        this.longitude = longitude;
    }

    public String getCodigoIBGE() {
        return codigoIBGE;
    }

    public void setCodigoIBGE(String codigoIBGE) {
        this.codigoIBGE = codigoIBGE;
    }

    public String getTecnologia() {
        return tecnologia;
    }

    public void setTecnologia(String tecnologia) {
        this.tecnologia = tecnologia;
    }

    public ValidacoesLinha getValidacoesLinha() {
        return validacoesLinha;
    }

    public void setValidacoesLinha(ValidacoesLinha validacoesLinha) {
        this.validacoesLinha = validacoesLinha;
    }

    public Logger getLoggerInsercoes() {
        return loggerInsercoes;
    }

    public void setLoggerInsercoes(Logger loggerInsercoes) {
        this.loggerInsercoes = loggerInsercoes;
    }
}
