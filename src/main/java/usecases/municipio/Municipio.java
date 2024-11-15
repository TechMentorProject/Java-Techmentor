package usecases.municipio;

import infrastructure.database.BancoOperacoes;
import infrastructure.logging.Logger;
import infrastructure.utils.ValidacoesLinha;
import usecases.BaseDeDados;

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
    private ValidacoesLinha validacoesLinha;
    private Logger loggerInsercoes;

    public Municipio(ValidacoesLinha validacoesLinha, Logger loggerInsercoes) {
        this.validacoesLinha = validacoesLinha;
        this.loggerInsercoes = loggerInsercoes;
    }

    public void inserirDadosComTratamento(List<List<Object>> dadosExcel, Connection conexao, BancoOperacoes bancoDeDados) throws SQLException, ClassNotFoundException {
        bancoDeDados.validarConexao();
        bancoDeDados.truncarTabela("municipio");

        System.out.println("Inserindo dados...");
        loggerInsercoes.gerarLog("💻 Iniciando inserção de dados na tabela municipio... 💻");

        String query = "INSERT INTO municipio (ano, fkCidade, operadora, domiciliosCobertosPercent, areaCobertaPercent, tecnologia) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = conexao.prepareStatement(query)) {
            processarEInserirDados(dadosExcel, preparedStatement, bancoDeDados);
            preparedStatement.executeBatch();
            conexao.commit();
            System.out.println("Dados inseridos com sucesso!");
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
            String[] valores = validacoesLinha.processarLinha(linha);

            if (!extraindoValoresDoMunicipio(preparedStatement, valores, linha, indiceColunas)) {
                continue;
            }
            bancoDeDados.adicionarBatch(preparedStatement, i);
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

    private boolean extraindoValoresDoMunicipio(PreparedStatement preparedStatement, String[] valores, List<Object> linha, Map<String, Integer> indiceColunas) throws SQLException {
        if (valores.length < 16) {
            return false;
        }

        setAno(valores[0]);
        String cidade = formatarCidade(valores[5]);
        if (cidade.contains("�?")) {
            System.out.println("Cidade contém o caractere '?' e será ignorada: " + cidade);
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
            System.err.println("Erro de FK: Cidade não encontrada na tabela 'cidade' -> " + cidade);
            loggerInsercoes.gerarLog("Erro de FK: Cidade não encontrada na tabela 'cidade' -> " + cidade);
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

        guardarValorProBanco(preparedStatement, getAno(), getCidade(), getOperadora(),
                domiciliosCobertosPercent, areaCobertaPercent, getTecnologia());

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
            System.err.println("Erro ao verificar a cidade: " + e.getMessage());
            loggerInsercoes.gerarLog("Erro ao verificar a cidade: " + e.getMessage());
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

    private void guardarValorProBanco(PreparedStatement guardarValor, String ano, String cidade, String operadora,
                                      Double domiciliosCobertosPercent, Double areaCobertaPercent, String tecnologia) throws SQLException {
        guardarValor.setString(1, ano);
        guardarValor.setString(2, cidade);
        guardarValor.setString(3, operadora);
        guardarValor.setDouble(4, areaCobertaPercent);
        guardarValor.setDouble(5, domiciliosCobertosPercent);
        guardarValor.setString(6, tecnologia);
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

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public String getOperadora() {
        return operadora;
    }

    public void setOperadora(String operadora) {
        this.operadora = operadora;
    }

    public Double getDomiciliosCobertosPorcentagem() {
        return domiciliosCobertosPorcentagem;
    }

    public void setDomiciliosCobertosPorcentagem(Double domiciliosCobertosPorcentagem) {
        this.domiciliosCobertosPorcentagem = domiciliosCobertosPorcentagem;
    }

    public Double getAreaCobertaPorcentagem() {
        return areaCobertaPorcentagem;
    }

    public void setAreaCobertaPorcentagem(Double areaCobertaPorcentagem) {
        this.areaCobertaPorcentagem = areaCobertaPorcentagem;
    }

    public String getTecnologia() {
        return tecnologia;
    }

    public void setTecnologia(String tecnologia) {
        this.tecnologia = tecnologia;
    }
}
