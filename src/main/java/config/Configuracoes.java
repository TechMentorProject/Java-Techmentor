package config;

public enum Configuracoes {

    // Enum constants
    IP_BANCO("localhost"),
    PORTA_BANCO("3306"),
    DATABASE("techmentor"),
    USUARIO("root"),
    SENHA("Kodol@te2403"),
    NOME_BUCKET_S3("techmentor-bucket"),
    CAMINHO_DIRETORIO_RAIZ("C:\\Users\\v8\\Documents\\Relatórios e Atividades SPTech\\PI\\Sem. Novo\\Projeto TechMentor\\base-de-dados\\base-de-dados"),
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

