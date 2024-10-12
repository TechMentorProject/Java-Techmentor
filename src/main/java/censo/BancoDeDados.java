package censo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BancoDeDados {

    private Connection conexao;

    public void conectar() throws ClassNotFoundException, SQLException {
        System.out.println("Conectando no banco");
        Class.forName("com.mysql.cj.jdbc.Driver");
        conexao = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/techmentor?useUnicode=true&characterEncoding=utf8&characterSetResults=utf8",
                "root",
                "root"
        );
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

        String query = "INSERT INTO censoIBGE (cidade, crescimentoPopulacional, densidadeDemografica) VALUES (?, ?, ?)";

        try (PreparedStatement guardarValor = conexao.prepareStatement(query)) {

            System.out.println("Inserindo dados");

            for (int i = 1; i < dadosExcel.size(); i++) {
                String dados = dadosExcel.get(i).toString();
                String [] dadosSeparados = dados.split(",");
                if(dadosSeparados[i] != null || dadosSeparados[i].isEmpty()) {
                    guardarValor.setString(1, dadosSeparados[3]); // Verifica se a área é null
                    guardarValor.setString(2, dadosSeparados[1]); // Verifica se a densidade é null
                    guardarValor.setString(3, dadosSeparados[2]); // Verifica se a UF é null
                }
                guardarValor.executeUpdate();
            }
        }
        System.out.println("Dados Inseridos");
    }
}
