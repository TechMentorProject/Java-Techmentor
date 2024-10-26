package domain;

public class CensoIBGE {

    private int idCensoIBGE;
    private String cidade;
    private Double area;
    private Double densidadeDemografica;

    public CensoIBGE() {
    }

    public CensoIBGE(int idCensoIBGE, String cidade, Double crescimentoPopulacional, Double densidadeDemografica) {
        this.idCensoIBGE = idCensoIBGE;
        this.cidade = cidade;
        this.area = crescimentoPopulacional;
        this.densidadeDemografica = densidadeDemografica;
    }

    public int getIdCensoIBGE() {
        return idCensoIBGE;
    }

    public void setIdCensoIBGE(int idCensoIBGE) {
        this.idCensoIBGE = idCensoIBGE;
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

    // Método toString
    @Override
    public String toString() {
        return "CensoIBGE{" +
                "idCensoIBGE=" + idCensoIBGE +
                ", cidade='" + cidade + '\'' +
                ", crescimentoPopulacional=" + area +
                ", densidadeDemografica=" + densidadeDemografica +
                '}';
    }

}
