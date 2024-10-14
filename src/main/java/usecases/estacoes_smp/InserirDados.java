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

        String _longitude = validadacoesLinha.buscarValorValido(valores, 11);
        String _latitude = validadacoesLinha.buscarValorValido(valores, 12);

        if (_longitude == null || _latitude == null) {
            return false;
        }

        Long longitude = Long.parseLong(_longitude.replace(".", ""));
        Long latitude = Long.parseLong(_latitude.replace(".", ""));

        estacoes.setCidade(validadacoesLinha.buscarValorValido(valores, 28));
        estacoes.setOperadora(validadacoesLinha.buscarValorValido(valores, 4));
        estacoes.setLatitude(latitude);
        estacoes.setLongitude(longitude);
        estacoes.setCodigoIBGE(validadacoesLinha.buscarValorValido(valores, 25));
        estacoes.setTecnologia(validadacoesLinha.buscarValorValido(valores, 9));

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
        return linhaConvertida.split(";");
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
