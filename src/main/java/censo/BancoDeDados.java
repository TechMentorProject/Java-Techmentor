package censo;

import java.sql.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BancoDeDados {

    private Connection conexao;

    public void conectar() throws ClassNotFoundException, SQLException {
        System.out.println("Conectando no banco");
        Class.forName("com.mysql.cj.jdbc.Driver");
        conexao = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/techmentor",
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

    public void truncarTabela( ) throws SQLException {
        System.out.println("Truncando a tabela " + "censoIBGE" + "...");
        try (Statement statement = conexao.createStatement()) {
            String truncateQuery = "TRUNCATE TABLE " + "censoIBGE";
            statement.executeUpdate(truncateQuery);
            System.out.println("Tabela truncada com sucesso!");
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
                if(dadosSeparados[1] == null || dadosSeparados[2] == null || dadosSeparados[3] == null) {
                continue;
                }
                Double crescimentoPopulacional = Double.parseDouble(dadosSeparados[1]);
                Double densidadeDemografica = Double.parseDouble(dadosSeparados[2]);
                System.out.println(crescimentoPopulacional);
                System.out.println(densidadeDemografica);
                if(dadosSeparados[i] != null || dadosSeparados[i].isEmpty()) {
                    guardarValor.setString(1, dadosSeparados[3]); // Cidade
                    guardarValor.setDouble(2, crescimentoPopulacional); // crescimentoPopulacional
                    guardarValor.setDouble(3, densidadeDemografica); // densidadeDemografica
                }
                guardarValor.executeUpdate();
            }
        }
        System.out.println("Dados Inseridos");
    }
}
