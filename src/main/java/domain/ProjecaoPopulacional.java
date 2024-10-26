package domain;

public class ProjecaoPopulacional {

    private String estado;
    private int ano;
    private long projecao;

    public ProjecaoPopulacional() {
    }

    public ProjecaoPopulacional(String estado, int ano, long projecao) {
        this.estado = estado;
        this.ano = ano;
        this.projecao = projecao;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public int getAno() {
        return ano;
    }

    public void setAno(int ano) {
        this.ano = ano;
    }

    public long getProjecao() {
        return projecao;
    }

    public void setProjecao(long projecao) {
        this.projecao = projecao;
    }

    @Override
    public String toString() {
        return "ProjecaoPopulacional{" +
                "estado='" + estado + '\'' +
                ", ano=" + ano +
                ", projecao=" + projecao +
                '}';
    }
}
