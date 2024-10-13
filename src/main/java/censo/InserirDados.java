package censo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class InserirDados {

    public void inserirDados(List<List<Object>> dadosExcel, Connection conexao) throws SQLException {

        if (conexao == null) {
            throw new SQLException("Conexão com o banco de dados não foi estabelecida.");
        }

        String query = "INSERT INTO censoIBGE (cidade, crescimentoPopulacional, densidadeDemografica) VALUES (?, ?, ?)";

        try (PreparedStatement guardarValor = conexao.prepareStatement(query)) {

            System.out.println("Inserindo dados no banco...");

            // Iterando sobre os dados da planilha, ignorando o cabeçalho (i = 1)
            for (int i = 1; i < dadosExcel.size(); i++) {
                List<Object> linha = dadosExcel.get(i);

                // Certifique-se de que a linha tem ao menos 3 colunas
                if (linha.size() >= 3 && linha.get(0) != null && linha.get(1) != null && linha.get(2) != null) {
                    // Definindo os valores no PreparedStatement
                    guardarValor.setString(1, linha.get(0).toString());  // Cidade
                    guardarValor.setDouble(2, Double.parseDouble(linha.get(1).toString()));  // Crescimento populacional
                    guardarValor.setDouble(3, Double.parseDouble(linha.get(2).toString()));  // Densidade demográfica

                    guardarValor.addBatch();  // Adicionando ao batch

                    // A cada 5000 registros, executa o batch
                    if (i % 5000 == 0) {
                        guardarValor.executeBatch();
                        conexao.commit();  // Commit manual
                        System.out.println("Batch de 5000 registros executado.");
                    }
                }
            }

            // Executa o batch restante
            guardarValor.executeBatch();
            conexao.commit();  // Commit final
            System.out.println("Todos os dados foram inseridos.");

        } catch (SQLException e) {
            conexao.rollback();  // Reverte em caso de erro
            throw e;
        }
    }
}
