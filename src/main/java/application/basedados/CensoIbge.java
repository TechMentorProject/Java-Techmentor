package application.basedados;
import application.BaseDeDados;
import infrastructure.database.BancoOperacoes;
import infrastructure.logging.Logger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CensoIbge extends BaseDeDados {

    private String cidade;
    private Double area;
    private Double densidadeDemografica;

    public CensoIbge(Logger logger) {
        this.logger = logger;
    }

    public void inserirDadosComTratamento(List<List<Object>> dadosExcel, Connection conexao, BancoOperacoes bancoDeDados) throws SQLException, ClassNotFoundException {
        bancoDeDados.validarConexao();

        logger.getLoggerEventos().gerarLog("Iniciando inserção de dados na tabela censoIBGE... ");

        String query = "INSERT INTO baseCensoIBGE (fkCidade, area, densidadeDemografica) VALUES (?, ?, ?)";
        try (PreparedStatement preparedStatement = conexao.prepareStatement(query)) {
            processarEInserirDados(dadosExcel, preparedStatement, bancoDeDados);

            preparedStatement.executeBatch();
            conexao.commit();

            logger.getLoggerInsercoes().gerarLog("Inserção de dados concluída com sucesso!");
        }
    }

    public void processarEInserirDados(List<List<Object>> dadosExcel, PreparedStatement preparedStatement, BancoOperacoes bancoDeDados) {
        Map<String, Integer> indiceColunas = new HashMap<>();
        indiceColunas.put("Municipio", obterIndiceColuna(dadosExcel, "Município"));
        indiceColunas.put("Area", obterIndiceColuna(dadosExcel, "Área(km²)"));
        indiceColunas.put("DensidadeDemografica", obterIndiceColuna(dadosExcel, "Densidade demográfica(hab/km²)"));

        for (int i = 1; i < dadosExcel.size(); i++) {
            List<Object> linha = dadosExcel.get(i);
            String[] valores = processarLinha(linha);

            try {
                if (valores.length >= 3 && extrairValoresDoCenso(preparedStatement, valores, indiceColunas)) {
                    preparedStatement.addBatch();
                }

            } catch (SQLException e) {
                logger.getLoggerErros().gerarLog("❌ Erro SQL ao processar linha " + i + ": " + e.getMessage());
            } catch (Exception e) {
                logger.getLoggerErros().gerarLog("❌ Erro geral ao processar linha " + i + ": " + e.getMessage());
            }
        }
    }

    private boolean extrairValoresDoCenso(PreparedStatement preparedStatement, String[] valores, Map<String, Integer> indiceColunas) throws SQLException {
        setCidade(buscarValorValido(valores, indiceColunas.get("Municipio")));
        if (getCidade().isEmpty() || getCidade().matches("\\d+")) {
            return false;
        }

        try {
            setArea(Double.parseDouble(buscarValorValido(valores, indiceColunas.get("Area"))));
            setDensidadeDemografica(Double.parseDouble(buscarValorValido(valores, indiceColunas.get("DensidadeDemografica"))));
        } catch (NumberFormatException e) {
            logger.gerarLog("⚠️ Valores inválidos para conversão numérica: " + e.getMessage());
            return false;
        }

        if (algumCampoInvalido(getCidade(), getArea(), getDensidadeDemografica())) {
            return false;
        }

        guardarValorParaOBanco(preparedStatement);
        return true;
    }

    public void guardarValorParaOBanco(PreparedStatement preparedStatement) throws SQLException {
        preparedStatement.setString(1, getCidade().trim());
        preparedStatement.setDouble(2, getArea());
        preparedStatement.setDouble(3, getDensidadeDemografica());
    }

    public int obterIndiceColuna(List<List<Object>> dadosExcel, String nomeColuna) {
        if (dadosExcel == null || dadosExcel.isEmpty() || dadosExcel.get(0).isEmpty()) {
            throw new IllegalArgumentException("O cabeçalho está vazio ou mal formado.");
        }

        String cabecalho = dadosExcel.get(0).toString();
        if (cabecalho.charAt(0) == '\uFEFF') {
            cabecalho = cabecalho.substring(1);
        }

        String[] colunas = cabecalho.split(",");
        for (int i = 0; i < colunas.length; i++) {
            if (colunas[i].trim().equalsIgnoreCase(nomeColuna)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Coluna '" + nomeColuna + "' não encontrada no cabeçalho.");
    }

    public String getCidade() { return cidade; }

    public void setCidade(String cidade) { this.cidade = cidade; }

    public Double getArea() { return area; }

    public void setArea(Double area) { this.area = area; }

    public Double getDensidadeDemografica() { return densidadeDemografica; }

    public void setDensidadeDemografica(Double densidadeDemografica) { this.densidadeDemografica = densidadeDemografica; }
}