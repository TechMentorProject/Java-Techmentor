package usecases.estacoes_smp;

import domain.EstacoesSMP;
import infrastructure.database.BancoOperacoes;
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

    void inserirDadosComTratamento(List<List<Object>> dadosExcel, Connection conexao, BancoOperacoes bancoDeDados) throws SQLException {
        bancoDeDados.validarConexao();
        bancoDeDados.truncarTabela("estacoesSMP");

        System.out.println("Inserindo dados...");

        String query = "INSERT INTO estacoesSMP (cidade, operadora, latitude, longitude, codigoIBGE, tecnologia) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = conexao.prepareStatement(query)) {
            processarEInserirDados(dadosExcel, preparedStatement, bancoDeDados);
            System.out.println("Dados inseridos com sucesso!");
        }
    }

    private void processarEInserirDados(List<List<Object>> dadosExcel, PreparedStatement preparedStatement, BancoOperacoes bancoDeDados) throws SQLException {
        // Cache de índices das colunas para otimizar o código
        Map<String, Integer> indiceColunas = new HashMap<>();
        indiceColunas.put("Municipio", obterIndiceColuna(dadosExcel, "Município-UF"));
        indiceColunas.put("Operadora", obterIndiceColuna(dadosExcel, "Empresa Fistel"));
        indiceColunas.put("Latitude", obterIndiceColuna(dadosExcel, "Latitude decimal"));
        indiceColunas.put("Longitude", obterIndiceColuna(dadosExcel, "Longitude decimal"));
        indiceColunas.put("CodigoIBGE", obterIndiceColuna(dadosExcel, "Código IBGE"));
        indiceColunas.put("Tecnologia", obterIndiceColuna(dadosExcel, "Tecnologia"));

        for (int i = 1; i < dadosExcel.size(); i++) {
            List<Object> linha = dadosExcel.get(i);
            String[] valores = processarLinha(linha);

            if (!extraindoValoresDoApache(preparedStatement, valores, linha, indiceColunas)) {
                continue;
            }
            bancoDeDados.adicionarBatch(preparedStatement, i);
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
    
        // Extraindo valores usando os índices já cacheados
        String _longitude = validacoesLinha.buscarValorValido(valores, indiceColunas.get("Longitude"));
        String _latitude = validacoesLinha.buscarValorValido(valores, indiceColunas.get("Latitude"));

        if (_longitude == null || _latitude == null) {
            return false;
        }

        Long longitude = Long.parseLong(_longitude.replace(".", ""));
        Long latitude = Long.parseLong(_latitude.replace(".", ""));

        estacoes.setCidade(validacoesLinha.buscarValorValido(valores, indiceColunas.get("Municipio") + 2));
        if (estacoes.getCidade().matches("\\d+")) {
            return false;
        }
        estacoes.setOperadora(validacoesLinha.buscarValorValido(valores, indiceColunas.get("Operadora")));
        estacoes.setLatitude(latitude);
        estacoes.setLongitude(longitude);
        estacoes.setCodigoIBGE(validacoesLinha.buscarValorValido(valores, indiceColunas.get("CodigoIBGE")));
        estacoes.setTecnologia(validacoesLinha.buscarValorValido(valores, indiceColunas.get("Tecnologia")));

        if (validacoesLinha.algumCampoInvalido(
                estacoes.getCidade(),
                estacoes.getOperadora(),
                estacoes.getLatitude(),
                estacoes.getLongitude(),
                estacoes.getCodigoIBGE(),
                estacoes.getTecnologia()
        )) {
            return false;
        }
        guardarValorProBanco(preparedStatement, estacoes.getCidade(), estacoes.getOperadora(), estacoes.getLatitude(), estacoes.getLongitude(), estacoes.getCodigoIBGE(), estacoes.getTecnologia());
        return true;
    }

    private String[] processarLinha(List<Object> linha) {
        // Usando StringBuilder para manipulação de strings
        StringBuilder linhaBuilder = new StringBuilder();
        for (Object valor : linha) {
            linhaBuilder.append(valor).append(";");
        }
        return linhaBuilder.toString().split(";");
    }

    private void guardarValorProBanco(PreparedStatement preparedStatement, String cidade, String operadora, Long latitude, Long longitude, String codigoIBGE, String tecnologia) throws SQLException {
        preparedStatement.setString(1, cidade);
        preparedStatement.setString(2, operadora);
        preparedStatement.setDouble(3, latitude);
        preparedStatement.setDouble(4, longitude);
        preparedStatement.setString(5, codigoIBGE);
        preparedStatement.setString(6, tecnologia);
    }
}
