package infrastructure.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class BancoSetup {

    private final Connection conexao;

    public BancoSetup(Connection conexao) {
        this.conexao = conexao;
    }

    public void criarEstruturaBanco() throws SQLException {
        try (Statement techmentor = conexao.createStatement()) {
            // Criação das tabelas
            techmentor.executeUpdate("""
                CREATE TABLE IF NOT EXISTS estado (
                    idEstado INT AUTO_INCREMENT PRIMARY KEY,
                    regiao VARCHAR(100),
                    UF CHAR(2)
                )
            """);

            techmentor.executeUpdate("""
                CREATE TABLE IF NOT EXISTS municipio (
                    idMunicipio INT AUTO_INCREMENT PRIMARY KEY,
                    ano CHAR(4),
                    cidade VARCHAR(100),
                    operadora VARCHAR(100),
                    domiciliosCobertosPercent DECIMAL(10,2),
                    areaCobertaPercent DECIMAL(5,2),
                    tecnologia VARCHAR(50)
                )
            """);

            techmentor.executeUpdate("""
                CREATE TABLE IF NOT EXISTS estacoesSMP (
                    idEstacoesSMP INT AUTO_INCREMENT PRIMARY KEY,
                    cidade VARCHAR(255),
                    operadora VARCHAR(255),
                    latitude BIGINT,
                    longitude BIGINT,
                    codigoIBGE VARCHAR(255),
                    tecnologia VARCHAR(255)
                )
            """);

            techmentor.executeUpdate("""
                CREATE TABLE IF NOT EXISTS censoIBGE (
                    idCensoIBGE INT AUTO_INCREMENT PRIMARY KEY,
                    cidade VARCHAR(100),
                    area DECIMAL(10,2),
                    densidadeDemografica DECIMAL(10,2)
                )
            """);

            techmentor.executeUpdate("""
                CREATE TABLE IF NOT EXISTS projecaoPopulacional (
                    idProjecaoPopulacional INT AUTO_INCREMENT PRIMARY KEY,
                    estado VARCHAR(100),
                    ano INT,
                    projecao INT
                )
            """);

            techmentor.executeUpdate("""
                CREATE TABLE IF NOT EXISTS empresa (
                    idEmpresa INT AUTO_INCREMENT PRIMARY KEY,
                    nomeEmpresa VARCHAR(100) NOT NULL,
                    nomeResponsavel VARCHAR(100),
                    cnpj VARCHAR(20) NOT NULL UNIQUE,
                    emailResponsavel VARCHAR(100) NOT NULL,
                    senha VARCHAR(100) NOT NULL
                )
            """);

            techmentor.executeUpdate("""
                CREATE TABLE IF NOT EXISTS cargo (
                    idCargo INT AUTO_INCREMENT PRIMARY KEY,
                    nomeCargo VARCHAR(100) NOT NULL,
                    salario DECIMAL(10,2) NOT NULL,
                    idEmpresa INT,
                    FOREIGN KEY (idEmpresa) REFERENCES empresa(idEmpresa)
                )
            """);

            techmentor.executeUpdate("""
                CREATE TABLE IF NOT EXISTS usuario (
                    idUsuario INT AUTO_INCREMENT PRIMARY KEY,
                    email VARCHAR(100),
                    nomeUsuario VARCHAR(100),
                    cpf VARCHAR(20),
                    senha VARCHAR(100),
                    idEmpresa INT,
                    idCargo INT,
                    FOREIGN KEY (idEmpresa) REFERENCES empresa(idEmpresa),
                    FOREIGN KEY (idCargo) REFERENCES cargo(idCargo)
                )
            """);

            // Ajustes de charset para compatibilidade UTF-8
            techmentor.executeUpdate("ALTER TABLE municipio CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            techmentor.executeUpdate("ALTER TABLE projecaoPopulacional CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");

            System.out.println("Estrutura do banco de dados criada com sucesso!");
        }
    }
}
