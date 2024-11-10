package usecases.estacoes_smp;

import domain.EstacoesSMP;
import infrastructure.database.BancoOperacoes;
import infrastructure.logging.Logger;
import infrastructure.utils.ValidacoesLinha;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InserirDados {

    ValidacoesLinha validacoesLinha = new ValidacoesLinha();
    EstacoesSMP estacoes = new EstacoesSMP();
    Logger loggerInsercoes = Logger.getLoggerInsercoes();

    void inserirDadosComTratamento(List<List<Object>> dadosExcel, Connection conexao, BancoOperacoes bancoDeDados) throws SQLException, ClassNotFoundException {
        bancoDeDados.validarConexao();
        bancoDeDados.truncarTabela("estacoesSMP");

        System.out.println("Inserindo dados...");
//        loggerInsercoes.gerarLog("💻 Iniciando inserção de dados na tabela estacoesSMP... 💻");

        String query = "INSERT INTO estacoesSMP (fkCidade, operadora, codigoIBGE, tecnologia) VALUES (?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = conexao.prepareStatement(query)) {
            processarEInserirDados(dadosExcel, preparedStatement, bancoDeDados);
            System.out.println("Dados inseridos com sucesso!");
        }
    }

    private void processarEInserirDados(List<List<Object>> dadosExcel, PreparedStatement preparedStatement, BancoOperacoes bancoDeDados) {
        // Cache de índices das colunas para otimizar o código
        Map<String, Integer> indiceColunas = new HashMap<>();
        indiceColunas.put("Municipio", obterIndiceColuna(dadosExcel, "Município-UF"));
        indiceColunas.put("Operadora", obterIndiceColuna(dadosExcel, "Empresa Fistel"));
        indiceColunas.put("CodigoIBGE", obterIndiceColuna(dadosExcel, "Código IBGE"));
        indiceColunas.put("Tecnologia", obterIndiceColuna(dadosExcel, "Tecnologia"));

        for (int i = 1; i < dadosExcel.size(); i++) {
            List<Object> linha = dadosExcel.get(i);
            String[] valores = processarLinha(linha);

            try {
                // Tenta extrair e inserir os valores; se falhar, loga o erro e continua
                if (extraindoValoresDoApache(preparedStatement, valores, linha, indiceColunas)) {
                    bancoDeDados.adicionarBatch(preparedStatement, i);
                }
            } catch (SQLException e) {
                System.err.println("Erro ao processar a linha " + i + ": " + e.getMessage());
                // Opcional: loggerInsercoes.gerarLog("Erro ao processar a linha " + i + ": " + e.getMessage());
            } catch (Exception e) {
                System.err.println("Erro inesperado na linha " + i + ": " + e.getMessage());
                // Opcional: loggerInsercoes.gerarLog("Erro inesperado na linha " + i + ": " + e.getMessage());
            }
        }
    }


    private int obterIndiceColuna(List<List<Object>> dadosExcel, String nomeColuna) {
        if (dadosExcel == null || dadosExcel.isEmpty() || dadosExcel.get(0).isEmpty()) {
            throw new IllegalArgumentException("O cabeçalho está vazio ou mal formado.");
        }

        String cabecalho = dadosExcel.get(0).toString();

        // Remover o BOM (Byte Order Mark) da primeira célula do cabeçalho, se presente
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

    private boolean extraindoValoresDoApache(PreparedStatement preparedStatement, String[] valores, List<Object> linha, Map<String, Integer> indiceColunas) throws SQLException {
        if (valores.length < 29) {
            System.err.println("Linha inválida, ignorando: " + linha);
            return false;
        }

        // Aplica o método formatarCidade para remover o sufixo do estado
        estacoes.setCidade(formatarCidade(validacoesLinha.buscarValorValido(valores, indiceColunas.get("Municipio") + 2)));
        if (estacoes.getCidade().matches("\\d+")) {
            return false;
        }

        estacoes.setOperadora(validacoesLinha.buscarValorValido(valores, indiceColunas.get("Operadora")));
        estacoes.setCodigoIBGE(validacoesLinha.buscarValorValido(valores, indiceColunas.get("CodigoIBGE")));
        estacoes.setTecnologia(validacoesLinha.buscarValorValido(valores, indiceColunas.get("Tecnologia")));

        if (validacoesLinha.algumCampoInvalido(
                estacoes.getCidade(),
                estacoes.getOperadora(),
                estacoes.getCodigoIBGE(),
                estacoes.getTecnologia()
        )) {
            return false;
        }
        guardarValorProBanco(preparedStatement, estacoes.getCidade(), estacoes.getOperadora(), estacoes.getCodigoIBGE(), estacoes.getTecnologia());
        return true;
    }


    private String formatarCidade(String cidade) {
        if (cidade != null && cidade.contains("-")) {
            return cidade.split("-")[0].trim(); // Remove o estado após o "-"
        }
        return cidade;
    }

    private String[] processarLinha(List<Object> linha) {
        // Usando StringBuilder para manipulação de strings
        StringBuilder linhaBuilder = new StringBuilder();
        for (Object valor : linha) {
            linhaBuilder.append(valor).append(";");
        }
        return linhaBuilder.toString().split(";");
    }

    private void guardarValorProBanco(PreparedStatement preparedStatement, String cidade, String operadora, String codigoIBGE, String tecnologia) throws SQLException {
        preparedStatement.setString(1, cidade);
        preparedStatement.setString(2, operadora);
        preparedStatement.setString(3, codigoIBGE);
        preparedStatement.setString(4, tecnologia);
    }
}
