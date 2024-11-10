package infrastructure.database;

import infrastructure.processing.workbook.ManipularArquivo;
import infrastructure.processing.workbook.ManipularPlanilha;
import org.apache.poi.util.IOUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class BancoSetup {

    private final Connection conexao;
    BancoOperacoes bancoOperacoes = new BancoOperacoes();
    BancoInsert bancoInsert = new BancoInsert(bancoOperacoes);
    ManipularArquivo manipularArquivo = new ManipularArquivo();
    public BancoSetup(Connection conexao) {
        this.conexao = conexao;
    }

    public void criarEstruturaBanco() throws SQLException {

        try (Statement stmt = conexao.createStatement()) {

            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS techmentor");
            stmt.executeUpdate("USE techmentor");

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS estado (
                    nomeEstado VARCHAR(100) PRIMARY KEY,
                    sigla char(2),
                    regiao VARCHAR(100)
                )
            """);
            System.out.println("Tabela 'estado' criada com sucesso.");

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS cidade (
                    nomeCidade VARCHAR(100) PRIMARY KEY,
                    fkEstado VARCHAR(100),
                    FOREIGN KEY (fkEstado) REFERENCES estado(nomeEstado)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS municipio (
                    idMunicipio INT AUTO_INCREMENT PRIMARY KEY,
                    fkCidade VARCHAR(100),
                    ano CHAR(4),
                    operadora VARCHAR(100),
                    domiciliosCobertosPercent DECIMAL(10,2),
                    areaCobertaPercent DECIMAL(5,2),
                    tecnologia VARCHAR(50),
                    FOREIGN KEY (fkCidade) REFERENCES cidade(nomeCidade)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS estacoesSMP (
                    idEstacoesSMP INT AUTO_INCREMENT PRIMARY KEY,
                    fkCidade VARCHAR(255),
                    operadora VARCHAR(255),
                    latitude DECIMAL(10,8),
                    longitude DECIMAL(11,8),
                    codigoIBGE VARCHAR(255),
                    tecnologia VARCHAR(255),
                    FOREIGN KEY (fkCidade) REFERENCES cidade(nomeCidade)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS censoIBGE (
                    idCensoIBGE INT AUTO_INCREMENT PRIMARY KEY,
                    fkCidade VARCHAR(100),
                    area DECIMAL(10,2),
                    densidadeDemografica DECIMAL(10,2),
                    FOREIGN KEY (fkCidade) REFERENCES cidade(nomeCidade)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS projecaoPopulacional (
                    idProjecaoPopulacional INT AUTO_INCREMENT PRIMARY KEY,
                    fkEstado VARCHAR(100),
                    ano INT,
                    projecao INT,
                    FOREIGN KEY (fkEstado) REFERENCES estado(nomeEstado)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS empresa (
                    cnpj VARCHAR(20) PRIMARY KEY NOT NULL UNIQUE,
                    nomeEmpresa VARCHAR(100) NOT NULL,
                    nomeResponsavel VARCHAR(100),
                    emailResponsavel VARCHAR(100) NOT NULL,
                    senha VARCHAR(100) NOT NULL
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS cargo (
                    nomeCargo VARCHAR(100) PRIMARY KEY NOT NULL,
                    acessos VARCHAR(100),
                    fkCnpj VARCHAR(20),
                    FOREIGN KEY (fkCnpj) REFERENCES empresa(cnpj)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS usuario (
                    cpf VARCHAR(20) PRIMARY KEY,
                    email VARCHAR(100),
                    nomeUsuario VARCHAR(100),
                    senha VARCHAR(100),
                    fkCnpj VARCHAR(20),
                    fkNomeCargo VARCHAR(100),
                    FOREIGN KEY (fkCnpj) REFERENCES empresa(cnpj),
                    FOREIGN KEY (fkNomeCargo) REFERENCES cargo(nomeCargo)
                )
            """);
            bancoInsert.inserirDadosIniciais();
            IOUtils.setByteArrayMaxOverride(250_000_000);

            String nomeArquivo = "Meu_Municipio_Cobertura.xlsx";
            String caminhoArquivo = "/app/base-dados" + "/" + nomeArquivo;

            List<List<Object>> dados = manipularArquivo.lerPlanilha(caminhoArquivo, false);

            List<String> cidades = bancoInsert.extrairCidades(dados);
            bancoInsert.inserirCidades(cidades);

            System.out.println("Estrutura do banco de dados criada com sucesso!");
        } catch (SQLException e) {
            System.err.println("Erro ao criar estrutura do banco de dados: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
