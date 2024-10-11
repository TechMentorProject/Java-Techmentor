package municipio;

import java.sql.*;
import java.util.List;
import java.util.Arrays;

public class BancoDeDados {

    private Connection conexao;

    // Método para conectar ao banco
    public void conectar() throws ClassNotFoundException, SQLException {
        System.out.println("Conectando no banco...");
        Class.forName("com.mysql.cj.jdbc.Driver");
        conexao = DriverManager.getConnection("jdbc:mysql://localhost:3306/techmentor?useUnicode=true&characterEncoding=UTF-8", "root", "root");
        conexao.setAutoCommit(false);  // Controle de transação manual
        System.out.println("Banco conectado.");
    }

    // Método para fechar a conexão
    public void fecharConexao() throws SQLException {
        if (conexao != null && !conexao.isClosed()) {
            conexao.close();
            System.out.println("Conexão fechada.");
        } else {
            System.out.println("A conexão já está fechada ou é nula.");
        }
    }

    // Método para validar a conexão
    private void validarConexao() throws SQLException {
        if (conexao == null) {
            throw new SQLException("Conexão com o banco de dados não foi estabelecida.");
        }
    }

    // Método para inserir dados com tratamento (semelhante ao `inserirDadosComTratamento`)
    public void inserirDadosComTratamento(List<List<Object>> dadosExcel) throws SQLException {
        validarConexao();

        truncarTabela();

        System.out.println("Inserindo dados...");

        String query = "INSERT INTO municipio (ano, cidade, operadora, domiciliosCobertosPercent, areaCobertaPercent, tecnologia) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = conexao.prepareStatement(query)) {
            processarEInserirDados(dadosExcel, preparedStatement);
            preparedStatement.executeBatch();
            conexao.commit();
            System.out.println("Dados inseridos com sucesso!");
        }
    }

    // Método para processar e inserir dados (semelhante ao `processarEInserirDados`)
    private void processarEInserirDados(List<List<Object>> dadosExcel, PreparedStatement preparedStatement) throws SQLException {
        for (int i = 1; i < dadosExcel.size(); i++) {  // Ignora a primeira linha (cabeçalho)
            List<Object> linha = dadosExcel.get(i);
            String[] valores = processarLinha(linha);

            // Se a linha não for válida, pula para a próxima
            if (!extraindoValoresDoMunicipio(preparedStatement, valores, linha)) {
                continue;
            }

            adicionarBatch(preparedStatement, i);
        }
    }

    // Método para extrair e validar os valores de cada linha (similar a `extraindoValoresDoApache`)
    private boolean extraindoValoresDoMunicipio(PreparedStatement preparedStatement, String[] valores, List<Object> linha) throws SQLException {
        if (valores.length < 13) {
            System.err.println("Linha inválida, ignorando: " + linha);
            return false;
        }

        // Extração e validação de valores
        String ano = buscarValorValido(valores, 0);
        String cidade = buscarValorValido(valores, 5);
        String operadora = buscarValorValido(valores, 2);
        String domiciliosCobertosPercent = buscarValorValido(valores, 10);
        String areaCobertaPercent = buscarValorValido(valores, 11);

        // Formatações específicas
        String areaCobertaFormatada = formatarAreaCoberta(areaCobertaPercent);
        String tecnologiaFormatada = formatarTecnologia(buscarValorValido(valores, 3));

        // Verifica se algum campo essencial é inválido
        if (algumCampoInvalido(ano, cidade, operadora, domiciliosCobertosPercent, areaCobertaPercent, tecnologiaFormatada)) {
            return false;
        }

        // Ignora se % coberto é zero
        if ("0".equals(domiciliosCobertosPercent) || "0".equals(areaCobertaPercent)) {
            return false;
        }

        // Preencher o `PreparedStatement`
        guardarValorProBanco(preparedStatement, ano, cidade, operadora, domiciliosCobertosPercent, areaCobertaFormatada, tecnologiaFormatada);
        return true;
    }

    // Método auxiliar para formatar a área coberta
    private String formatarAreaCoberta(String areaCobertaPercent) {
        if (areaCobertaPercent != null && areaCobertaPercent.length() >= 2) {
            return areaCobertaPercent.substring(0, 2);
        }
        return areaCobertaPercent;
    }

    // Método auxiliar para truncar a tabela (igual ao `truncarTabela`)
    private void truncarTabela() throws SQLException {
        System.out.println("Truncando a tabela municipio...");
        try (Statement statement = conexao.createStatement()) {
            String truncateQuery = "TRUNCATE TABLE municipio";
            statement.executeUpdate(truncateQuery);
            System.out.println("Tabela truncada com sucesso!");
        }
    }

    // Método auxiliar para adicionar o batch
    private void adicionarBatch(PreparedStatement preparedStatement, int linhaAtual) throws SQLException {
        if (linhaAtual % 5000 == 0) {
            preparedStatement.executeBatch();
            conexao.commit();
        }
        preparedStatement.addBatch();
    }

    // Método auxiliar para verificar campos inválidos (igual ao `algumCampoInvalido`)
    private boolean algumCampoInvalido(String... campos) {
        for (String campo : campos) {
            if (campo == null || campo.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    // Método auxiliar para preencher o `PreparedStatement` (igual ao `guardarValorProBanco`)
    private void guardarValorProBanco(PreparedStatement guardarValor, String ano, String cidade, String operadora, String domiciliosCobertosPercent, String areaCobertaFormatada, String tecnologiaFormatada) throws SQLException {
        guardarValor.setString(1, ano);
        guardarValor.setString(2, cidade);
        guardarValor.setString(3, operadora);
        guardarValor.setString(4, domiciliosCobertosPercent);
        guardarValor.setString(5, areaCobertaFormatada);
        guardarValor.setString(6, tecnologiaFormatada);
    }

    // Método auxiliar para processar a linha (igual ao `processarLinha`)
    private String[] processarLinha(List<Object> linha) {
        String linhaConvertida = buscarValorValido(linha);
        return linhaConvertida.split(";");
    }

    // Converte uma lista de objetos para uma string com separador ";"
    private String buscarValorValido(List<Object> row) {
        StringBuilder linha = new StringBuilder();
        for (Object celula : row) {
            if (!linha.isEmpty()) {
                linha.append(";");  // Adicionar separador
            }
            linha.append(celula != null ? celula.toString() : "");
        }
        return linha.toString();
    }

    // Método auxiliar para obter um valor seguro de um array (igual ao `buscarValorValido`)
    private String buscarValorValido(String[] valores, int index) {
        if (index < valores.length) {
            String valor = valores[index];
            if (!valor.isEmpty()) {
                return valor;
            }
        }
        return null;
    }

    // Método auxiliar para formatar a tecnologia (igual ao `formatarTecnologia`)
    private String formatarTecnologia(String tecnologia) {
        if (tecnologia == null || tecnologia.isEmpty() || tecnologia.equalsIgnoreCase("Todas")) {
            return "2G, 3G, 4G, 5G";  // Se for "Todas", retorna todas as tecnologias
        }

        String[] possiveisTecnologias = {"2G", "3G", "4G", "5G"};
        StringBuilder tecnologiasFormatadas = new StringBuilder();

        for (String tech : possiveisTecnologias) {
            if (tecnologia.toUpperCase().contains(tech.toUpperCase())) {
                if (!tecnologiasFormatadas.isEmpty()) {
                    tecnologiasFormatadas.append(", ");
                }
                tecnologiasFormatadas.append(tech);
            }
        }

        return tecnologiasFormatadas.toString().isEmpty() ? null : tecnologiasFormatadas.toString();
    }
}
