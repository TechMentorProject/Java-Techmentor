package domain;

public class CensoIBGE {

    private String cidade;
    private Double area;
    private Double densidadeDemografica;

    public CensoIBGE() {
    }

    public CensoIBGE(String cidade, Double area, Double densidadeDemografica) {
        this.cidade = cidade;
        this.area = area;
        this.densidadeDemografica = densidadeDemografica;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public Double getArea() {
        return area;
    }

    public void setArea(Double area) {
        this.area = area;
    }

    public Double getDensidadeDemografica() {
        return densidadeDemografica;
    }

    public void setDensidadeDemografica(Double densidadeDemografica) {
        this.densidadeDemografica = densidadeDemografica;
    }

    @Override
    public String toString() {
        return "CensoIBGE{" +
                "cidade='" + cidade + '\'' +
                ", area=" + area +
                ", densidadeDemografica=" + densidadeDemografica +
                '}';
    }
}
