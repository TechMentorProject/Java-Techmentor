package projecao_populacional;

import java.sql.*;
import java.util.List;
import java.util.stream.Collectors;

public class BancoDeDados {

    private Connection conexao;

    // Conectar ao banco
    public void conectar() throws ClassNotFoundException, SQLException {
        System.out.println("Conectando no banco...");
        Class.forName("com.mysql.cj.jdbc.Driver");
        conexao = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/techmentor",
                "root",
                "root"
        );
        conexao.setAutoCommit(false);  // Controle de transação manual
        System.out.println("Banco conectado.");
    }

    // Fechar a conexão
    public void fecharConexao() throws SQLException {
        if (conexao != null && !conexao.isClosed()) {
            conexao.close();
            System.out.println("Conexão fechada.");
        } else {
            System.out.println("A conexão já está fechada ou é nula.");
        }
    }

    // Método para validar a conexão
    private void validarConexao() throws SQLException {
        if (conexao == null) {
            throw new SQLException("Conexão com o banco de dados não foi estabelecida.");
        }
    }

    // Inserir dados com tratamento (similar ao `inserirDadosComTratamento`)
    public void inserirDadosComTratamento(List<List<Object>> dadosExcel) throws SQLException {
        validarConexao();

        truncarTabela();

        System.out.println("Inserindo dados...");

        String query = "INSERT INTO projecaoPopulacional (estado, idade, projecao_2024, projecao_2025, projecao_2026, projecao_2027, projecao_2028) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = conexao.prepareStatement(query)) {
            processarEInserirDados(dadosExcel, preparedStatement);
            preparedStatement.executeBatch();
            conexao.commit();
            System.out.println("Dados inseridos com sucesso!");
        }
    }

    // Processar e inserir os dados no banco (semelhante ao `processarEInserirDados`)
    private void processarEInserirDados(List<List<Object>> dadosExcel, PreparedStatement preparedStatement) throws SQLException {
        int linhasIgnoradas = 0;
        int linhasInseridas = 0;

        for (int i = 0; i < dadosExcel.size(); i++) {
            List<Object> linha = dadosExcel.get(i);
            String[] valores = processarLinha(linha);

            // Verifica a validade dos dados e processa
            if (!extraindoValoresDaProjecao(preparedStatement, valores, linha)) {
                linhasIgnoradas++;
                continue;
            }

            adicionarBatch(preparedStatement, i);
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
        String estado = buscarValorValido(valores, 4);
        if (estado != null) {
            estado = estado.replace(".", "");
        }

        String idade = buscarValorValido(valores, 0);
        if (idade != null) {
            idade = idade.replace(".", "");
        }

        String ano_2024 = buscarValorValido(valores, 29);
        if (ano_2024 != null) {
            ano_2024 = ano_2024.replace(".", "");
        }

        String ano_2025 = buscarValorValido(valores, 30);
        if (ano_2025 != null) {
            ano_2025 = ano_2025.replace(".", "");
        }

        String ano_2026 = buscarValorValido(valores, 31);
        if (ano_2026 != null) {
            ano_2026 = ano_2026.replace(".", "");
        }

        String ano_2027 = buscarValorValido(valores, 32);
        if (ano_2027 != null) {
            ano_2027 = ano_2027.replace(".", "");
        }

        String ano_2028 = buscarValorValido(valores, 33);
        if (ano_2028 != null) {
            ano_2028 = ano_2028.replace(".", "");
        }


        // Ignorar linhas com palavras proibidas
        if (estado != null && contemPalavrasProibidas(estado, linha)) {
            return false;
        }

        // Se algum campo é inválido, ignorar a linha
        if (algumCampoInvalido(estado, idade, ano_2024, ano_2025, ano_2026, ano_2027, ano_2028)) {
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

    // Adicionar ao batch e executar periodicamente (igual ao `adicionarBatch`)
    private void adicionarBatch(PreparedStatement preparedStatement, int linhaAtual) throws SQLException {
        if (linhaAtual % 5000 == 0) {
            preparedStatement.executeBatch();
            conexao.commit();
        }
        preparedStatement.addBatch();
    }

    // Truncar a tabela (igual ao `truncarTabela`)
    private void truncarTabela() throws SQLException {
        System.out.println("Truncando a tabela projecaoPopulacional...");
        try (Statement statement = conexao.createStatement()) {
            String truncateQuery = "TRUNCATE TABLE projecaoPopulacional";
            statement.executeUpdate(truncateQuery);
            System.out.println("Tabela truncada com sucesso!");
        }
    }

    // Método auxiliar para verificar campos inválidos
    private boolean algumCampoInvalido(Object... campos) {
        for (Object campo : campos) {
            if (campo == null) {
                return true; // Se o campo for null, é inválido
            }
            if (campo instanceof String && ((String) campo).isEmpty()) {
                return true; // Se for uma String vazia, é inválido
            }
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
        preparedStatement.setInt(2,idadeFormatada);
        preparedStatement.setInt(3, _ano_2024);
        preparedStatement.setInt(4, _ano_2025);
        preparedStatement.setInt(5, _ano_2026);
        preparedStatement.setInt(6, _ano_2027);
        preparedStatement.setInt(7, _ano_2028);
    }

    // Processar a linha e convertê-la para String (igual ao `processarLinha`)
    private String[] processarLinha(List<Object> linha) {
        String linhaConvertida = buscarValorValido(linha);
        return linhaConvertida.split(";");
    }

    // Converte uma lista de objetos para uma string com separador ";"
    private String buscarValorValido(List<Object> row) {
        StringBuilder linha = new StringBuilder();
        for (Object celula : row) {
            if (!linha.isEmpty()) {
                linha.append(";");
            }
            linha.append(celula != null ? celula.toString() : "");
        }
        return linha.toString();
    }

    // Retorna um valor seguro de um array (igual ao `buscarValorValido`)
    private String buscarValorValido(String[] valores, int index) {
        if (index < valores.length) {
            String valor = valores[index];
            if (!valor.isEmpty()) {
                return valor;
            }
        }
        return null;
    }
}