package config;

public enum Configuracoes {

    // Enum constants
    IP_BANCO("localhost"),
    PORTA_BANCO("3306"),
    DATABASE("techmentor"),
    USUARIO("root"),
    SENHA("root"),
    NOME_BUCKET_S3("techmentor-bucket"),
    CAMINHO_DIRETORIO_RAIZ("/app/base-dados"),
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
        return valor;
    }
}

