package censo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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
        }
    }

    public void inserirDados(List<List<Object>> dadosExcel) throws SQLException {
        if (conexao == null) {
            throw new SQLException("Conexão com o banco de dados não foi estabelecida.");
        }

        System.out.println("Preparando para inserir dados");

        // Query atualizada, mantendo todos os campos como strings
        String query = "INSERT INTO censoIBGE (cidade, crescimentoPopulacional, densidadeDemografica) VALUES (?, ?, ?)";

        try (PreparedStatement preparedStatement = conexao.prepareStatement(query)) {
            // Pular a primeira linha (cabeçalho)
            for (int i = 1; i < dadosExcel.size(); i++) {
                List<Object> row = dadosExcel.get(i);

                // Insere os valores como string
                preparedStatement.setString(1, row.get(1) != null ? row.get(1).toString() : ""); // Verifica se a área é null
                preparedStatement.setString(2, row.get(2) != null ? row.get(2).toString() : ""); // Verifica se a densidade é null
                preparedStatement.setString(3, row.get(3) != null ? row.get(3).toString() : ""); // Verifica se a UF é null


                // Executar a query
                preparedStatement.executeUpdate();
            }
        }
        System.out.println("Dados Inseridos");
    }
}
