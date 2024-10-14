package domain;

public class ProjecaoPopulacional {

    private int idProjecaoPopulacional;
    private String estado;
    private int idade;
    private int projecao2024;
    private int projecao2025;
    private int projecao2026;
    private int projecao2027;
    private int projecao2028;


    public ProjecaoPopulacional() {
    }

    public ProjecaoPopulacional(int idProjecaoPopulacional, String estado, int idade, int projecao2024, int projecao2025, int projecao2026, int projecao2027, int projecao2028) {
        this.idProjecaoPopulacional = idProjecaoPopulacional;
        this.estado = estado;
        this.idade = idade;
        this.projecao2024 = projecao2024;
        this.projecao2025 = projecao2025;
        this.projecao2026 = projecao2026;
        this.projecao2027 = projecao2027;
        this.projecao2028 = projecao2028;
    }

    public int getIdProjecaoPopulacional() {
        return idProjecaoPopulacional;
    }

    public void setIdProjecaoPopulacional(int idProjecaoPopulacional) {
        this.idProjecaoPopulacional = idProjecaoPopulacional;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public int getIdade() {
        return idade;
    }

    public void setIdade(int idade) {
        this.idade = idade;
    }

    public int getProjecao2024() {
        return projecao2024;
    }

    public void setProjecao2024(int projecao2024) {
        this.projecao2024 = projecao2024;
    }

    public int getProjecao2025() {
        return projecao2025;
    }

    public void setProjecao2025(int projecao2025) {
        this.projecao2025 = projecao2025;
    }

    public int getProjecao2026() {
        return projecao2026;
    }

    public void setProjecao2026(int projecao2026) {
        this.projecao2026 = projecao2026;
    }

    public int getProjecao2027() {
        return projecao2027;
    }

    public void setProjecao2027(int projecao2027) {
        this.projecao2027 = projecao2027;
    }

    public int getProjecao2028() {
        return projecao2028;
    }

    public void setProjecao2028(int projecao2028) {
        this.projecao2028 = projecao2028;
    }

    // Método toString
    @Override
    public String toString() {
        return "ProjecaoPopulacional{" +
                "idProjecaoPopulacional=" + idProjecaoPopulacional +
                ", estado='" + estado + '\'' +
                ", idade=" + idade +
                ", projecao2024=" + projecao2024 +
                ", projecao2025=" + projecao2025 +
                ", projecao2026=" + projecao2026 +
                ", projecao2027=" + projecao2027 +
                ", projecao2028=" + projecao2028 +
                '}';
    }

}
