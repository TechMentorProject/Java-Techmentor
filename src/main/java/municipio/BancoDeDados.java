package municipio;

import java.sql.*;
import java.util.List;

public class BancoDeDados {

    private Connection conexao;

    public void conectar() throws ClassNotFoundException, SQLException {
        System.out.println("Conectando no banco");
        Class.forName("com.mysql.cj.jdbc.Driver");
        conexao = DriverManager.getConnection("jdbc:mysql://localhost:3306/techmentor", "root", "root");
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

        String query = "INSERT INTO municipio (ano, cidade, operadora, domiciliosCobertosPercent, areaCobertaPercent, tecnologia) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        PreparedStatement preparedStatement = conexao.prepareStatement(query);

        // Pular a primeira linha (cabeçalho)
        for (int i = 1; i < dadosExcel.size(); i++) {  // Começar em 1 para pular o cabeçalho
            List<Object> row = dadosExcel.get(i);

            // Converter linha para String e aplicar split
            String linha = convertRowToString(row);

            // Fazer o split da linha para dividir os campos
            String[] valores = linha.split(";");

            // Verifica se o número de campos corresponde ao esperado
            if (valores.length < 13) {
                System.err.println("Linha com menos colunas do que o esperado. Ignorando: " + linha);
                continue;  // Pular se a linha não tiver a quantidade esperada de colunas
            }

            // Inserindo os dados splitados no banco
            preparedStatement.setString(1, getSafeValue(valores, 0)); // ano
            preparedStatement.setString(2, getSafeValue(valores, 5));  // cidade
            preparedStatement.setString(3, getSafeValue(valores, 2)); // operadora
            preparedStatement.setString(4, getSafeValue(valores, 11)); // domiciliosCobertosPercent
            preparedStatement.setString(5, getSafeValue(valores, 12)); // areaCobertaPercent
            preparedStatement.setString(6, getSafeValue(valores, 3));  // tecnologia




            // Executar a query para a linha atual
            preparedStatement.executeUpdate();
        }

        System.out.println("Dados Inseridos");
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
