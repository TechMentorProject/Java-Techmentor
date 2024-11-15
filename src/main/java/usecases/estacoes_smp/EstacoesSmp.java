package usecases.estacoes_smp;

import infrastructure.database.BancoOperacoes;
import infrastructure.logging.Logger;
import infrastructure.utils.ValidacoesLinha;
import usecases.BaseDeDados;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

public class EstacoesSmp extends BaseDeDados {

    private String cidade;
    private String operadora;
    private String codigoIBGE;
    private String tecnologia;
    private ValidacoesLinha validacoesLinha;
    private Logger loggerInsercoes;

    private int linhasInseridas;
    private int linhasRemovidas;

    public EstacoesSmp(ValidacoesLinha validacoesLinha, Logger loggerInsercoes) {
        this.validacoesLinha = validacoesLinha;
        this.loggerInsercoes = loggerInsercoes;
    }

    public void inserirDadosComTratamento(List<List<Object>> dadosExcel, Connection conexao, BancoOperacoes bancoDeDados) throws SQLException, ClassNotFoundException {
        bancoDeDados.validarConexao();
        bancoDeDados.truncarTabela("baseEstacoesSMP");

        linhasInseridas = 0;
        linhasRemovidas = 0;

        System.out.println("Inserindo dados...");
        loggerInsercoes.gerarLog("💻 Iniciando inserção de dados na tabela estacoesSMP... 💻");

        String query = "INSERT INTO baseEstacoesSMP (fkCidade, operadora, codigoIBGE, tecnologia) VALUES (?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = conexao.prepareStatement(query)) {
            processarEInserirDados(dadosExcel, preparedStatement, bancoDeDados);

            System.out.println("Linhas inseridas: " + linhasInseridas);
            System.out.println("Linhas removidas: " + linhasRemovidas);
            System.out.println("Inserção da baseEstacoesSMP concluída com sucesso!");
        }
    }

    private void processarEInserirDados(List<List<Object>> dadosExcel, PreparedStatement preparedStatement, BancoOperacoes bancoDeDados) {
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
                if (valores.length >= 4 && extraindoValoresDoApache(preparedStatement, valores, indiceColunas)) {
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

    private boolean extraindoValoresDoApache(PreparedStatement preparedStatement, String[] valores, Map<String, Integer> indiceColunas) throws SQLException {
        setCidade(formatarCidade(validacoesLinha.buscarValorValido(valores, indiceColunas.get("Municipio"))));
        if (getCidade().matches("\\d+")) {
            return false;
        }

        setOperadora(validacoesLinha.buscarValorValido(valores, indiceColunas.get("Operadora")));
        setCodigoIBGE(validacoesLinha.buscarValorValido(valores, indiceColunas.get("CodigoIBGE")));
        setTecnologia(validacoesLinha.buscarValorValido(valores, indiceColunas.get("Tecnologia")));

        if (validacoesLinha.algumCampoInvalido(
                getCidade(),
                getOperadora(),
                getCodigoIBGE(),
                getTecnologia()
        )) {
            return false;
        }
        guardarValorProBanco(preparedStatement, getCidade(), getOperadora(), getCodigoIBGE(), getTecnologia());
        return true;
    }

    private String formatarCidade(String cidade) {
        if (cidade != null && cidade.contains("-")) {
            return cidade.split("-")[0].trim();
        }
        return cidade;
    }

    private String[] processarLinha(List<Object> linha) {
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

    private void guardarValorProBanco(PreparedStatement preparedStatement, String cidade, String operadora, String codigoIBGE, String tecnologia) throws SQLException {
        preparedStatement.setString(1, cidade);
        preparedStatement.setString(2, operadora);
        preparedStatement.setString(3, codigoIBGE);
        preparedStatement.setString(4, tecnologia);
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

    public String getCodigoIBGE() {
        return codigoIBGE;
    }

    public void setCodigoIBGE(String codigoIBGE) {
        this.codigoIBGE = codigoIBGE;
    }

    public String getTecnologia() {
        return tecnologia;
    }

    public void setTecnologia(String tecnologia) {
        this.tecnologia = tecnologia;
    }
}
