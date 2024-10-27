package usecases.projecao_populacional;

import domain.ProjecaoPopulacional;
import infrastructure.database.BancoOperacoes;
import infrastructure.logging.Logger;
import infrastructure.utils.ValidacoesLinha;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InserirDados {

    ValidacoesLinha validacoesLinha = new ValidacoesLinha();
    ProjecaoPopulacional projecao = new ProjecaoPopulacional();
    Logger loggerInsercoes = Logger.getLoggerInsercoes();

    public void inserirDadosComTratamento(List<List<Object>> dadosExcel, Connection conexao, BancoOperacoes bancoDeDados) throws SQLException, ClassNotFoundException {
        bancoDeDados.validarConexao();
        bancoDeDados.truncarTabela("projecaoPopulacional");

        System.out.println("Inserindo dados...");
        loggerInsercoes.gerarLog("💻 Iniciando inserção de dados na tabela projecaoPopulacional... 💻");

        String query = "INSERT INTO projecaoPopulacional (estado, ano, projecao) VALUES (?, ?, ?)";
        try (PreparedStatement preparedStatement = conexao.prepareStatement(query)) {
            processarEInserirDados(dadosExcel, preparedStatement, bancoDeDados);
            preparedStatement.executeBatch();
            conexao.commit();
            System.out.println("Dados inseridos com sucesso!");
        }
    }

    private void processarEInserirDados(List<List<Object>> dadosExcel, PreparedStatement preparedStatement, BancoOperacoes bancoDeDados) throws SQLException {
        int anoAtual = Year.now().getValue();

        // Cache de índices das colunas
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
            String[] valores = validacoesLinha.processarLinha(linha);

            String estado = validacoesLinha.buscarValorValido(valores, indiceColunas.get("local")).replace(".", "");

            if (estado == null || contemPalavrasProibidas(estado, linha)) {
                continue;
            }

            if (!estado.equals(estadoAtual)) {
                // Insere projeções acumuladas no banco para o estado anterior
                if (estadoAtual != null) {
                    for (Map.Entry<Integer, Long> entry : somaProjecoes.entrySet()) {
                        inserirNoBanco(preparedStatement, estadoAtual, entry.getKey(), entry.getValue());
                    }
                    somaProjecoes.clear();
                }
                estadoAtual = estado;
            }

            int idade = Integer.parseInt(validacoesLinha.buscarValorValido(valores, indiceColunas.get("idade")));
            if (idade >= 0 && idade <= 90) {
                for (int j = 0; j < 5; j++) {
                    int ano = anoAtual + j;
                    long projecaoAno = parseLongWithDot(validacoesLinha.buscarValorValido(valores, indiceColunas.get(String.valueOf(ano))));
                    somaProjecoes.put(ano, somaProjecoes.getOrDefault(ano, 0L) + projecaoAno);
                }
            }
        }

        // Inserir as projeções para o último estado processado
        for (Map.Entry<Integer, Long> entry : somaProjecoes.entrySet()) {
            inserirNoBanco(preparedStatement, estadoAtual, entry.getKey(), entry.getValue());
        }
    }

    private int obterIndiceColuna(List<List<Object>> dadosExcel, String nomeColuna) {
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

    private long parseLongWithDot(String numeroEmString) {
        if (numeroEmString != null) {
            // Remove pontos e vírgulas da string antes de converter para Long
            numeroEmString = numeroEmString.replace(".", "").replace(",", "");
            return Long.parseLong(numeroEmString);
        }
        return 0;
    }


    private void inserirNoBanco(PreparedStatement preparedStatement, String estado, int ano, long projecao) throws SQLException {

        this.projecao.setEstado(estado);
        this.projecao.setAno(ano);
        this.projecao.setProjecao(projecao);

        preparedStatement.setString(1, this.projecao.getEstado());
        System.out.println(this.projecao.getEstado());
        preparedStatement.setInt(2, this.projecao.getAno());
        preparedStatement.setLong(3, this.projecao.getProjecao());
        preparedStatement.addBatch();
    }
}
