package infrastructure.database;

import config.Configuracoes;
import infrastructure.processing.workbook.ManipularArquivo;
import org.apache.poi.util.IOUtils;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class BancoSetup {

    private final Connection conexao;
    private BancoInsert bancoInsert;
    private ManipularArquivo manipularArquivo;

    public BancoSetup(Connection conexao, BancoInsert bancoInsert, ManipularArquivo manipularArquivo) {
        this.conexao = conexao;
        this.bancoInsert = bancoInsert;
        this.manipularArquivo = manipularArquivo;
    }

    public void criarEstruturaBanco() throws SQLException {

        try (Statement stmt = conexao.createStatement()) {

            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + Configuracoes.DATABASE.getValor());
            stmt.executeUpdate("USE " + Configuracoes.DATABASE.getValor());

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS estado (
                    nomeEstado VARCHAR(100) PRIMARY KEY,
                    sigla char(2),
                    regiao VARCHAR(25),
                    qtdAntenas int,
                    maiorOperadora varchar(100)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS cidade (
                    nomeCidade VARCHAR(100) PRIMARY KEY,
                    fkEstado VARCHAR(100),
                    UNIQUE(nomeCidade, fkEstado),
                    FOREIGN KEY (fkEstado) REFERENCES estado(nomeEstado)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS baseMunicipio (
                    idMunicipio INT AUTO_INCREMENT PRIMARY KEY,
                    fkCidade VARCHAR(100),
                    ano CHAR(4),
                    operadora VARCHAR(25),
                    domiciliosCobertosPercentual DECIMAL(5,2),
                    areaCobertaPercentual DECIMAL(5,2),
                    tecnologia VARCHAR(15),
                    FOREIGN KEY (fkCidade) REFERENCES cidade(nomeCidade)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS baseEstacoesSMP (
                    idEstacoesSMP INT AUTO_INCREMENT PRIMARY KEY,
                    fkCidade VARCHAR(100),
                    operadora VARCHAR(25),
                    codigoIBGE VARCHAR(25),
                    tecnologia VARCHAR(25),
                    FOREIGN KEY (fkCidade) REFERENCES cidade (nomeCidade)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS baseCensoIBGE (
                    idCensoIBGE INT AUTO_INCREMENT PRIMARY KEY,
                    fkCidade VARCHAR(100),
                    area DECIMAL(10,2),
                    densidadeDemografica DECIMAL(10,2),
                    FOREIGN KEY (fkCidade) REFERENCES cidade(nomeCidade)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS baseProjecaoPopulacional (
                    idProjecaoPopulacional INT AUTO_INCREMENT PRIMARY KEY,
                    fkEstado VARCHAR(100),
                    ano INT,
                    projecao INT,
                    FOREIGN KEY (fkEstado) REFERENCES estado(nomeEstado)
                  )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS empresa (
                    cnpj VARCHAR(20) PRIMARY KEY,
                    nomeEmpresa VARCHAR(100) NOT NULL,
                    nomeResponsavel VARCHAR(100),
                    emailResponsavel VARCHAR(100) NOT NULL,
                    senha VARCHAR(100) NOT NULL,
                    webhook VARCHAR(100),
                    imagemPerfil VARCHAR(200)
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
                    imagemPerfil VARCHAR(100),
                    fkCnpj VARCHAR(20),
                    fkNomeCargo VARCHAR(100),
                    FOREIGN KEY (fkCnpj) REFERENCES empresa(cnpj),
                    FOREIGN KEY (fkNomeCargo) REFERENCES cargo(nomeCargo)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS historico (
                     idHistorico INT AUTO_INCREMENT,
                     dataAcesso DATE,
                     fkCpf VARCHAR(20),
                     PRIMARY KEY (idHistorico, fkCpf),
                     FOREIGN KEY (fkCpf) REFERENCES usuario (cpf)
                 );
            """);

            stmt.executeUpdate("""
                 CREATE TABLE IF NOT EXISTS notificacao (
                     idNotificacao INT PRIMARY KEY AUTO_INCREMENT,
                     texto VARCHAR(150),
                     dataCriacao DATETIME DEFAULT CURRENT_TIMESTAMP,
                     paraEmpresa bool,
                     statusEnviada bool,
                     fkCnpj VARCHAR(20),
                     FOREIGN KEY (fkCnpj) REFERENCES empresa(cnpj));
                 """);

            bancoInsert.inserirDadosIniciais();
            IOUtils.setByteArrayMaxOverride(250_000_000);

            String nomeArquivo = "Meu_Municipio_Cobertura.xlsx";
            String caminhoArquivo = Configuracoes.CAMINHO_DIRETORIO_RAIZ.getValor() + "/" + nomeArquivo;

            List<List<Object>> dados = manipularArquivo.lerPlanilha(caminhoArquivo, false);

            List<String> cidades = bancoInsert.extrairCidades(dados);
            bancoInsert.inserirCidades(cidades);

        } catch (SQLException e) {
            System.err.println("Erro ao criar estrutura do banco de dados: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}