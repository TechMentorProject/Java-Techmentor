package config;

public enum Configuracoes {

    // Enum constants
    IP_BANCO("localhost"),
    PORTA_BANCO("3306"),
    DATABASE("techmentor"),
    USUARIO("root"),
    SENHA("Na.161105"),
    NOME_BUCKET_S3("techmentor"),
    CAMINHO_DIRETORIO_RAIZ("C:\\Users\\naomi\\OneDrive\\Área de Trabalho\\base-de-dados"),
    DIRETORIO_LOGS("app/logs/LogsTechMentor"),
    AMBIENTE("DEV");

    // Propriedade para armazenar o valor
    private final String valor;

    // Construtor do enum
    Configuracoes(String valor) {
        this.valor = valor;
    }

    // Método para obter o valor
    public String getValor() {
        return System.getenv(name()) != null ? System.getenv(name()) : valor;
    }
}

