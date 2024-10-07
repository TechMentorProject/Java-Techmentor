package projecao_populacional;

import java.sql.*;
import java.util.List;

public class BancoDeDados {

    private Connection conexao;

    // Conectar ao banco
    public void conectar() throws ClassNotFoundException, SQLException {
        System.out.println("Conectando no banco");
        Class.forName("com.mysql.cj.jdbc.Driver");
        conexao = DriverManager.getConnection("jdbc:mysql://localhost:3306/techmentor?useUnicode=true&characterEncoding=UTF-8", "root", "root");
        conexao.setAutoCommit(false); // Para controle de transação
        System.out.println("Banco conectado");
    }

    // Fechar conexão
    public void fecharConexao() throws SQLException {
        if (conexao != null && !conexao.isClosed()) {
            conexao.close();
            System.out.println("Conexão fechada.");
        } else {
            System.out.println("A conexão já está fechada ou é nula.");
        }
    }

    // Inserir dados
    public void inserirDados(List<List<Object>> dadosExcel) throws SQLException {
        if (conexao == null) {
            throw new SQLException("Conexão com o banco de dados não foi estabelecida.");
        }

        String query = "INSERT INTO projecaoPopulacional (estado, idade, projecao_2024, projecao_2025, projecao_2026, projecao_2027, projecao_2028)" +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        System.out.println("Truncando a tabela projecaoPopulacional...");
        String truncateQuery = "TRUNCATE TABLE projecaoPopulacional";
        Statement statement = conexao.createStatement();
        statement.executeUpdate(truncateQuery);
        System.out.println("Tabela Truncada com sucesso!");

        PreparedStatement preparedStatement = conexao.prepareStatement(query);

        // Contadores para debug
        int linhasIgnoradas = 0;
        int linhasInseridas = 0;

        // Processar linhas do Excel
        System.out.println("Inserindo dados");
        for (int i = 0; i < dadosExcel.size(); i++) {  // Ignora o cabeçalho (linha 1)
            List<Object> row = dadosExcel.get(i);
            String linha = convertRowToString(row);
            String[] valores = linha.split(";");

            // Verificar e imprimir linhas com problemas
            if (valores.length < 13) {
                System.out.println("Linha ignorada por ter menos de 13 colunas: " + linha);
                linhasIgnoradas++;
                continue;  // Pula a linha se faltar valores
            }

            String estado = getSafeValue(valores, 4);  // Estado na coluna 4
            String idade = getSafeValue(valores, 0);   // Idade na coluna 0
            String ano_2024 = getSafeValue(valores, 29);  // Projeções nas colunas 29-33
            String ano_2025 = getSafeValue(valores, 30);
            String ano_2026 = getSafeValue(valores, 31);
            String ano_2027 = getSafeValue(valores, 32);
            String ano_2028 = getSafeValue(valores, 33);

            if (estado.toLowerCase().equals("sul") || estado.toLowerCase().equals("norte") || estado.toLowerCase().equals("local")) {
                System.out.println("Linha com os 3 valores mano, removendo");
                continue;
            }

            // ** Depurar valores **
            if (estado == null || idade == null || ano_2024 == null || ano_2025 == null || ano_2026 == null || ano_2027 == null || ano_2028 == null) {
                System.out.println("Linha ignorada por ter valores nulos: " + linha);
                linhasIgnoradas++;
                continue; // Pula a linha se qualquer valor necessário for nulo
            }

            // Verifica se contém "Brasil", "homem", "mulher" ou regiões do Brasil (case insensitive)
            String linhaLowerCase = linha.toLowerCase();
            if (linhaLowerCase.contains("brasil") ||
                    linhaLowerCase.contains("homens") ||
                    linhaLowerCase.contains("mulheres") ||
                    linhaLowerCase.contains("centro-oeste") ||
                    linhaLowerCase.contains("sudeste") ||
                    linhaLowerCase.contains("nordeste")) {
                System.out.println("Linha ignorada devido ao filtro de palavras: " + linha);
                linhasIgnoradas++;
                continue; // Pula a linha
            }

            // Adicionar ao batch
            preparedStatement.setString(1, estado);
            preparedStatement.setString(2, idade);
            preparedStatement.setString(3, ano_2024);
            preparedStatement.setString(4, ano_2025);
            preparedStatement.setString(5, ano_2026);
            preparedStatement.setString(6, ano_2027);
            preparedStatement.setString(7, ano_2028);

            preparedStatement.addBatch();
            linhasInseridas++;

            // Executar o batch a cada 5000 linhas
            if (i % 5000 == 0) {
                preparedStatement.executeBatch();
                conexao.commit();
            }
        }

        // Executar o batch final e commit
        preparedStatement.executeBatch();
        conexao.commit();  // Adicionado commit final

        System.out.println("Dados inseridos com sucesso.");
        System.out.println("Linhas inseridas: " + linhasInseridas);
        System.out.println("Linhas ignoradas: " + linhasIgnoradas);

        preparedStatement.close();
    }

    // Converte uma linha do Excel para String com separadores ";"
    private String convertRowToString(List<Object> row) {
        StringBuilder linha = new StringBuilder();
        for (Object celula : row) {
            if (linha.length() > 0) {
                linha.append(";");
            }
            linha.append(celula != null ? celula.toString() : "");
        }
        return linha.toString();
    }

    // Retorna um valor seguro de um array
    private String getSafeValue(String[] valores, int index) {
        return (index < valores.length && !valores[index].isEmpty()) ? valores[index] : null;
    }

}
