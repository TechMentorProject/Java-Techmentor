package estacoes_smp;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BancoDeDados {

    private Connection conexao;

    public void conectar() throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        conexao = DriverManager.getConnection("jdbc:mysql://localhost:3306/techmentor", "root", "root");
        conexao.setAutoCommit(false);
        System.out.println("Banco conectado");
    }

    public void fecharConexao() throws SQLException {
        if (conexao != null && !conexao.isClosed()) {
            conexao.close();
            System.out.println("Conexão fechada.");
        } else {
            System.out.println("A conexão já está fechada ou é nula.");
        }
    }

    private void validarConexao() throws SQLException {
        if (conexao == null) {
            throw new SQLException("Conexão com o banco de dados não foi estabelecida.");
        }
    }

    public void inserirDadosComTratamento(List<List<Object>> dadosExcel) throws SQLException {
        validarConexao();

        truncarTabela();

        System.out.println("Inserindo dados...");

        String query = "INSERT INTO estacoesSMP (cidade, operadora, latitude, longitude, codigoIBGE, tecnologia) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = conexao.prepareStatement(query)) {
            processarEInserirDados(dadosExcel, preparedStatement);
            preparedStatement.executeBatch();
            conexao.commit();
            System.out.println("Dados inseridos com sucesso!");
        }
    }

    private void processarEInserirDados(List<List<Object>> dadosExcel, PreparedStatement preparedStatement) throws SQLException {
        for (int i = 1; i < dadosExcel.size(); i++) {
            List<Object> linha = dadosExcel.get(i);
            String[] valores = processarLinha(linha);

            // Se a linha não é válida, pula para a próxima
            if (!extraindoValoresDoApache(preparedStatement, valores, linha)) {
                continue;
            }

            adicionarBatch(preparedStatement, i);
        }
    }

    private boolean extraindoValoresDoApache(PreparedStatement preparedStatement, String[] valores, List<Object> linha) throws SQLException {
        if (valores.length < 29) {
            System.err.println("Linha inválida, ignorando: " + linha);
            return false;
        }

        // Agrupando a extração e validação dos valores
        String nomeDaUF = buscarValorValido(valores, 28);
        String empresaFistel = buscarValorValido(valores, 4);
        String latitude = buscarValorValido(valores, 11);
        String longitude = buscarValorValido(valores, 12);
        String codigoIBGE = buscarValorValido(valores, 25);
        String tecnologia = buscarValorValido(valores, 9);

        // Verifica se algum campo é inválido antes de inserir no banco
        if (algumCampoInvalido(nomeDaUF, empresaFistel, latitude, longitude, codigoIBGE, tecnologia)) {
//            System.err.println("Dados inválidos na linha, ignorando: " + linha);
            return false;
        }
        guardarValorProBanco(preparedStatement, nomeDaUF, empresaFistel, latitude, longitude, codigoIBGE, tecnologia);
        return true;
    }

    private void adicionarBatch(PreparedStatement preparedStatement, int linhaAtual) throws SQLException {
        if (linhaAtual % 5000 == 0) {
            preparedStatement.executeBatch();
            conexao.commit();
        }
        preparedStatement.addBatch();
    }

    private void truncarTabela( ) throws SQLException {
        System.out.println("Truncando a tabela " + "estacoesSMP" + "...");
        try (Statement statement = conexao.createStatement()) {
            String truncateQuery = "TRUNCATE TABLE " + "estacoesSMP";
            statement.executeUpdate(truncateQuery);
            System.out.println("Tabela truncada com sucesso!");
        }
    }

    // Método auxiliar para processar cada linha e retornar os valores como array de String
    private String[] processarLinha(List<Object> linha) {
        String linhaConvertida = buscarValorValido(linha);
        return linhaConvertida.split(";");
    }

    // Verifica se algum dos campos essenciais é nulo ou vazio
    private boolean algumCampoInvalido(String... campos) {
        for (String campo : campos) {
            if (campo == null || campo.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    // Preenche o PreparedStatement com os valores corretos
    private void guardarValorProBanco(PreparedStatement guardarValor, String nomeDaUF, String empresaFistel, String latitude, String longitude, String codigoIBGE, String tecnologia) throws SQLException {
        guardarValor.setString(1, nomeDaUF);
        guardarValor.setString(2, empresaFistel);
        guardarValor.setString(3, latitude);
        guardarValor.setString(4, longitude);
        guardarValor.setString(5, codigoIBGE);
        guardarValor.setString(6, tecnologia);
    }

    // Converte uma lista de objetos para uma string com separador ';'
    private String buscarValorValido(List<Object> row) {
        StringBuilder linha = new StringBuilder();
        for (Object celula : row) {
            if (!linha.isEmpty()) {
                linha.append(";");  // Adicionar separador
            }
            linha.append(celula != null ? celula.toString() : "");
        }
        return linha.toString();
    }

    // Método auxiliar para obter um valor de forma segura de um array
    private String buscarValorValido(String[] valores, int index) {
        // Verifica se o índice está dentro do tamanho do array
        if (index < valores.length) {
            String valor = valores[index];

            if (!valor.isEmpty()) {
                return valor;
            }
        }
        // Retorna null se o índice for inválido ou o valor estiver vazio
        return null;
    }
}