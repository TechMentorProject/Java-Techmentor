package estacoes_smp;

import java.sql.*;
import java.util.List;

public class BancoDeDados {

    private Connection conexao;

    public void conectar() throws ClassNotFoundException, SQLException {
        System.out.println("Conectando no banco");
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

    public void inserirDados(List<List<Object>> dadosExcel) throws SQLException {
        if (conexao == null) {
            throw new SQLException("Conexão com o banco de dados não foi estabelecida.");
        }
        System.out.println("Preparando para inserir dados");

        String query = "INSERT INTO estacoesSMP (cidade, operadora, latitude, longitude, " +
                "codigoIBGE, tecnologia) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        System.out.println("Truncando a tabela estacoesSMP...");
        String truncateQuery = "TRUNCATE TABLE estacoesSMP";
        Statement statement = conexao.createStatement();
        statement.executeUpdate(truncateQuery);
        System.out.println("Tabela Truncada com sucesso!");

        PreparedStatement preparedStatement = conexao.prepareStatement(query);

        // Pular a primeira linha (cabeçalho)
        for (int i = 1; i < dadosExcel.size(); i++) {  // Começar em 1 para pular o cabeçalho
            List<Object> row = dadosExcel.get(i);

            // Converter linha para String e aplicar split
            String linha = convertRowToString(row);

            // Fazer o split da linha para dividir os campos
            String[] valores = linha.split(";");

            // Verifica se o número de campos corresponde ao esperado
            if (valores.length < 29) {
                System.err.println("Linha com menos colunas do que o esperado. Ignorando: " + linha);
                continue;  // Pular se a linha não tiver a quantidade esperada de colunas
            }


            String nomeDaUF = getSafeValue(valores, 28);  // Nome_da_UF
            String empresaFistel = getSafeValue(valores, 4);  // Empresa_Fistel
            String latitude = getSafeValue(valores, 11);  // Latitude
            String longitude = getSafeValue(valores, 12);  // Longitude
            String codigoIBGE = getSafeValue(valores, 25);  // Codigo_IBGE
            String tecnologia = getSafeValue(valores, 9);  // Tecnologia

            // Verificar se algum dos campos é null
            if (nomeDaUF == null || empresaFistel == null || latitude == null || longitude == null || codigoIBGE == null || tecnologia == null) {
                continue;  // Pular a linha se algum campo essencial for null
            }

            // Inserir os dados no PreparedStatement se todos os campos estiverem válidos
            preparedStatement.setString(1, nomeDaUF);
            preparedStatement.setString(2, empresaFistel);
            preparedStatement.setString(3, latitude);
            preparedStatement.setString(4, longitude);
            preparedStatement.setString(5, codigoIBGE);
            preparedStatement.setString(6, tecnologia);

            // Executar o batch a cada 5000 linhas
            if (i % 5000 == 0) {
                preparedStatement.executeBatch();
                conexao.commit();
            }


            // Executar a query para a linha atual
            preparedStatement.addBatch();
        }

        System.out.println("Dados Inseridos");
        preparedStatement.executeBatch();
        preparedStatement.close();
    }

    /**
     * Converte uma lista de objetos para uma string, unindo com separador ';'.
     */
    private String convertRowToString(List<Object> row) {
        StringBuilder linha = new StringBuilder();

        for (Object celula : row) {
            if (linha.length() > 0) {
                linha.append(";");  // Adicionar separador entre os valores
            }
            linha.append(celula != null ? celula.toString() : "");  // Converte o Object para String
        }

        return linha.toString();
    }

    /**
     * Método auxiliar para obter um valor de forma segura a partir de um array.
     * Retorna null se o índice estiver fora dos limites ou o valor for vazio.
     */
    private String getSafeValue(String[] valores, int index) {
        return (index < valores.length && !valores[index].isEmpty()) ? valores[index] : null;
    }
}
