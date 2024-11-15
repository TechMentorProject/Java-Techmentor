package infrastructure.database;

import config.Configuracoes;

import java.sql.*;

public class BancoOperacoes {

    private Connection conexao;

    public void conectar() throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");

        conexao = DriverManager.getConnection(
                "jdbc:mysql://" + Configuracoes.IP_BANCO.getValor() + ":" + Configuracoes.PORTA_BANCO.getValor() + "?rewriteBatchedStatements=true",
                Configuracoes.USUARIO.getValor(),
                Configuracoes.SENHA.getValor()
        );
        conexao.setAutoCommit(false);
        System.out.println("Conexão ao servidor MySQL estabelecida.");

        criarBancoSeNaoExistir(Configuracoes.DATABASE.getValor());

        conexao.close();
        conexao = DriverManager.getConnection(
                "jdbc:mysql://" + Configuracoes.IP_BANCO.getValor() + ":" + Configuracoes.PORTA_BANCO.getValor() + "/" + Configuracoes.DATABASE.getValor() + "?rewriteBatchedStatements=true",
                Configuracoes.USUARIO.getValor(),
                Configuracoes.SENHA.getValor()
        );
        conexao.setAutoCommit(false);
    }

    private void criarBancoSeNaoExistir(String nomeBanco) throws SQLException {
        try (Statement stmt = conexao.createStatement()) {
            String sql = "CREATE DATABASE IF NOT EXISTS " + nomeBanco;
            stmt.executeUpdate(sql);
            System.out.println("Banco de dados '" + nomeBanco + "' criado / verificado com sucesso.");
        }
    }

    public void fecharConexao() throws SQLException {
        if (conexao != null && !conexao.isClosed()) {
            conexao.close();
            System.out.println("Conexão fechada.");
            System.out.println("-----------------------------------------");
        } else {
            System.out.println("A conexão já está fechada ou é nula.");
            System.out.println("-----------------------------------------");
        }
    }

    public void validarConexao() throws SQLException, ClassNotFoundException {
        if (conexao == null || conexao.isClosed()) {
            conectar();
        }
    }

    public void truncarTabela(String nomeTabela) throws SQLException {
        try (Statement statement = conexao.createStatement()) {
            String truncateQuery = "TRUNCATE TABLE " + nomeTabela;
            statement.executeUpdate(truncateQuery);
            System.out.println("-----------------------------------------");
            System.out.println("Tabela " + nomeTabela + " truncada com sucesso!");
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
