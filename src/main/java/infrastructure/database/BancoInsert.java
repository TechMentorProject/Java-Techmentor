package infrastructure.database;

import infrastructure.utils.ValidacoesLinha;
import java.sql.*;
import java.util.*;

public class BancoInsert {

    private final BancoOperacoes bancoOperacoes;
    private final ValidacoesLinha validacoesLinha;

    public BancoInsert(BancoOperacoes bancoOperacoes, ValidacoesLinha validacoesLinha) {
        this.bancoOperacoes = bancoOperacoes;
        this.validacoesLinha = validacoesLinha;
    }

    public void inserirDadosIniciais() throws SQLException {
        try {
            bancoOperacoes.validarConexao();
            String verificaTabelaVazia = "SELECT COUNT(*) AS count FROM estado";
            bancoOperacoes.getConexao().setAutoCommit(false);
            String insercaoEstados = "INSERT IGNORE INTO estado (regiao, sigla, nomeEstado) VALUES " +
                    "('Norte', 'AC', 'Acre')," +
                    "('Norte', 'AP', 'Amapá')," +
                    "('Norte', 'AM', 'Amazonas')," +
                    "('Norte', 'PA', 'Pará')," +
                    "('Norte', 'RO', 'Rondônia')," +
                    "('Norte', 'RR', 'Roraima')," +
                    "('Norte', 'TO', 'Tocantins')," +
                    "('Nordeste', 'AL', 'Alagoas')," +
                    "('Nordeste', 'BA', 'Bahia')," +
                    "('Nordeste', 'CE', 'Ceará')," +
                    "('Nordeste', 'MA', 'Maranhão')," +
                    "('Nordeste', 'PB', 'Paraíba')," +
                    "('Nordeste', 'PE', 'Pernambuco')," +
                    "('Nordeste', 'PI', 'Piauí')," +
                    "('Nordeste', 'RN', 'Rio Grande do Norte')," +
                    "('Nordeste', 'SE', 'Sergipe')," +
                    "('Centro-Oeste', 'DF', 'Distrito Federal')," +
                    "('Centro-Oeste', 'GO', 'Goiás')," +
                    "('Centro-Oeste', 'MT', 'Mato Grosso')," +
                    "('Centro-Oeste', 'MS', 'Mato Grosso do Sul')," +
                    "('Sudeste', 'ES', 'Espírito Santo')," +
                    "('Sudeste', 'MG', 'Minas Gerais')," +
                    "('Sudeste', 'RJ', 'Rio de Janeiro')," +
                    "('Sudeste', 'SP', 'São Paulo')," +
                    "('Sul', 'PR', 'Paraná')," +
                    "('Sul', 'RS', 'Rio Grande do Sul')," +
                    "('Sul', 'SC', 'Santa Catarina')";

            try (Connection conexao = bancoOperacoes.getConexao();
                 PreparedStatement verificaStmt = conexao.prepareStatement(verificaTabelaVazia);
                 PreparedStatement insercaoStmt = conexao.prepareStatement(insercaoEstados)) {

                conexao.setAutoCommit(false);

                try (ResultSet resultSet = verificaStmt.executeQuery()) {
                    resultSet.next();
                    int count = resultSet.getInt("count");

                    if (count == 0) {
                        insercaoStmt.executeUpdate();
                        conexao.commit();
                        System.out.println("Estados inseridos com sucesso.");
                    } else {
                        System.out.println("Tabela 'estado' já contém registros.");
                    }
                }
            } catch (SQLException e) {
                System.err.println("Erro ao inserir dados iniciais: " + e.getMessage());
                throw e;
            }
        } catch (Exception e) {
            System.err.println("Erro inesperado: " + e.getMessage());
        } finally {
            bancoOperacoes.fecharConexao();
        }
    }

    public List<String> extrairCidades(List<List<Object>> dadosExcel) {
        Set<String> cidadesSet = new HashSet<>();
        Integer indiceColuna = obterIndiceColuna(dadosExcel, "Município");

        for (int i = 1; i < dadosExcel.size(); i++) {
            List<Object> linha = dadosExcel.get(i);
            List<String> valores = Arrays.stream(validacoesLinha.processarLinha(linha)).toList();
            String cidadeOriginal = valores.get(indiceColuna).trim(); // Manter formato original

            // Verifica se o nome da cidade contém o caractere '?'
            if (cidadeOriginal.contains("�?")) {
                continue; // Pula essa linha
            }

            cidadesSet.add(cidadeOriginal);
        }
        return new ArrayList<>(cidadesSet);
    }


