package municipio;

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

        String query = "INSERT INTO municipio (ano, cidade, operadora, domiciliosCobertosPercent, areaCobertaPercent, tecnologia) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        System.out.println("Truncando a tabela municipio...");
        String truncateQuery = "TRUNCATE TABLE municipio";
        Statement statement = conexao.createStatement();
        statement.executeUpdate(truncateQuery);
        System.out.println("Tabela Truncada com sucesso!");

        PreparedStatement preparedStatement = conexao.prepareStatement(query);

        // Processar linhas do Excel
        System.out.println("Inserindo dados");
        for (int i = 1; i < dadosExcel.size(); i++) {  // Ignora o cabeçalho (linha 1)
            List<Object> row = dadosExcel.get(i);
            String linha = convertRowToString(row);
            String[] valores = linha.split(";");

            if (valores.length < 13) {
                continue;
            }

            String ano = getSafeValue(valores, 0);
            String cidade = getSafeValue(valores, 5);
            String operadora = getSafeValue(valores, 2);
            String domiciliosCobertosPercent = getSafeValue(valores, 10);
            String areaCobertaPercent = getSafeValue(valores, 11);
            String areaCobertaFormatada = areaCobertaPercent;
            if (areaCobertaPercent != null && areaCobertaPercent.length() >= 2) {
                areaCobertaFormatada = areaCobertaPercent.substring(0, 2);
            }

            // Ignorar se % cobertos é zero
            if ("0".equals(domiciliosCobertosPercent) || "0".equals(areaCobertaPercent)) {
                continue;
            }

            // Formatar tecnologia, incluindo tratamento de "Todas"
            String tecnologiaFormatada = formatarTecnologia(getSafeValue(valores, 3));
            if (tecnologiaFormatada == null || tecnologiaFormatada.isEmpty()) {
                continue;
            }

            // Adicionar ao batch
            preparedStatement.setString(1, ano);  // ano
            preparedStatement.setString(2, cidade);  // cidade
            preparedStatement.setString(3, operadora);  // operadora
            preparedStatement.setString(4, domiciliosCobertosPercent);  // % domicilios cobertos
            preparedStatement.setString(5, areaCobertaFormatada);  // % area coberta
            preparedStatement.setString(6, tecnologiaFormatada);  // tecnologia

            preparedStatement.addBatch();

            // Executar o batch a cada 5000 linhas
            if (i % 5000 == 0) {
                preparedStatement.executeBatch();
                conexao.commit();
            }
        }

        // Executar o batch final e commit
        preparedStatement.executeBatch();

        System.out.println("Dados inseridos com sucesso.");
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

    // Formata tecnologia e lida com o valor "Todas"
    private String formatarTecnologia(String tecnologia) {
        if (tecnologia == null || tecnologia.isEmpty() || tecnologia.equalsIgnoreCase("Todas")) {
            return "2G, 3G, 4G, 5G";  // Se for "Todas", retorna todas as tecnologias
        }

        String[] possiveisTecnologias = {"2G", "3G", "4G", "5G"};
        StringBuilder tecnologiasFormatadas = new StringBuilder();

        for (String tech : possiveisTecnologias) {
            if (tecnologia.toUpperCase().contains(tech.toUpperCase())) {
                if (tecnologiasFormatadas.length() > 0) {
                    tecnologiasFormatadas.append(", ");
                }
                tecnologiasFormatadas.append(tech);
            }
        }

        return tecnologiasFormatadas.toString().isEmpty() ? null : tecnologiasFormatadas.toString();
    }

}
