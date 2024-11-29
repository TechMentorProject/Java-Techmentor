package infrastructure.database;

import application.BaseDeDados;
import java.sql.*;
import java.util.*;

public class BancoInsert {

    private final BancoOperacoes bancoOperacoes;
    private BaseDeDados baseDeDados;

    public BancoInsert(BancoOperacoes bancoOperacoes, BaseDeDados baseDeDados) {
        this.bancoOperacoes = bancoOperacoes;
        this.baseDeDados = baseDeDados;
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
        Integer indiceColunaCidadeEstado = obterIndiceColuna(dadosExcel, "Município");

        for (int i = 1; i < dadosExcel.size(); i++) {
            List<Object> linha = dadosExcel.get(i);
            List<String> valores = Arrays.stream(baseDeDados.processarLinha(linha)).toList();

            String cidadeEstadoOriginal = valores.get(indiceColunaCidadeEstado).trim();

            if (cidadeEstadoOriginal.contains("�?")) {
                continue;
            }
            
            cidadesSet.add(cidadeEstadoOriginal);
        }
        return new ArrayList<>(cidadesSet);
    }

    public int obterIndiceColuna(List<List<Object>> dadosExcel, String nomeColuna) {
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
    public void inserirCidades(List<String> cidadesComSigla) {
        try {
            bancoOperacoes.validarConexao();
            Connection conexao = bancoOperacoes.getConexao();
            conexao.setAutoCommit(false);

            for (String cidadeComSigla : cidadesComSigla) {
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
            String[] partes = cidadeComSigla.split(" - ");
            if (partes.length != 2) {
                throw new IllegalArgumentException("Formato inválido. Esperado: 'Cidade - Sigla'");
            }

            String nomeCidade = partes[0].trim();
            String siglaEstado = limparSigla(partes[1].trim());

            String nomeEstado = buscarFkEstado(siglaEstado);
            if (nomeEstado == null) {
                throw new SQLException("Estado com sigla '" + siglaEstado + "' não encontrado.");
            }

            if (!cidadeJaExiste(nomeCidade, nomeEstado)) {
                String sqlInsert = "INSERT INTO cidade (nomeCidade, fkEstado) VALUES (?, ?)";
                try (PreparedStatement stmt = bancoOperacoes.getConexao().prepareStatement(sqlInsert)) {
                    if (nomeCidade.startsWith("\"")) {
                        nomeCidade = nomeCidade.substring(1);
                    }
                    stmt.setString(1, nomeCidade);
                    stmt.setString(2, nomeEstado);
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

    private boolean cidadeJaExiste(String nomeCidade, String nomeEstado) throws SQLException {
        String sqlVerifica = "SELECT COUNT(*) FROM cidade WHERE LOWER(nomeCidade) = LOWER(?) AND fkEstado = ?";
        try (PreparedStatement stmt = bancoOperacoes.getConexao().prepareStatement(sqlVerifica)) {
            if (nomeCidade.startsWith("\"")) {
                nomeCidade = nomeCidade.substring(1);
            }
            stmt.setString(1, nomeCidade);
            stmt.setString(2, nomeEstado);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
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
