package infrastructure.database;

import java.sql.*;

public class BancoOperacoes {

    private Connection conexao;

    public void conectar() throws ClassNotFoundException, SQLException {
        System.out.println("Conectando no banco...");
        Class.forName("com.mysql.cj.jdbc.Driver");
        conexao = DriverManager.getConnection(
                "jdbc:mysql://54.157.243.147:3306/techmentor?rewriteBatchedStatements=true",
                "root",
                "root"
        );
        conexao.setAutoCommit(false);  // Controle de transação manual
        System.out.println("Banco conectado.");
    }

    public void fecharConexao() throws SQLException {
        if (conexao != null && !conexao.isClosed()) {
            conexao.close();
            System.out.println("Conexão fechada.");
        } else {
            System.out.println("A conexão já está fechada ou é nula.");
        }
    }

    public void validarConexao() throws SQLException, ClassNotFoundException {
        if (conexao == null || conexao.isClosed()) {
            conectar(); // Reabre a conexão se estiver fechada ou nula
        }
    }

    public void truncarTabela(String nomeTabela) throws SQLException {
        System.out.println("Truncando a tabela " + nomeTabela + "...");
        try (Statement statement = conexao.createStatement()) {
            String truncateQuery = "TRUNCATE TABLE " + nomeTabela;
            statement.executeUpdate(truncateQuery);
            System.out.println("Tabela truncada com sucesso!");
        }
    }

    public void adicionarBatch(PreparedStatement preparedStatement, int linhaAtual) throws SQLException {
        if (linhaAtual % 5000 == 0) {
            preparedStatement.executeBatch();
            conexao.commit();
            preparedStatement.clearBatch();
        }
        preparedStatement.addBatch();
    }

    public Connection getConexao() {
        return conexao;
    }

}
