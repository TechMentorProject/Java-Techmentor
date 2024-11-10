package usecases.municipio;

import domain.Municipio;
import infrastructure.database.BancoOperacoes;
import infrastructure.logging.Logger;
import infrastructure.utils.ValidacoesLinha;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InserirDados {

    ValidacoesLinha validacoesLinha = new ValidacoesLinha();
    Municipio municipio = new Municipio();
    Logger loggerInsercoes = Logger.getLoggerInsercoes();

    public void inserirDadosComTratamento(List<List<Object>> dadosExcel, Connection conexao, BancoOperacoes bancoDeDados) throws SQLException, ClassNotFoundException {
        bancoDeDados.validarConexao();
        bancoDeDados.truncarTabela("municipio");

        System.out.println("Inserindo dados...");
//        loggerInsercoes.gerarLog("💻 Iniciando inserção de dados na tabela municipio... 💻");

        String query = "INSERT INTO municipio (ano, fkCidade, operadora, domiciliosCobertosPercent, areaCobertaPercent, tecnologia) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = conexao.prepareStatement(query)) {
            processarEInserirDados(dadosExcel, preparedStatement, bancoDeDados);
            preparedStatement.executeBatch();
            conexao.commit();
            System.out.println("Dados inseridos com sucesso!");
        }
    }

    public void processarEInserirDados(List<List<Object>> dadosExcel, PreparedStatement preparedStatement, BancoOperacoes bancoDeDados) throws SQLException {
        // Cache de índices das colunas para otimizar
        Map<String, Integer> indiceColunas = new HashMap<>();
        indiceColunas.put("Ano", obterIndiceColuna(dadosExcel, "Ano"));
        indiceColunas.put("Cidade", obterIndiceColuna(dadosExcel, "Município"));
        indiceColunas.put("Operadora", obterIndiceColuna(dadosExcel, "Operadora"));
        indiceColunas.put("DomiciliosCobertos", obterIndiceColuna(dadosExcel, "% domicílios cobertos"));
        indiceColunas.put("AreaCoberta", obterIndiceColuna(dadosExcel, "% área coberta"));
        indiceColunas.put("Tecnologia", obterIndiceColuna(dadosExcel, "Tecnologia"));

        for (int i = 1; i < dadosExcel.size(); i++) {  // Ignora o cabeçalho
            List<Object> linha = dadosExcel.get(i);
            String[] valores = validacoesLinha.processarLinha(linha);

            if (!extraindoValoresDoMunicipio(preparedStatement, valores, linha, indiceColunas)) {
                continue;
            }
            bancoDeDados.adicionarBatch(preparedStatement, i);
        }
    }

    private int obterIndiceColuna(List<List<Object>> dadosExcel, String nomeColuna) {
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

    private String formatarCidade(String cidade) {
        if (cidade != null && cidade.contains(" - ")) {
            return cidade.split(" - ")[0].trim();
        }
        return cidade;
    }

    private boolean extraindoValoresDoMunicipio(PreparedStatement preparedStatement, String[] valores, List<Object> linha, Map<String, Integer> indiceColunas) throws SQLException {
        if (valores.length < 13) {
            return false;
        }

        municipio.setAno(validacoesLinha.buscarValorValido(valores, indiceColunas.get("Ano")));

        // Aplica o método formatarCidade para remover o sufixo do estado
        String cidade = formatarCidade(validacoesLinha.buscarValorValido(valores, indiceColunas.get("Cidade")));

        if (cidade != null && cidade.contains("�?")) {
            System.out.println("Cidade com possível erro de codificação detectada: " + cidade);
            return false;  // Pula essa linha
        }


        municipio.setCidade(cidade);

        municipio.setOperadora(validacoesLinha.buscarValorValido(valores, indiceColunas.get("Operadora")));

        String domiciliosCobertosPercentBruto = validacoesLinha.buscarValorValido(valores, indiceColunas.get("DomiciliosCobertos") - 1);
        if (domiciliosCobertosPercentBruto != null) {
            municipio.setDomiciliosCobertosPorcentagem(Integer.parseInt(domiciliosCobertosPercentBruto));
        }

        String areaCobertaPercent = validacoesLinha.buscarValorValido(valores, indiceColunas.get("AreaCoberta") - 1);
        String areaCobertaFormatada = formatarAreaCoberta(areaCobertaPercent);
        if (areaCobertaFormatada != null) {
            municipio.setAreaCobertaPorcentagem(Integer.parseInt(areaCobertaFormatada));
        }

        String tecnologiaFormatada = formatarTecnologia(validacoesLinha.buscarValorValido(valores, indiceColunas.get("Tecnologia")));
        municipio.setTecnologia(tecnologiaFormatada);

        if (validacoesLinha.algumCampoInvalido(municipio.getAno(), municipio.getCidade(), municipio.getOperadora(),
                municipio.getDomiciliosCobertosPorcentagem(), municipio.getAreaCobertaPorcentagem(), municipio.getTecnologia())) {
            return false;
        }

        if (municipio.getAreaCobertaPorcentagem() == 0 || municipio.getDomiciliosCobertosPorcentagem() == 0) {
            return false;
        }

        guardarValorProBanco(preparedStatement, municipio.getAno(), municipio.getCidade(), municipio.getOperadora(),
                municipio.getDomiciliosCobertosPorcentagem(), municipio.getAreaCobertaPorcentagem(), municipio.getTecnologia());

        return true;
    }



    private String formatarAreaCoberta(String areaCobertaPercent) {
        if (areaCobertaPercent != null && areaCobertaPercent.length() >= 2) {
            return areaCobertaPercent.substring(0, 2);
        }
        return areaCobertaPercent;
    }

    private void guardarValorProBanco(PreparedStatement guardarValor, String ano, String cidade, String operadora, Integer domiciliosCobertosPercent, Integer areaCobertaFormatada, String tecnologiaFormatada) throws SQLException {
        guardarValor.setString(1, ano);
        guardarValor.setString(2, cidade);
        System.out.println(cidade);
        guardarValor.setString(3, operadora);
        guardarValor.setInt(4, domiciliosCobertosPercent);
        guardarValor.setInt(5, areaCobertaFormatada);
        guardarValor.setString(6, tecnologiaFormatada);
    }

    private String formatarTecnologia(String tecnologia) {
        if (tecnologia == null || tecnologia.isEmpty() || tecnologia.equalsIgnoreCase("Todas")) {
            return "2G, 3G, 4G, 5G";
        }

        String[] possiveisTecnologias = {"2G", "3G", "4G", "5G"};
        StringBuilder tecnologiasFormatadas = new StringBuilder();

        for (String tech : possiveisTecnologias) {
            if (tecnologia.toUpperCase().contains(tech.toUpperCase())) {
                if (tecnologiasFormatadas.length() > 0) {
                    tecnologiasFormatadas.append(", ");
                }
                tecnologiasFormatadas.append(tech);
            }
        }
        return tecnologiasFormatadas.length() == 0 ? null : tecnologiasFormatadas.toString();
    }
}
