package application;

import infrastructure.database.BancoOperacoes;
import infrastructure.logging.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public abstract class BaseDeDados {

    protected Logger logger;

    public abstract int obterIndiceColuna(List<List<Object>> dadosExcel, String nomeColuna);
    public abstract void inserirDadosComTratamento(List<List<Object>> dadosExcel, Connection conexao, BancoOperacoes bancoDeDados) throws SQLException, ClassNotFoundException;
    public abstract void guardarValorParaOBanco(PreparedStatement preparedStatement) throws SQLException;
    public abstract void processarEInserirDados(List<List<Object>> dadosExcel, PreparedStatement preparedStatement, BancoOperacoes bancoDeDados) throws SQLException;

    public boolean algumCampoInvalido(Object... campos) {
        for (Object campo : campos) {
            if (campo == null) {
                return true;
            }
            if (campo instanceof String && ((String) campo).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public String[] processarLinha(List<Object> linha) {
        String linhaConvertida = formatarLinha (linha);
        return linhaConvertida.split(";");
    }

    public String formatarLinha(List<Object> row) {
        StringBuilder linha = new StringBuilder();
        for (Object celula : row) {
            if (!linha.isEmpty()) {
                linha.append(";");
            }
            linha.append(celula != null ? celula.toString() : "");
        }
        return linha.toString();
    }

    public String buscarValorValido(String[] valores, int index) {
        if (index < valores.length) {
            String valor = valores[index];
            if (!valor.isEmpty()) {
                return valor;
            }
        }
        return null;
    }
}
