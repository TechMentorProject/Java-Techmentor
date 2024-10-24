package usecases.estacoes_smp;

import domain.EstacoesSMP;
import infrastructure.database.BancoOperacoes;
import infrastructure.utils.ValidacoesLinha;

import java.sql.*;
import java.util.List;

public class InserirDados {

    ValidacoesLinha validadacoesLinha = new ValidacoesLinha();
    EstacoesSMP estacoes = new EstacoesSMP();

    void inserirDadosComTratamento(List<List<Object>> dadosExcel, Connection conexao, BancoOperacoes bancoDeDados) throws SQLException {
        bancoDeDados.validarConexao();
        bancoDeDados.truncarTabela("estacoesSMP");

        System.out.println("Inserindo dados...");

        String query = "INSERT INTO estacoesSMP (cidade, operadora, latitude, longitude, codigoIBGE, tecnologia) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = conexao.prepareStatement(query)) {
            processarEInserirDados(dadosExcel, preparedStatement, bancoDeDados);
            preparedStatement.executeBatch();
            conexao.commit();
            System.out.println("Dados inseridos com sucesso!");
        }
    }

    private void processarEInserirDados(List<List<Object>> dadosExcel, PreparedStatement preparedStatement, BancoOperacoes bancoDeDados) throws SQLException {
        for (int i = 1; i < dadosExcel.size(); i++) {
            List<Object> linha = dadosExcel.get(i);
            String[] valores = processarLinha(linha);
            
            if (!extraindoValoresDoApache(preparedStatement, valores, linha, dadosExcel)) {
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
        if (cabecalho.length() > 0 && cabecalho.charAt(0) == '\uFEFF') {
            cabecalho = cabecalho.substring(1); // Remove o Byte Order Mark
        }

        String[] colunas = cabecalho.split(";");

        // Procurar a coluna pelo nome e retornar o índice correspondente
        for (int i = 0; i < colunas.length; i++) {
            String nomeAtual = colunas[i].trim(); // Remover espaços ao redor
            if (nomeAtual.equalsIgnoreCase(nomeColuna)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Coluna '" + nomeColuna + "' não encontrada no cabeçalho.");
    }


    private boolean extraindoValoresDoApache(PreparedStatement preparedStatement, String[] valores, List<Object> linha, List<List<Object>> dadosExcel) throws SQLException {
        if (valores.length < 29) {
            System.err.println("Linha inválida, ignorando: " + linha);
            return false;
        }

        int indiceMunicipio = obterIndiceColuna(dadosExcel, "Município-UF");
        int indiceOperadora = obterIndiceColuna(dadosExcel, "Empresa Fistel");
        int indiceLatitude = obterIndiceColuna(dadosExcel, "Latitude decimal");
        int indiceLongitude = obterIndiceColuna(dadosExcel, "Longitude decimal");
        int indiceCodigoIBGE = obterIndiceColuna(dadosExcel, "Código IBGE");
        int indiceTecnologia = obterIndiceColuna(dadosExcel, "Tecnologia");

        String _longitude = validadacoesLinha.buscarValorValido(valores, indiceLongitude);
        String _latitude = validadacoesLinha.buscarValorValido(valores, indiceLatitude);

        if (_longitude == null || _latitude == null) {
            return false;
        }

        Long longitude = Long.parseLong(_longitude.replace(".", ""));
        Long latitude = Long.parseLong(_latitude.replace(".", ""));

        estacoes.setCidade(validadacoesLinha.buscarValorValido(valores, indiceMunicipio + 2));
        if (estacoes.getCidade().matches("\\d+")) {
            return false;
        }
        estacoes.setOperadora(validadacoesLinha.buscarValorValido(valores, indiceOperadora));
        estacoes.setLatitude(latitude);
        estacoes.setLongitude(longitude);
        estacoes.setCodigoIBGE(validadacoesLinha.buscarValorValido(valores, indiceCodigoIBGE));
        estacoes.setTecnologia(validadacoesLinha.buscarValorValido(valores, indiceTecnologia));

        if (validadacoesLinha.algumCampoInvalido(
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
        String linhaConvertida = validadacoesLinha.buscarValorValido(linha);
        String[] colunas = linhaConvertida.split(";");
        return colunas;
    }

    private void guardarValorProBanco(PreparedStatement guardarValor, String cidade, String operadora, Long latitude, Long longitude, String codigoIBGE, String tecnologia) throws SQLException {
        guardarValor.setString(1, cidade);
        guardarValor.setString(2, operadora);
        guardarValor.setDouble(3, latitude);
        guardarValor.setDouble(4, longitude);
        guardarValor.setString(5, codigoIBGE);
        guardarValor.setString(6, tecnologia);
    }
}
