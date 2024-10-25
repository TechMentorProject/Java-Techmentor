package usecases.projecao_populacional;

import domain.ProjecaoPopulacional;
import infrastructure.database.BancoOperacoes;
import infrastructure.utils.ValidacoesLinha;

import java.sql.*;
import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;

public class InserirDados {

    ValidacoesLinha validacoesLinha = new ValidacoesLinha();
    ProjecaoPopulacional projecao = new ProjecaoPopulacional();

    public void inserirDadosComTratamento(List<List<Object>> dadosExcel, Connection conexao, BancoOperacoes bancoDeDados) throws SQLException {
        bancoDeDados.validarConexao();
        bancoDeDados.truncarTabela("projecaoPopulacional");


        System.out.println("Inserindo dados...");

        String query = "INSERT INTO projecaoPopulacional (estado, ano, projecao) VALUES (?, ?, ?)";
        try (PreparedStatement preparedStatement = conexao.prepareStatement(query)) {
            processarEInserirDados(dadosExcel, preparedStatement, bancoDeDados);
            preparedStatement.executeBatch();
            conexao.commit();
            System.out.println("Dados inseridos com sucesso!");
        }
    }

    private int obterIndiceColuna(List<List<Object>> dadosExcel, String nomeColuna) {
        String cabecalho = dadosExcel.get(4).toString();
        cabecalho = cabecalho.replace("[", "").replace("]", "");
        if (cabecalho.length() > 0 && cabecalho.charAt(0) == '\uFEFF') {
            cabecalho = cabecalho.substring(1);
        }
        String[] colunas = cabecalho.split(",");
        for (int i = 0; i < colunas.length; i++) {
            String nomeAtual = colunas[i].trim();
            if (nomeAtual.equalsIgnoreCase(nomeColuna)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Coluna '" + nomeColuna + "' não encontrada no cabeçalho.");
    }

    private void processarEInserirDados(List<List<Object>> dadosExcel, PreparedStatement preparedStatement, BancoOperacoes bancoDeDados) throws SQLException {
        int anoAtual = Year.now().getValue(); // Obter o ano atual dinamicamente

        for (int i = 5; i < dadosExcel.size(); i++) {
            List<Object> linha = dadosExcel.get(i);
            String[] valores = validacoesLinha.processarLinha(linha);
            String estado = validacoesLinha.buscarValorValido(valores, obterIndiceColuna(dadosExcel, "local")).replace(".", "");

            if (estado == null || contemPalavrasProibidas(estado, linha)) {
                continue; // Ignorar linhas proibidas ou inválidas
            }

            long[] somaProjecoes = new long[5];  // Array para acumular projeções dos próximos 5 anos

            // Processar projeções para o estado atual
            while (i < dadosExcel.size()) {
                List<Object> linhaInterna = dadosExcel.get(i);
                String[] valoresInternos = validacoesLinha.processarLinha(linhaInterna);
                String estadoInterno = validacoesLinha.buscarValorValido(valoresInternos, obterIndiceColuna(dadosExcel, "local")).replace(".", "");

                if (estadoInterno.equals(estado) && !contemPalavrasProibidas(estadoInterno, linhaInterna)) {
                    int idade = Integer.parseInt(validacoesLinha.buscarValorValido(valoresInternos, obterIndiceColuna(dadosExcel, "idade")));
                    if (idade >= 0 && idade <= 90) {
                        for (int j = 0; j < 5; j++) {
                            // Acumular projeções para anoAtual até anoAtual+4
                            somaProjecoes[j] += parseLongWithDot(
                                    validacoesLinha.buscarValorValido(valoresInternos, obterIndiceColuna(dadosExcel, String.valueOf(anoAtual + j)))
                            );
                        }
                    }
                } else {
                    // Inserir no banco as projeções acumuladas para os próximos 5 anos
                    for (int j = 0; j < 5; j++) {
                        inserirNoBanco(preparedStatement, estado, anoAtual + j, somaProjecoes[j]);
                    }
                    i--;  // Ajuste para não pular a primeira linha do novo estado
                    break;  // Sair do loop interno ao mudar de estado
                }
                i++;
            }
        }
    }

    private boolean contemPalavrasProibidas(String estado, List<Object> linha) {
        String linhaLowerCase = linha.stream()
                .map(celula -> celula != null ? celula.toString() : "")
                .collect(Collectors.joining(" "))
                .toLowerCase();

        if (estado.equalsIgnoreCase("sul") || estado.equalsIgnoreCase("norte") || estado.equalsIgnoreCase("local")) {
            return true;
        }

        return linhaLowerCase.contains("brasil") || linhaLowerCase.contains("homens") || linhaLowerCase.contains("mulheres")
                || linhaLowerCase.contains("centro-oeste") || linhaLowerCase.contains("sudeste") || linhaLowerCase.contains("nordeste");
    }

    private long parseLongWithDot(String numeroEmString) {
        if (numeroEmString != null) {
            numeroEmString = numeroEmString.replace(".", "");
            return Long.parseLong(numeroEmString);
        }
        return 0;
    }

    private void inserirNoBanco(PreparedStatement preparedStatement, String estado, int ano, long projecao) throws SQLException {

        this.projecao.setEstado(estado);
        this.projecao.setAno(ano);
        this.projecao.setProjecao(projecao);

        preparedStatement.setString(1, this.projecao.getEstado());
        preparedStatement.setInt(2, this.projecao.getAno());
        preparedStatement.setLong(3, this.projecao.getProjecao());
        preparedStatement.addBatch();
    }
}
