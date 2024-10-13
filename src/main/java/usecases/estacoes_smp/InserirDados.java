package usecases.estacoes_smp;

import infraestructure.database.BancoOperacoes;
import infraestructure.ValidacoesLinha;

import java.sql.*;
import java.util.List;

public class InserirDados {

    ValidacoesLinha validadacoesLinha = new ValidacoesLinha();

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

            // Se a linha não é válida, pula para a próxima
            if (!extraindoValoresDoApache(preparedStatement, valores, linha)) {
                continue;
            }
            bancoDeDados.adicionarBatch(preparedStatement, i);
        }
    }

    private boolean extraindoValoresDoApache(PreparedStatement preparedStatement, String[] valores, List<Object> linha) throws SQLException {
        if (valores.length < 29) {
            System.err.println("Linha inválida, ignorando: " + linha);
            return false;
        }

        // Agrupando a extração e validação dos valores
        String nomeDaUF = validadacoesLinha.buscarValorValido(valores, 28);
        String empresaFistel = validadacoesLinha.buscarValorValido(valores, 4);
        String _latitude = validadacoesLinha.buscarValorValido(valores, 11);
        String _longitude = validadacoesLinha.buscarValorValido(valores, 12);
        String codigoIBGE = validadacoesLinha.buscarValorValido(valores, 25);
        String tecnologia = validadacoesLinha.buscarValorValido(valores, 9);

        // Verifica se algum campo é inválido antes de inserir no banco
        if (validadacoesLinha.algumCampoInvalido(nomeDaUF, empresaFistel, _latitude, _longitude, codigoIBGE, tecnologia)) {
            return false;
        }
        Long longitude = null;
        Long latitude = null;

        if (_longitude != null && _latitude != null) {
            longitude = Long.parseLong(_longitude.replace(".", ""));
            latitude = Long.parseLong(_latitude.replace(".", ""));
        }
        guardarValorProBanco(preparedStatement, nomeDaUF, empresaFistel, latitude, longitude, codigoIBGE, tecnologia);
        return true;
    }

    // Método auxiliar para processar cada linha e retornar os valores como array de String
    private String[] processarLinha(List<Object> linha) {
        String linhaConvertida = validadacoesLinha.buscarValorValido(linha);
        return linhaConvertida.split(";");
    }

    // Preenche o PreparedStatement com os valores corretos
    private void guardarValorProBanco(PreparedStatement guardarValor, String nomeDaUF, String empresaFistel, Long latitude, Long longitude, String codigoIBGE, String tecnologia) throws SQLException {
        guardarValor.setString(1, nomeDaUF);
        guardarValor.setString(2, empresaFistel);
        guardarValor.setDouble(3, latitude);
        guardarValor.setDouble(4, longitude);
        guardarValor.setString(5, codigoIBGE);
        guardarValor.setString(6, tecnologia);
    }
}