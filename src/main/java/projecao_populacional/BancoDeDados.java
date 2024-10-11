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
        conexao = DriverManager.getConnection("jdbc:mysql://localhost:3306/techmentor?useUnicode=true&characterEncoding=UTF-8", "root", "root");
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
        String idade = buscarValorValido(valores, 0);
        String ano_2024 = buscarValorValido(valores, 29);
        String ano_2025 = buscarValorValido(valores, 30);
        String ano_2026 = buscarValorValido(valores, 31);
        String ano_2027 = buscarValorValido(valores, 32);
        String ano_2028 = buscarValorValido(valores, 33);

        // Ignorar linhas com palavras proibidas
        if (contémPalavrasProibidas(estado, idade, linha)) {
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
    private boolean contémPalavrasProibidas(String estado, String idade, List<Object> linha) {
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

    // Verificar se algum campo é inválido (igual ao `algumCampoInvalido`)
    private boolean algumCampoInvalido(String... campos) {
        for (String campo : campos) {
            if (campo == null || campo.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    // Preencher o PreparedStatement (igual ao `guardarValorProBanco`)
    private void guardarValorProBanco(PreparedStatement preparedStatement, String estado, String idade, String ano_2024, String ano_2025, String ano_2026, String ano_2027, String ano_2028) throws SQLException {
        preparedStatement.setString(1, estado);
        preparedStatement.setString(2, idade);
        preparedStatement.setString(3, ano_2024);
        preparedStatement.setString(4, ano_2025);
        preparedStatement.setString(5, ano_2026);
        preparedStatement.setString(6, ano_2027);
        preparedStatement.setString(7, ano_2028);
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
            if (linha.length() > 0) {
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