    public void inserirCidades(List<String> cidades) {
        try {
            bancoOperacoes.validarConexao();
            Connection conexao = bancoOperacoes.getConexao();
            conexao.setAutoCommit(false);

            for (String cidadeComSigla : cidades) {
                inserirCidadeComEstado(cidadeComSigla);
            }
            conexao.commit();
            System.out.println("Todas as cidades foram inseridas com sucesso!");
        } catch (Exception e) {
            try {
                bancoOperacoes.getConexao().rollback();
            } catch (SQLException ex) {
                System.err.println("Erro ao reverter transação: " + ex.getMessage());
            }
            System.err.println("Erro ao inserir cidades: " + e.getMessage());
        }
    }

    public void inserirCidadeComEstado(String cidadeComSigla) {
        try {
            // Verifica se o nome da cidade contém o caractere '?'
            if (cidadeComSigla.contains("�?")) {
                System.out.println("Cidade contém o caractere '?' e será ignorada: " + cidadeComSigla);
                return; // Pula a inserção dessa cidade
            }

            String[] partes = cidadeComSigla.split(" - ");
            if (partes.length != 2) {
                throw new IllegalArgumentException("Formato inválido. Esperado: 'Cidade - Sigla'");
            }

            String cidadeParaInsercao = partes[0].trim();
            if (cidadeParaInsercao.charAt(0) == '"') {
                cidadeParaInsercao = cidadeParaInsercao.substring(1);
            }
            String cidadeParaComparacao = cidadeParaInsercao.toLowerCase();
            String sigla = limparSigla(partes[1].trim());

            if (!cidadeJaExiste(cidadeParaComparacao, sigla)) {
                String nomeEstado = buscarFkEstado(sigla);
                if (nomeEstado == null) {
                    throw new SQLException("Estado com sigla '" + sigla + "' não encontrado.");
                }

                String sqlInsert = "INSERT IGNORE INTO cidade (nomeCidade, fkEstado) VALUES (?, ?)";
                try (PreparedStatement stmt = bancoOperacoes.getConexao().prepareStatement(sqlInsert)) {
                    stmt.setString(1, cidadeParaInsercao);  // Insere apenas o nome da cidade
                    stmt.setString(2, nomeEstado);          // Insere a chave estrangeira do estado
                    stmt.executeUpdate();
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao inserir cidade com estado: " + e.getMessage());
        }
    }


    private String limparSigla(String sigla) {
        return sigla.replaceAll("[^A-Za-z]", "").toUpperCase();
    }

    private boolean cidadeJaExiste(String nomeCidade, String sigla) throws SQLException {
        String sqlVerifica = "SELECT COUNT(*) FROM cidade WHERE LOWER(nomeCidade) = LOWER(?) AND fkEstado = ?";
        try (PreparedStatement stmt = bancoOperacoes.getConexao().prepareStatement(sqlVerifica)) {
            stmt.setString(1, nomeCidade);
            stmt.setString(2, sigla);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    private int obterIndiceColuna(List<List<Object>> dadosExcel, String nomeColuna) {
        String cabecalho = dadosExcel.get(0).get(0).toString();
        if (cabecalho.length() > 0 && cabecalho.charAt(0) == '\uFEFF') {
            cabecalho = cabecalho.substring(1);
        }
        String[] colunas = cabecalho.split(";");
        for (int i = 0; i < colunas.length; i++) {
            if (colunas[i].trim().equalsIgnoreCase(nomeColuna)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Coluna '" + nomeColuna + "' não encontrada no cabeçalho.");
    }
    public String buscarFkEstado(String sigla) throws SQLException {
        String sql = "SELECT nomeEstado FROM estado WHERE sigla = ?";

        try (PreparedStatement stmt = bancoOperacoes.getConexao().prepareStatement(sql)) {
            stmt.setString(1, sigla);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("nomeEstado");
                }
            }
        }
        return null;
    }
}
