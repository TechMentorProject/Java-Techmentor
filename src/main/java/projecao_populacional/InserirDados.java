package projecao_populacional;

import geral.BancoOperacoes;
import geral.ValidacoesLinha;

import java.sql.*;
import java.util.List;
import java.util.stream.Collectors;

public class InserirDados {

    ValidacoesLinha validacoesLinha = new ValidacoesLinha();
    // Inserir dados com tratamento (similar ao `inserirDadosComTratamento`)
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

    // Processar e inserir os dados no banco (semelhante ao `processarEInserirDados`)
    private void processarEInserirDados(List<List<Object>> dadosExcel, PreparedStatement preparedStatement, BancoOperacoes bancoDeDados) throws SQLException {
        int linhasIgnoradas = 0;
        int linhasInseridas = 0;

        for (int i = 0; i < dadosExcel.size(); i++) {
            List<Object> linha = dadosExcel.get(i);
            String[] valores = validacoesLinha.processarLinha(linha);

            // Verifica a validade dos dados e processa
            if (!extraindoValoresDaProjecao(preparedStatement, valores, linha)) {
                linhasIgnoradas++;
                continue;
            }

            bancoDeDados.adicionarBatch(preparedStatement, i);
            linhasInseridas++;
        }

        System.out.println("Linhas inseridas: " + linhasInseridas);
        System.out.println("Linhas ignoradas: " + linhasIgnoradas);
    }

    // Extrair e validar valores da projeção (semelhante ao `extraindoValoresDoApache`)
    private boolean extraindoValoresDaProjecao(PreparedStatement preparedStatement, String[] valores, List<Object> linha) throws SQLException {
        if (valores.length < 34) {
            System.out.println("Linha inválida, ignorando: " + linha);
            return false;
        }
        String estado = validacoesLinha.buscarValorValido(valores, 4);
        if (estado != null) {
            estado = estado.replace(".", "");
        }

        String idade = validacoesLinha.buscarValorValido(valores, 0);
        if (idade != null) {
            idade = idade.replace(".", "");
        }

        String ano_2024 = validacoesLinha.buscarValorValido(valores, 29);
        if (ano_2024 != null) {
            ano_2024 = ano_2024.replace(".", "");
        }

        String ano_2025 = validacoesLinha.buscarValorValido(valores, 30);
        if (ano_2025 != null) {
            ano_2025 = ano_2025.replace(".", "");
        }

        String ano_2026 = validacoesLinha.buscarValorValido(valores, 31);
        if (ano_2026 != null) {
            ano_2026 = ano_2026.replace(".", "");
        }

        String ano_2027 = validacoesLinha.buscarValorValido(valores, 32);
        if (ano_2027 != null) {
            ano_2027 = ano_2027.replace(".", "");
        }

        String ano_2028 = validacoesLinha.buscarValorValido(valores, 33);
        if (ano_2028 != null) {
            ano_2028 = ano_2028.replace(".", "");
        }


        // Ignorar linhas com palavras proibidas
        if (estado != null && contemPalavrasProibidas(estado, linha)) {
            return false;
        }

        // Se algum campo é inválido, ignorar a linha
        if (validacoesLinha.algumCampoInvalido(estado, idade, ano_2024, ano_2025, ano_2026, ano_2027, ano_2028)) {
            System.out.println("Linha ignorada por conter campos inválidos: " + linha);
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
            System.out.println("Linha ignorada por ter regiões proibidas: " + linha);
            return true;
        }

        if (linhaLowerCase.contains("brasil") || linhaLowerCase.contains("homens") || linhaLowerCase.contains("mulheres")
                || linhaLowerCase.contains("centro-oeste") || linhaLowerCase.contains("sudeste") || linhaLowerCase.contains("nordeste")) {
            System.out.println("Linha ignorada devido ao filtro de palavras proibidas: " + linha);
            return true;
        }
        return false;
    }

    // Preencher o PreparedStatement (igual ao `guardarValorProBanco`)
    private void guardarValorProBanco(PreparedStatement preparedStatement, String estado, String idade, String ano_2024, String ano_2025, String ano_2026, String ano_2027, String ano_2028) throws SQLException {
        int idadeFormatada = Integer.parseInt(idade);
        int _ano_2024 = Integer.parseInt(ano_2024);
        int _ano_2025 = Integer.parseInt(ano_2025);
        int _ano_2026 = Integer.parseInt(ano_2026);
        int _ano_2027 = Integer.parseInt(ano_2027);
        int _ano_2028 = Integer.parseInt(ano_2028);

        preparedStatement.setString(1, estado);
        preparedStatement.setInt(2, idadeFormatada);
        preparedStatement.setInt(3, _ano_2024);
        preparedStatement.setInt(4, _ano_2025);
        preparedStatement.setInt(5, _ano_2026);
        preparedStatement.setInt(6, _ano_2027);
        preparedStatement.setInt(7, _ano_2028);
    }
}
