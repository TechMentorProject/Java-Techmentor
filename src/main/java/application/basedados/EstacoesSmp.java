package application.basedados;

import application.BaseDeDados;
import infrastructure.database.BancoOperacoes;
import infrastructure.logging.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

public class EstacoesSmp extends BaseDeDados {

    private String cidade;
    private String operadora;
    private String codigoIbge;
    private String tecnologia;

    private int linhasInseridas;
    private int linhasRemovidas;

    public EstacoesSmp(Logger logger) {
        this.logger = logger;
    }

    public void inserirDadosComTratamento(List<List<Object>> dadosExcel, Connection conexao, BancoOperacoes bancoDeDados) throws SQLException, ClassNotFoundException {
        bancoDeDados.validarConexao();
        bancoDeDados.truncarTabela("baseEstacoesSMP");

        linhasInseridas = 0;
        linhasRemovidas = 0;

        System.out.println("Inserindo dados...");
        logger.getLoggerEventos().gerarLog("💻 Iniciando inserção de dados na tabela estacoesSMP... 💻");

        String query = "INSERT INTO baseEstacoesSMP (fkCidade, operadora, codigoIBGE, tecnologia) VALUES (?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = conexao.prepareStatement(query)) {
            processarEInserirDados(dadosExcel, preparedStatement, bancoDeDados);

            System.out.println("Linhas inseridas: " + linhasInseridas);
            System.out.println("Linhas removidas: " + linhasRemovidas);
            System.out.println("Inserção da baseEstacoesSMP concluída com sucesso!");
        }
    }

    public void processarEInserirDados(List<List<Object>> dadosExcel, PreparedStatement preparedStatement, BancoOperacoes bancoDeDados) {
        Map<String, Integer> indiceColunas = new HashMap<>();
        indiceColunas.put("Municipio", obterIndiceColuna(dadosExcel, "Município-UF"));
        indiceColunas.put("Operadora", obterIndiceColuna(dadosExcel, "Empresa Estação"));
        indiceColunas.put("CodigoIBGE", obterIndiceColuna(dadosExcel, "Geração"));
        indiceColunas.put("Tecnologia", obterIndiceColuna(dadosExcel, "Tecnologia"));

        for (int i = 1; i < dadosExcel.size(); i++) {
            List<Object> linha = dadosExcel.get(i);
            String[] valores = processarLinha(linha);

            if (valores.length < 41) {
                List<String> listaValores = new ArrayList<>(Arrays.asList(valores));

                while (listaValores.size() < 41) {
                    listaValores.add(0, "");
                }

                valores = listaValores.toArray(new String[0]);

            } else if (valores.length > 41) {
                List<String> listaValores = new ArrayList<>(Arrays.asList(valores));

                while (listaValores.size() > 41) {
                    listaValores.remove(0); // Remove o elemento do início
                }

                valores = listaValores.toArray(new String[0]);
            }

            try {
                if (valores.length >= 4 && extrairValoresDasEstacoes(preparedStatement, valores, indiceColunas)) {
                    bancoDeDados.adicionarBatch(preparedStatement, i);
                    linhasInseridas++;
                }
            } catch (SQLException e) {
                linhasRemovidas++;
            } catch (Exception e) {
                linhasRemovidas++;
            }
        }
    }

    public int obterIndiceColuna(List<List<Object>> dadosExcel, String nomeColuna) {
        if (dadosExcel == null || dadosExcel.isEmpty() || dadosExcel.get(0).isEmpty()) {
            throw new IllegalArgumentException("O cabeçalho está vazio ou mal formado.");
        }

        String cabecalho = dadosExcel.get(0).toString();
        if (cabecalho.charAt(0) == '\uFEFF') {
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

    private boolean extrairValoresDasEstacoes(PreparedStatement preparedStatement, String[] valores, Map<String, Integer> indiceColunas) throws SQLException {
        setCidade(formatarCidade(buscarValorValido(valores, indiceColunas.get("Municipio"))));
        if (getCidade().matches("\\d+")) {
            return false;
        }

        setOperadora(buscarValorValido(valores, indiceColunas.get("Operadora")));
        setCodigoIbge(buscarValorValido(valores, indiceColunas.get("CodigoIBGE")));
        setTecnologia(buscarValorValido(valores, indiceColunas.get("Tecnologia")));

        if (algumCampoInvalido(
                getCidade(),
                getOperadora(),
                getCodigoIbge(),
                getTecnologia()
        )) {
            return false;
        }
        guardarValorParaOBanco(preparedStatement);
        return true;
    }

    private String formatarCidade(String cidade) {
        if (cidade != null && cidade.contains("-")) {
            return cidade.split("-")[0].trim();
        }
        return cidade;
    }

    @Override
    public String[] processarLinha(List<Object> linha) {
        List<String> camposProcessados = new ArrayList<>();

        for (Object valor : linha) {
            if (valor != null && !valor.toString().isEmpty()) {
                String valorStr = valor.toString().trim();

                if (valorStr.matches("^00;\\d+$")) {
                    valorStr = valorStr.substring(3);
                }

                String[] subCampos = valorStr.split(";");
                for (String subCampo : subCampos) {
                    if (!subCampo.trim().isEmpty()) {
                        camposProcessados.add(subCampo.trim());
                    } else {
                        camposProcessados.add("");
                    }
                }
            } else {
                camposProcessados.add("");
            }
        }

        return camposProcessados.toArray(new String[0]);
    }

    public void guardarValorParaOBanco(PreparedStatement preparedStatement) throws SQLException {
        preparedStatement.setString(1, getCidade());
        preparedStatement.setString(2, getOperadora());
        preparedStatement.setString(3, getCodigoIbge());
        preparedStatement.setString(4, getTecnologia());
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public String getOperadora() {
        return operadora;
    }

    public void setOperadora(String operadora) {
        this.operadora = operadora;
    }

    public String getCodigoIbge() {
        return codigoIbge;
    }

    public void setCodigoIbge(String codigoIbge) {
        this.codigoIbge = codigoIbge;
    }

    public String getTecnologia() {
        return tecnologia;
    }

    public void setTecnologia(String tecnologia) {
        this.tecnologia = tecnologia;
    }
}
