package usecases.municipio;

import domain.Municipio;
import infrastructure.database.BancoOperacoes;
import infrastructure.utils.ValidacoesLinha;

import java.sql.*;
import java.util.List;

public class InserirDados {

    ValidacoesLinha validadacoesLinha = new ValidacoesLinha();
    Municipio municipio = new Municipio();

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
    private int obterIndiceColuna(List<List<Object>> dadosExcel, String nomeColuna) {

        String cabecalho = dadosExcel.get(0).get(0).toString(); // Obter a string do cabeçalho

        // Remover o BOM (Byte Order Mark) da primeira célula do cabeçalho
        if (cabecalho.length() > 0 && cabecalho.charAt(0) == '\uFEFF') {
            cabecalho = cabecalho.substring(1); // Remove o Byte Order Mark
        }

        // Fazer o split da string com base no delimitador ";"
        String[] colunas = cabecalho.split(";");

        // Itera sobre as colunas buscando o nome correspondente
        for (int i = 0; i < colunas.length; i++) {
            String nomeAtual = colunas[i].trim(); // Remover espaços em branco ao redor

            if (nomeAtual.equalsIgnoreCase(nomeColuna)) {
                return i; // Retorna o índice correspondente ao nome da coluna
            }
        }
        throw new IllegalArgumentException("Coluna '" + nomeColuna + "' não encontrada no cabeçalho.");
    }

    private void processarEInserirDados(List<List<Object>> dadosExcel, PreparedStatement preparedStatement, BancoOperacoes bancoDeDados) throws SQLException {
        for (int i = 1; i < dadosExcel.size(); i++) {  // Ignora a primeira linha (cabeçalho)
            List<Object> linha = dadosExcel.get(i);

            String[] valores = validadacoesLinha.processarLinha(linha);

            if (!extraindoValoresDoMunicipio(preparedStatement, valores, linha, dadosExcel)) {
                continue;
            }
            bancoDeDados.adicionarBatch(preparedStatement, i);
        }
    }

    private boolean extraindoValoresDoMunicipio(PreparedStatement preparedStatement, String[] valores, List<Object> linha, List<List<Object>> dadosExcel) throws SQLException {
        if (valores.length < 13) {
            return false;
        }

        int indiceAno = obterIndiceColuna(dadosExcel, "Ano");
        int indiceCidade = obterIndiceColuna(dadosExcel, "Município");
        int indiceOperadora = obterIndiceColuna(dadosExcel, "Operadora");
        int indiceDomiciliosCobertos = obterIndiceColuna(dadosExcel, "% domicílios cobertos");
        int indiceAreaCoberta = obterIndiceColuna(dadosExcel, "% área coberta");
        int indiceTecnologia = obterIndiceColuna(dadosExcel, "Tecnologia");

        municipio.setAno(validadacoesLinha.buscarValorValido(valores, indiceAno));
        municipio.setCidade(validadacoesLinha.buscarValorValido(valores, indiceCidade));
        municipio.setOperadora(validadacoesLinha.buscarValorValido(valores, indiceOperadora));

        String domiciliosCobertosPercentBruto = validadacoesLinha.buscarValorValido(valores, indiceDomiciliosCobertos - 1);

        if (domiciliosCobertosPercentBruto != null) {
            municipio.setDomiciliosCobertosPorcentagem(Integer.parseInt(domiciliosCobertosPercentBruto));
        }

        String areaCobertaPercent = validadacoesLinha.buscarValorValido(valores, indiceAreaCoberta - 1);

        String areaCobertaFormatada = formatarAreaCoberta(areaCobertaPercent);

        if (areaCobertaFormatada != null) {
            municipio.setAreaCobertaPorcentagem(Integer.parseInt(areaCobertaFormatada));
        }

        String tecnologiaFormatada = formatarTecnologia(validadacoesLinha.buscarValorValido(valores, indiceTecnologia));
        municipio.setTecnologia(tecnologiaFormatada);

        if (validadacoesLinha.algumCampoInvalido(municipio.getAno(), municipio.getCidade(), municipio.getOperadora(), municipio.getDomiciliosCobertosPorcentagem(), municipio.getAreaCobertaPorcentagem(), municipio.getTecnologia())) {
            return false;
        }

        if (municipio.getAreaCobertaPorcentagem() == 0 || municipio.getDomiciliosCobertosPorcentagem() == 0) {
            return false;
        }

        // Preencher o `PreparedStatement`
        guardarValorProBanco(preparedStatement, municipio.getAno(), municipio.getCidade(), municipio.getOperadora(),
                municipio.getDomiciliosCobertosPorcentagem(), municipio.getAreaCobertaPorcentagem(),
                municipio.getTecnologia());

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
    private void guardarValorProBanco(PreparedStatement guardarValor, String ano, String cidade, String operadora, Integer domiciliosCobertosPercent, Integer areaCobertaFormatada, String tecnologiaFormatada) throws SQLException {
        guardarValor.setString(1, ano);
        guardarValor.setString(2, cidade);
        guardarValor.setString(3, operadora);
        guardarValor.setInt(4, domiciliosCobertosPercent);
        guardarValor.setInt(5, areaCobertaFormatada);
        guardarValor.setString(6, tecnologiaFormatada);
    }

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
