package application.basedados;

import application.BaseDeDados;
import infrastructure.database.BancoOperacoes;
import infrastructure.logging.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Municipio extends BaseDeDados {

    private String ano;
    private String cidade;
    private String operadora;
    private Double domiciliosCobertosPorcentagem;
    private Double areaCobertaPorcentagem;
    private String tecnologia;

    private int linhasInseridas = 0;
    private int linhasRemovidas = 0;

    public Municipio(Logger logger) {
        this.logger = logger;
    }

    public void inserirDadosComTratamento(List<List<Object>> dadosExcel, Connection conexao, BancoOperacoes bancoDeDados) throws SQLException, ClassNotFoundException {
        bancoDeDados.validarConexao();
        bancoDeDados.truncarTabela("baseMunicipio");

        System.out.println("Inserindo dados...");
        logger.getLoggerEventos().gerarLog("💻 Iniciando inserção de dados na tabela municipio... 💻");

        String query = "INSERT INTO baseMunicipio (ano, fkCidade, operadora, domiciliosCobertosPercentual, areaCobertaPercentual, tecnologia) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = conexao.prepareStatement(query)) {
            processarEInserirDados(dadosExcel, preparedStatement, bancoDeDados);
            preparedStatement.executeBatch();
            conexao.commit();
            System.out.println("Linhas inseridas: " + linhasInseridas);
            System.out.println("Linhas removidas: " + linhasRemovidas);
            System.out.println("Inserção da baseMunícipio concluída com sucesso!");
        }
    }

    public void processarEInserirDados(List<List<Object>> dadosExcel, PreparedStatement preparedStatement, BancoOperacoes bancoDeDados) throws SQLException {
        Map<String, Integer> indiceColunas = new HashMap<>();
        indiceColunas.put("Ano", obterIndiceColuna(dadosExcel, "Ano"));
        indiceColunas.put("Cidade", obterIndiceColuna(dadosExcel, "Município"));
        indiceColunas.put("Operadora", obterIndiceColuna(dadosExcel, "Operadora"));
        indiceColunas.put("DomiciliosCobertos", obterIndiceColuna(dadosExcel, "% domicílios cobertos"));
        indiceColunas.put("AreaCoberta", obterIndiceColuna(dadosExcel, "% área coberta"));
        indiceColunas.put("Tecnologia", obterIndiceColuna(dadosExcel, "Tecnologia"));

        for (int i = 1; i < dadosExcel.size(); i++) {
            List<Object> linha = dadosExcel.get(i);
            String[] valores = processarLinha(linha);

            if (!extrairValoresDoMunicipio(preparedStatement, valores, linha, indiceColunas)) {
                linhasRemovidas++;
                continue;
            }
            bancoDeDados.adicionarBatch(preparedStatement, i);
            linhasInseridas++;
        }
    }

    public int obterIndiceColuna(List<List<Object>> dadosExcel, String nomeColuna) {
        String cabecalho = dadosExcel.get(0).get(0).toString();
        if (cabecalho.length() > 0 && cabecalho.charAt(0) == '\uFEFF') {
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

    private boolean extrairValoresDoMunicipio(PreparedStatement preparedStatement, String[] valores, List<Object> linha, Map<String, Integer> indiceColunas) throws SQLException {
        if (valores.length < 16) {
            return false;
        }

        setAno(valores[0]);
        String cidade = formatarCidade(valores[5]);
        if (cidade.contains("�?")) {
            return false;
        }

        if (cidade.charAt(0) == '"') {
            cidade = cidade.substring(1);
        }
        setCidade(cidade);
        setOperadora(valores[2]);
        String tecnologiasFormatadas = formatarTecnologias(valores[3]);
        setTecnologia(tecnologiasFormatadas);

        if (!verificarCidadeExistente(cidade, preparedStatement.getConnection())) {
            return false;
        }

        Double areaCobertaPercent = combinarValoresComoDouble(valores[12], valores[13]);
        if (areaCobertaPercent != null) {
            setAreaCobertaPorcentagem(areaCobertaPercent);
        }

        Double domiciliosCobertosPercent = combinarValoresComoDouble(valores[14], valores[15]);
        if (domiciliosCobertosPercent != null) {
            setDomiciliosCobertosPorcentagem(domiciliosCobertosPercent);
        }

        if (areaCobertaPercent == null || domiciliosCobertosPercent == null) {
            return false;
        }

        guardarValorParaOBanco(preparedStatement);

        return true;
    }
    private boolean verificarCidadeExistente(String cidade, Connection conexao) {
        String query = "SELECT COUNT(*) FROM cidade WHERE nomeCidade = ?";
        try (PreparedStatement stmt = conexao.prepareStatement(query)) {
            stmt.setString(1, cidade);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
        }
        return false;
    }
    private String formatarCidade(String cidade) {
        if (cidade != null && cidade.contains("-")) {
            return cidade.split("-")[0].trim();
        }
        return cidade;
    }

    private String formatarTecnologias(String tecnologia) {
        return tecnologia.replaceAll("(\\dG)(?=\\dG)", "$1, ");
    }

    private Double combinarValoresComoDouble(String parteInteira, String parteDecimal) {
        try {
            if (parteInteira == null || parteDecimal == null || parteInteira.isEmpty() || parteDecimal.isEmpty()) {
                return null;
            }
            String parteDecimalTruncada;

            if (parteDecimal.contains("E")) {
                String parteDecimalSemVirgula = parteDecimal.replace(",", "").replace(".", "");
                parteDecimalTruncada = parteDecimalSemVirgula.substring(0, 2);
            } else {
                parteDecimalTruncada = parteDecimal.length() > 2 ? parteDecimal.substring(0, 2) : parteDecimal;
            }

            String valorFormatado = String.format("%s.%s", parteInteira, parteDecimalTruncada);

            return Double.parseDouble(valorFormatado);
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            return null;
        }
    }

    public void guardarValorParaOBanco(PreparedStatement preparedStatement) throws SQLException {
        preparedStatement.setString(1, getAno());
        preparedStatement.setString(2, getCidade());
        preparedStatement.setString(3, getOperadora());
        preparedStatement.setDouble(4, getAreaCobertaPorcentagem());
        preparedStatement.setDouble(5, getDomiciliosCobertosPorcentagem());
        preparedStatement.setString(6, getTecnologia());
    }

    public String getAno() {
        return ano;
    }

    public void setAno(String ano) {
        this.ano = ano;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) { this.cidade = cidade; }

    public String getOperadora() {
        return operadora;
    }

    public void setOperadora(String operadora) {
        this.operadora = operadora;
    }

    public Double getDomiciliosCobertosPorcentagem() {
        return domiciliosCobertosPorcentagem;
    }

    public void setDomiciliosCobertosPorcentagem(Double domiciliosCobertosPorcentagem) { this.domiciliosCobertosPorcentagem = domiciliosCobertosPorcentagem; }

    public Double getAreaCobertaPorcentagem() {
        return areaCobertaPorcentagem;
    }

    public void setAreaCobertaPorcentagem(Double areaCobertaPorcentagem) { this.areaCobertaPorcentagem = areaCobertaPorcentagem; }

    public String getTecnologia() {
        return tecnologia;
    }

    public void setTecnologia(String tecnologia) {
        this.tecnologia = tecnologia;
    }
}
