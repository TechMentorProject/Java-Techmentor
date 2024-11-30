package infrastructure.database;

import infrastructure.database.BancoOperacoes;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Performance {

    public void calcularQtdAntenasEBuscarMaiorOperadora(BancoOperacoes bancoDeDados) throws SQLException, ClassNotFoundException {
        bancoDeDados.validarConexao();
        Connection conexao = bancoDeDados.getConexao();

        try (PreparedStatement stmtEstados = conexao.prepareStatement("""
                SELECT nomeEstado FROM estado WHERE regiao IN ('Sul', 'Sudeste')
            """)) {

            ResultSet rsEstados = stmtEstados.executeQuery();
            while (rsEstados.next()) {
                String nomeEstado = rsEstados.getString("nomeEstado");

                // Calcular a quantidade de antenas
                int qtdAntenas = calcularQtdAntenas(conexao, nomeEstado);

                // Atualizar quantidade de antenas na tabela estado
                atualizarQtdAntenas(conexao, nomeEstado, qtdAntenas);

                // Buscar a maior operadora
                String maiorOperadora = buscarMaiorOperadora(conexao, nomeEstado);

                // Atualizar a maior operadora na tabela estado
                atualizarMaiorOperadora(conexao, nomeEstado, maiorOperadora);
            }
            conexao.commit();
        }
    }

    private int calcularQtdAntenas(Connection conexao, String nomeEstado) throws SQLException {
        try (PreparedStatement stmtQtdAntenas = conexao.prepareStatement("""
                SELECT COUNT(*) AS qtdAntenasPorEstado FROM baseEstacoesSMP
                JOIN cidade ON cidade.nomeCidade = baseEstacoesSMP.fkCidade
                WHERE cidade.fkEstado = ?
            """)) {
            stmtQtdAntenas.setString(1, nomeEstado);
            ResultSet rsQtdAntenas = stmtQtdAntenas.executeQuery();
            return rsQtdAntenas.next() ? rsQtdAntenas.getInt("qtdAntenasPorEstado") : 0;
        }
    }

    private void atualizarQtdAntenas(Connection conexao, String nomeEstado, int qtdAntenas) throws SQLException {
        try (PreparedStatement stmtUpdateAntenas = conexao.prepareStatement("""
                UPDATE estado SET qtdAntenas = ? WHERE nomeEstado = ?
            """)) {
            stmtUpdateAntenas.setInt(1, qtdAntenas);
            stmtUpdateAntenas.setString(2, nomeEstado);
            stmtUpdateAntenas.executeUpdate();
        }
    }

    private String buscarMaiorOperadora(Connection conexao, String nomeEstado) throws SQLException {
        try (PreparedStatement stmtMaiorOperadora = conexao.prepareStatement("""
                SELECT operadora, COUNT(*) AS aparicoes FROM baseEstacoesSMP
                JOIN cidade ON cidade.nomeCidade = baseEstacoesSMP.fkCidade
                WHERE cidade.fkEstado = ?
                GROUP BY operadora
                ORDER BY aparicoes DESC LIMIT 1
            """)) {
            stmtMaiorOperadora.setString(1, nomeEstado);
            ResultSet rsMaiorOperadora = stmtMaiorOperadora.executeQuery();
            return rsMaiorOperadora.next() ? rsMaiorOperadora.getString("operadora") : "Desconhecida";
        }
    }

    private void atualizarMaiorOperadora(Connection conexao, String nomeEstado, String maiorOperadora) throws SQLException {
        try (PreparedStatement stmtUpdateOperadora = conexao.prepareStatement("""
                UPDATE estado SET maiorOperadora = ? WHERE nomeEstado = ?
            """)) {
            stmtUpdateOperadora.setString(1, maiorOperadora);
            stmtUpdateOperadora.setString(2, nomeEstado);
            stmtUpdateOperadora.executeUpdate();
        }
    }
}
