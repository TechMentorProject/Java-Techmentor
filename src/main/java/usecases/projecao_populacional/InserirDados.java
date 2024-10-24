package usecases.projecao_populacional;

import domain.ProjecaoPopulacional;
import infrastructure.database.BancoOperacoes;
import infrastructure.utils.ValidacoesLinha;

import java.awt.*;
import java.sql.*;
import java.util.List;
import java.util.stream.Collectors;

public class InserirDados {

    ValidacoesLinha validacoesLinha = new ValidacoesLinha();
    ProjecaoPopulacional projecao = new ProjecaoPopulacional();

    public void inserirDadosComTratamento(List<List<Object>> dadosExcel, Connection conexao, BancoOperacoes bancoDeDados) throws SQLException {
        bancoDeDados.validarConexao();

        bancoDeDados.truncarTabela("projecaoPopulacional");

        System.out.println("Inserindo dados...");

        String query = "INSERT INTO projecaoPopulacional (estado, idade, projecao_2024, projecao_2025, projecao_2026, projecao_2027, projecao_2028) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = conexao.prepareStatement(query)) {
            processarEInserirDados(dadosExcel, preparedStatement, bancoDeDados);
            preparedStatement.executeBatch();
            conexao.commit();
            System.out.println("Dados inseridos com sucesso!");
        }
    }

    private int obterIndiceColuna(List<List<Object>> dadosExcel, String nomeColuna) {

        String cabecalho = dadosExcel.get(4).toString(); // Obter a string do cabeçalho

        cabecalho = cabecalho.replace("[", "").replace("]", "");

        // Remover o BOM (Byte Order Mark) da primeira célula do cabeçalho
        if (cabecalho.length() > 0 && cabecalho.charAt(0) == '\uFEFF') {
            cabecalho = cabecalho.substring(1); // Remove o Byte Order Mark
        }

        // Fazer o split da string com base no delimitador ";"
        String[] colunas = cabecalho.split(",");
        // Itera sobre as colunas buscando o nome correspondente
        for (int i = 0; i < colunas.length; i++) {
            String nomeAtual = colunas[i].trim(); // Remover espaços em branco ao redor

            if (nomeAtual.equalsIgnoreCase(nomeColuna)) {
                return i; // Retorna o índice correspondente ao nome da coluna
            }
        }

        throw new IllegalArgumentException("Coluna '" + nomeColuna + "' não encontrada no cabeçalho.");
    }

    // Processar e inserir os dados no banco (semelhante ao `processarEInserirDados`)
    private void processarEInserirDados(List<List<Object>> dadosExcel, PreparedStatement preparedStatement, BancoOperacoes bancoDeDados) throws SQLException {

        for (int i = 0; i < dadosExcel.size(); i++) {
            List<Object> linha = dadosExcel.get(i);
            String[] valores = validacoesLinha.processarLinha(linha);

            // Verifica a validade dos dados e processa
            if (!extraindoValoresDaProjecao(preparedStatement, valores, linha, dadosExcel)) {
                continue;
            }

            bancoDeDados.adicionarBatch(preparedStatement, i);
        }
    }

    // Extrair e validar valores da projeção (semelhante ao `extraindoValoresDoApache`)
    private boolean extraindoValoresDaProjecao(PreparedStatement preparedStatement, String[] valores, List<Object> linha, List<List<Object>> dadosExcel) throws SQLException {

        int indiceIdade = obterIndiceColuna(dadosExcel, "idade");
        int indiceEstado = obterIndiceColuna(dadosExcel, "local");
        int indiceProj2024 = obterIndiceColuna(dadosExcel, "2024");
        int indiceProj2025 = obterIndiceColuna(dadosExcel, "2025");
        int indiceProj2026 = obterIndiceColuna(dadosExcel, "2026");
        int indiceProj2027 = obterIndiceColuna(dadosExcel, "2027");
        int indiceProj2028 = obterIndiceColuna(dadosExcel, "2028");

        if (valores.length < 34) {
            return false;
        }
        String estado = validacoesLinha.buscarValorValido(valores, indiceEstado);
        if (estado != null) {
            estado = estado.replace(".", "");
        }

        String idade = validacoesLinha.buscarValorValido(valores, indiceIdade);
        if (idade != null) {
            idade = idade.replace(".", "");
        }

        String ano_2024 = validacoesLinha.buscarValorValido(valores, indiceProj2024);
        if (ano_2024 != null) {
            ano_2024 = ano_2024.replace(".", "");
        }

        String ano_2025 = validacoesLinha.buscarValorValido(valores, indiceProj2025);
        if (ano_2025 != null) {
            ano_2025 = ano_2025.replace(".", "");
        }

        String ano_2026 = validacoesLinha.buscarValorValido(valores, indiceProj2026);
        if (ano_2026 != null) {
            ano_2026 = ano_2026.replace(".", "");
        }

        String ano_2027 = validacoesLinha.buscarValorValido(valores, indiceProj2027);
        if (ano_2027 != null) {
            ano_2027 = ano_2027.replace(".", "");
        }

        String ano_2028 = validacoesLinha.buscarValorValido(valores, indiceProj2028);
        if (ano_2028 != null) {
            ano_2028 = ano_2028.replace(".", "");
        }

        // Ignorar linhas com palavras proibidas
        if (estado != null && contemPalavrasProibidas(estado, linha)) {
            return false;
        }

        // Se algum campo é inválido, ignorar a linha
        if (validacoesLinha.algumCampoInvalido(estado, idade, ano_2024, ano_2025, ano_2026, ano_2027, ano_2028)) {
            return false;
        }

        // Preencher o `PreparedStatement`
        guardarValorProBanco(preparedStatement, estado, idade, ano_2024, ano_2025, ano_2026, ano_2027, ano_2028);
        return true;
    }

    // Método para verificar se há palavras proibidas (similar ao `containsProhibitedWords`)
    private boolean contemPalavrasProibidas(String estado, List<Object> linha) {
        // Converte a linha para String e aplica o filtro
        String linhaLowerCase = linha.stream()
                .map(celula -> celula != null ? celula.toString() : "")  // Converte cada célula para String
                .collect(Collectors.joining(" "))  // Junta os valores com espaço
                .toLowerCase();

        if (estado.equalsIgnoreCase("sul") || estado.equalsIgnoreCase("norte") || estado.equalsIgnoreCase("local")) {
            return true;
        }

        if (linhaLowerCase.contains("brasil") || linhaLowerCase.contains("homens") || linhaLowerCase.contains("mulheres")
                || linhaLowerCase.contains("centro-oeste") || linhaLowerCase.contains("sudeste") || linhaLowerCase.contains("nordeste")) {
            return true;
        }
        return false;
    }

    // Preencher o PreparedStatement (igual ao `guardarValorProBanco`)
    private void guardarValorProBanco(PreparedStatement preparedStatement, String estado, String idade, String ano_2024, String ano_2025, String ano_2026, String ano_2027, String ano_2028) throws SQLException {
        projecao.setEstado(estado);
        projecao.setIdade(Integer.parseInt(idade));
        projecao.setProjecao2024(Integer.parseInt(ano_2024));
        projecao.setProjecao2025(Integer.parseInt(ano_2025));
        projecao.setProjecao2026(Integer.parseInt(ano_2026));
        projecao.setProjecao2027(Integer.parseInt(ano_2027));
        projecao.setProjecao2028(Integer.parseInt(ano_2028));

        preparedStatement.setString(1, projecao.getEstado());
        preparedStatement.setInt(2, projecao.getIdade());
        preparedStatement.setInt(3, projecao.getProjecao2024());
        preparedStatement.setInt(4, projecao.getProjecao2025());
        preparedStatement.setInt(5, projecao.getProjecao2026());
        preparedStatement.setInt(6, projecao.getProjecao2027());
        preparedStatement.setInt(7, projecao.getProjecao2028());
    }
}
