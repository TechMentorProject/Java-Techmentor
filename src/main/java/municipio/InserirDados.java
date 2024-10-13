package municipio;

import geral.BancoOperacoes;
import geral.ValidacoesLinha;

import java.sql.*;
import java.util.List;

public class InserirDados {

    ValidacoesLinha validadacoesLinha = new ValidacoesLinha();

    // Método para inserir dados com tratamento (semelhante ao `inserirDadosComTratamento`)
    public void inserirDadosComTratamento(List<List<Object>> dadosExcel, Connection conexao, BancoOperacoes bancoDeDados) throws SQLException {
        bancoDeDados.validarConexao();

        bancoDeDados.truncarTabela("municipio");

        System.out.println("Inserindo dados...");

        String query = "INSERT INTO municipio (ano, cidade, operadora, domiciliosCobertosPorcentagem, areaCobertaPorcentagem, tecnologia) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = conexao.prepareStatement(query)) {
            processarEInserirDados(dadosExcel, preparedStatement, bancoDeDados);
            preparedStatement.executeBatch();
            conexao.commit();
            System.out.println("Dados inseridos com sucesso!");
        }
    }

    // Método para processar e inserir dados (semelhante ao `processarEInserirDados`)
    private void processarEInserirDados(List<List<Object>> dadosExcel, PreparedStatement preparedStatement, BancoOperacoes bancoDeDados) throws SQLException {
        for (int i = 1; i < dadosExcel.size(); i++) {  // Ignora a primeira linha (cabeçalho)
            List<Object> linha = dadosExcel.get(i);
            String[] valores = validadacoesLinha.processarLinha(linha);

            // Se a linha não for válida, pula para a próxima
            if (!extraindoValoresDoMunicipio(preparedStatement, valores, linha)) {
                continue;
            }
            bancoDeDados.adicionarBatch(preparedStatement, i);
        }
    }

    // Método para extrair e validar os valores de cada linha (similar a `extraindoValoresDoApache`)
    private boolean extraindoValoresDoMunicipio(PreparedStatement preparedStatement, String[] valores, List<Object> linha) throws SQLException {
        if (valores.length < 13) {
            System.err.println("Linha inválida, ignorando: " + linha);
            return false;
        }

        // Extração e validação de valores
        String ano = validadacoesLinha.buscarValorValido(valores, 0);
        String cidade = validadacoesLinha.buscarValorValido(valores, 5);
        String operadora = validadacoesLinha.buscarValorValido(valores, 2);
        String domiciliosCobertosPercentBruto = validadacoesLinha.buscarValorValido(valores, 10);


        Double domiciliosCobertosPercent = null;
        if (domiciliosCobertosPercentBruto != null) {
            domiciliosCobertosPercent = Double.parseDouble(domiciliosCobertosPercentBruto);
        }

        String areaCobertaPercent = validadacoesLinha.buscarValorValido(valores, 11);

        // Formatações específicas
        String areaCobertaFormatada = formatarAreaCoberta(areaCobertaPercent);
        Double areaCoberta = null;
        if (areaCobertaFormatada != null) {
            areaCoberta = Double.parseDouble(areaCobertaFormatada);
        }
        String tecnologiaFormatada = formatarTecnologia(validadacoesLinha.buscarValorValido(valores, 3));

//         Verifica se algum campo essencial é inválido
        if (validadacoesLinha.algumCampoInvalido(ano, cidade, operadora, domiciliosCobertosPercent, areaCobertaPercent, tecnologiaFormatada)) {
            return false;
        }

        // Ignora se % coberto é zero
        if ("0".equals(areaCobertaPercent)) {
            return false;
        }

        // Preencher o `PreparedStatement`
        guardarValorProBanco(preparedStatement, ano, cidade, operadora, domiciliosCobertosPercent, areaCoberta, tecnologiaFormatada);
        return true;
    }

    // Método auxiliar para formatar a área coberta
    private String formatarAreaCoberta(String areaCobertaPercent) {
        if (areaCobertaPercent != null && areaCobertaPercent.length() >= 2) {
            return areaCobertaPercent.substring(0, 2);
        }
        return areaCobertaPercent;
    }

    // Método auxiliar para preencher o `PreparedStatement` (igual ao `guardarValorProBanco`)
    private void guardarValorProBanco(PreparedStatement guardarValor, String ano, String cidade, String operadora, Double domiciliosCobertosPercent, Double areaCobertaFormatada, String tecnologiaFormatada) throws SQLException {
        guardarValor.setString(1, ano);
        guardarValor.setString(2, cidade);
        guardarValor.setString(3, operadora);
        guardarValor.setDouble(4, domiciliosCobertosPercent);
        guardarValor.setDouble(5, areaCobertaFormatada);
        guardarValor.setString(6, tecnologiaFormatada);
    }


    // Método auxiliar para formatar a tecnologia (igual ao `formatarTecnologia`)
    private String formatarTecnologia(String tecnologia) {
        if (tecnologia == null || tecnologia.isEmpty() || tecnologia.equalsIgnoreCase("Todas")) {
            return "2G, 3G, 4G, 5G";  // Se for "Todas", retorna todas as tecnologias
        }

        String[] possiveisTecnologias = {"2G", "3G", "4G", "5G"};
        StringBuilder tecnologiasFormatadas = new StringBuilder();

        for (String tech : possiveisTecnologias) {
            if (tecnologia.toUpperCase().contains(tech.toUpperCase())) {
                if (!tecnologiasFormatadas.isEmpty()) {
                    tecnologiasFormatadas.append(", ");
                }
                tecnologiasFormatadas.append(tech);
            }
        }
        return tecnologiasFormatadas.toString().isEmpty() ? null : tecnologiasFormatadas.toString();
    }
}
