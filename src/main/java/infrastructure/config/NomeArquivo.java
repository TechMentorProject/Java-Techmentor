package infrastructure.config;

public enum NomeArquivo {
    ESTACOES_SMP("Estacoes_SMP.xlsx"),
    CENSOIBGE("Território -"),
    MUNICIPIO("Meu_Municipio_Cobertura.xlsx"),
    PROJECAO("projecoes_2024_tab1_idade_simples.xlsx");

    private final String nome;

    // Construtor do enum
    NomeArquivo(String nome) {
        this.nome = nome;
    }

    // Método para obter o nome do arquivo
    public String getNome() {
        return nome;
    }
}

