package application.baseDeDados;

import application.BaseDeDados;
import infrastructure.database.BancoOperacoes;
import infrastructure.logging.Logger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class CensoIbge extends BaseDeDados {

    private String cidade;
    private Double area;
    private Double densidadeDemografica;
    private Logger loggerInsercoes;

    public CensoIbge(Logger logger) {
        this.loggerInsercoes = logger;
    }

    public int obterIndiceColuna(List<List<Object>> dadosExcel, String nomeColuna) {

        String cabecalho = dadosExcel.get(0).toString();
        if (cabecalho.length() > 0 && cabecalho.charAt(0) == '\uFEFF') {
            cabecalho = cabecalho.substring(1);
        }
        String[] colunas = cabecalho.split(",");

        for (int i = 0; i < colunas.length; i++) {
            String nomeAtual = colunas[i].trim();
            if (nomeAtual.equalsIgnoreCase(nomeColuna)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Coluna '" + nomeColuna + "' não encontrada no cabeçalho.");
    }

    public void inserirDadosComTratamento(List<List<Object>> dadosExcel, Connection conexao, BancoOperacoes bancoOperacoes) throws SQLException {
        if (conexao == null) {
            throw new SQLException("Conexão com o banco de dados não foi estabelecida.");
        }

        String query = "INSERT INTO baseCensoIBGE (fkCidade, area, densidadeDemografica) VALUES (?, ?, ?)";

        try (PreparedStatement guardarValor = conexao.prepareStatement(query)) {

            loggerInsercoes.gerarLog("💻 Iniciando inserção de dados na tabela censoIBGE... 💻");

            int indiceMunicipio = obterIndiceColuna(dadosExcel, "Município");
            int indiceDensidadeDemografica = obterIndiceColuna(dadosExcel, "Densidade demográfica(hab/km²)");
            int indiceArea = obterIndiceColuna(dadosExcel, "Área(km²)");

            for (int i = 1; i < dadosExcel.size(); i++) {
                List<Object> linha = dadosExcel.get(i);

                if (linha.size() >= 3 && linha.get(0) != null && linha.get(1) != null && linha.get(2) != null) {

                    setCidade(linha.get(indiceMunicipio).toString().trim());
                    setArea(Double.parseDouble(linha.get(indiceArea).toString()));
                    setDensidadeDemografica(Double.parseDouble(linha.get(indiceDensidadeDemografica).toString()));

                    guardarValor.setString(1, getCidade());
                    guardarValor.setDouble(2, getArea());
                    guardarValor.setDouble(3, getDensidadeDemografica());

                    guardarValor.addBatch();

                    if (i % 5000 == 0) {
                        guardarValor.executeBatch();
                        conexao.commit();
                    }
                }
            }

            guardarValor.executeBatch();
            conexao.commit();
        } catch (SQLException e) {
            conexao.rollback();
            loggerInsercoes.gerarLog("❌ Erro ao inserir dados em CENSO: " + e.getMessage() + " - revertendo... ❌");
            throw e;
        }
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public Double getArea() {
        return area;
    }

    public void setArea(Double area) {
        this.area = area;
    }

    public Double getDensidadeDemografica() {
        return densidadeDemografica;
    }

    public void setDensidadeDemografica(Double densidadeDemografica) {
        this.densidadeDemografica = densidadeDemografica;
    }

    public Logger getLoggerInsercoes() {
        return loggerInsercoes;
    }

    public void setLoggerInsercoes(Logger loggerInsercoes) {
        this.loggerInsercoes = loggerInsercoes;
    }
}
