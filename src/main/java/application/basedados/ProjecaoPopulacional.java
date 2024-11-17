package application.basedados;

import application.BaseDeDados;
import infrastructure.database.BancoOperacoes;
import infrastructure.logging.Logger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjecaoPopulacional extends BaseDeDados {

    private String estado;
    private int ano;
    private long projecao;

    int linhasInseridas = 0;

    public ProjecaoPopulacional(Logger logger) {
        this.loggerInsercoes = logger;
    }

    public void inserirDadosComTratamento(List<List<Object>> dadosExcel, Connection conexao, BancoOperacoes bancoDeDados) throws SQLException, ClassNotFoundException {
        bancoDeDados.validarConexao();
        bancoDeDados.truncarTabela("baseProjecaoPopulacional");

        System.out.println("Inserindo dados...");
        loggerInsercoes.gerarLog("💻 Iniciando inserção de dados na tabela projecaoPopulacional... 💻");

        String query = "INSERT INTO baseProjecaoPopulacional (fkEstado, ano, projecao) VALUES (?, ?, ?)";
        try (PreparedStatement preparedStatement = conexao.prepareStatement(query)) {
            processarEInserirDados(dadosExcel, preparedStatement, bancoDeDados);
            preparedStatement.executeBatch();
            conexao.commit();
            System.out.println("Linhas inseridas: " + linhasInseridas);
            System.out.println("Inserção da baseProjecaoPopulacional concluída com sucesso!");
        }
    }

    public void processarEInserirDados(List<List<Object>> dadosExcel, PreparedStatement preparedStatement, BancoOperacoes bancoDeDados) throws SQLException {
        int anoAtual = Year.now().getValue();

        Map<String, Integer> indiceColunas = new HashMap<>();
        indiceColunas.put("local", obterIndiceColuna(dadosExcel, "local"));
        indiceColunas.put("idade", obterIndiceColuna(dadosExcel, "idade"));

        for (int j = 0; j < 5; j++) { // Cache dos anos subsequentes
            indiceColunas.put(String.valueOf(anoAtual + j), obterIndiceColuna(dadosExcel, String.valueOf(anoAtual + j)));
        }

        String estadoAtual = null;
        Map<Integer, Long> somaProjecoes = new HashMap<>();

        for (int i = 5; i < dadosExcel.size(); i++) {
            List<Object> linha = dadosExcel.get(i);
            String[] valores = processarLinha(linha);

            String estado = buscarValorValido(valores, indiceColunas.get("local")).replace(".", "");

            if (estado == null || contemPalavrasProibidas(estado, linha)) {
                continue;
            }

            if (!estado.equals(estadoAtual)) {
                if (estadoAtual != null) {
                    for (Map.Entry<Integer, Long> entry : somaProjecoes.entrySet()) {
                        setEstado(estadoAtual);
                        setAno(entry.getKey());
                        setProjecao(entry.getValue());
                        linhasInseridas++;
                        guardarValorParaOBanco(preparedStatement);
                    }
                    somaProjecoes.clear();
                }
                estadoAtual = estado;
            }

            int idade = Integer.parseInt(buscarValorValido(valores, indiceColunas.get("idade")));
            if (idade >= 0 && idade <= 90) {
                for (int j = 0; j < 5; j++) {
                    int ano = anoAtual + j;
                    long projecaoAno = filtrarLongComPonto(buscarValorValido(valores, indiceColunas.get(String.valueOf(ano))));
                    somaProjecoes.put(ano, somaProjecoes.getOrDefault(ano, 0L) + projecaoAno);
                }
            }
        }

        for (Map.Entry<Integer, Long> entry : somaProjecoes.entrySet()) {
            setEstado(estadoAtual);
            setAno(entry.getKey());
            setProjecao(entry.getValue());
            guardarValorParaOBanco(preparedStatement);
            linhasInseridas++;
        }

    }

    public int obterIndiceColuna(List<List<Object>> dadosExcel, String nomeColuna) {
        String cabecalho = dadosExcel.get(4).toString().replace("[", "").replace("]", "");
        if (cabecalho.length() > 0 && cabecalho.charAt(0) == '\uFEFF') {
            cabecalho = cabecalho.substring(1);
        }

        String[] colunas = cabecalho.split(",");
        for (int i = 0; i < colunas.length; i++) {
            if (colunas[i].trim().equalsIgnoreCase(nomeColuna)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Coluna '" + nomeColuna + "' não encontrada no cabeçalho.");
    }

    private boolean contemPalavrasProibidas(String estado, List<Object> linha) {
        StringBuilder linhaLowerCase = new StringBuilder();
        for (Object celula : linha) {
            if (celula != null) {
                linhaLowerCase.append(celula.toString().toLowerCase()).append(" ");
            }
        }

        return estado.equalsIgnoreCase("sul") || estado.equalsIgnoreCase("norte") || estado.equalsIgnoreCase("local")
                || linhaLowerCase.toString().contains("brasil") || linhaLowerCase.toString().contains("homens")
                || linhaLowerCase.toString().contains("mulheres") || linhaLowerCase.toString().contains("centro-oeste")
                || linhaLowerCase.toString().contains("sudeste") || linhaLowerCase.toString().contains("nordeste");
    }

    private long filtrarLongComPonto(String numeroEmString) {
        if (numeroEmString != null) {
            numeroEmString = numeroEmString.replace(".", "").replace(",", "");
            return Long.parseLong(numeroEmString);
        }
        return 0;
    }

    public void guardarValorParaOBanco(PreparedStatement preparedStatement) throws SQLException {
        preparedStatement.setString(1, getEstado());
        preparedStatement.setInt(2, getAno());
        preparedStatement.setLong(3, getProjecao());
        preparedStatement.addBatch();
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public int getAno() {
        return ano;
    }

    public void setAno(int ano) {
        this.ano = ano;
    }

    public long getProjecao() {
        return projecao;
    }

    public void setProjecao(long projecao) {
        this.projecao = projecao;
    }

}
