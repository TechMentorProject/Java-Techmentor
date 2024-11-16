package application;

import infrastructure.database.BancoOperacoes;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public abstract class BaseDeDados {

    public abstract int obterIndiceColuna(List<List<Object>> dadosExcel, String nomeColuna);
    public abstract void inserirDadosComTratamento(List<List<Object>> dadosExcel, Connection conexao, BancoOperacoes bancoDeDados) throws SQLException, ClassNotFoundException;

}
