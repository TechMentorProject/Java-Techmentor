package infrastructure.database;

import infrastructure.config.Configuracoes;

import java.sql.*;

public class BancoOperacoes {

    private Connection conexao;

    public void conectar() throws ClassNotFoundException, SQLException {
        System.out.println("Conectando ao servidor MySQL...");
        Class.forName("com.mysql.cj.jdbc.Driver");

        // Conexão inicial sem especificar o banco de dados
        conexao = DriverManager.getConnection(
                "jdbc:mysql://" + Configuracoes.IP_BANCO.getValor() + ":" + Configuracoes.PORTA_BANCO.getValor() + "?rewriteBatchedStatements=true",
                Configuracoes.USUARIO.getValor(),
                Configuracoes.SENHA.getValor()
        );
        conexao.setAutoCommit(false);
        System.out.println("Conexão ao servidor MySQL estabelecida.");

        // Criação do banco de dados, se necessário
        criarBancoSeNaoExistir("techmentor");

        // Conectar novamente, agora especificando o banco de dados
        conexao.close();
        conexao = DriverManager.getConnection(
                "jdbc:mysql://" + Configuracoes.IP_BANCO.getValor() + ":" + Configuracoes.PORTA_BANCO.getValor() + "/techmentor?rewriteBatchedStatements=true",
                Configuracoes.USUARIO.getValor(),
                Configuracoes.SENHA.getValor()
        );
        conexao.setAutoCommit(false);
        System.out.println("Conectado ao banco de dados 'techmentor'.");
    }

    private void criarBancoSeNaoExistir(String nomeBanco) throws SQLException {
        try (Statement stmt = conexao.createStatement()) {
            String sql = "CREATE DATABASE IF NOT EXISTS " + nomeBanco;
            stmt.executeUpdate(sql);
            System.out.println("Banco de dados '" + nomeBanco + "' criado/verificado com sucesso.");
        }
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
            conectar();
        }
    }

    // Método auxiliar para truncar tabelas
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
