package domain;

public class CensoIBGE {

    private int idCensoIBGE;
    private String cidade;
    private Double crescimentoPopulacional;
    private Double densidadeDemografica;

    public CensoIBGE() {
    }

    public CensoIBGE(int idCensoIBGE, String cidade, Double crescimentoPopulacional, Double densidadeDemografica) {
        this.idCensoIBGE = idCensoIBGE;
        this.cidade = cidade;
        this.crescimentoPopulacional = crescimentoPopulacional;
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

    public Double getCrescimentoPopulacional() {
        return crescimentoPopulacional;
    }

    public void setCrescimentoPopulacional(Double crescimentoPopulacional) {
        this.crescimentoPopulacional = crescimentoPopulacional;
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
                ", crescimentoPopulacional=" + crescimentoPopulacional +
                ", densidadeDemografica=" + densidadeDemografica +
                '}';
    }

}
