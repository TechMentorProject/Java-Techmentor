package domain;

public class EstacoesSMP {

    private String cidade;
    private String operadora;
    private long latitude;
    private long longitude;
    private String codigoIBGE;
    private String tecnologia;

    public EstacoesSMP() {
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public String getOperadora() {
        return operadora;
    }

    public void setOperadora(String operadora) {
        this.operadora = operadora;
    }

    public long getLatitude() {
        return latitude;
    }

    public void setLatitude(long latitude) {
        this.latitude = latitude;
    }

    public long getLongitude() {
        return longitude;
    }

    public void setLongitude(long longitude) {
        this.longitude = longitude;
    }

    public String getCodigoIBGE() {
        return codigoIBGE;
    }

    public void setCodigoIBGE(String codigoIBGE) {
        this.codigoIBGE = codigoIBGE;
    }

    public String getTecnologia() {
        return tecnologia;
    }

    public void setTecnologia(String tecnologia) {
        this.tecnologia = tecnologia;
    }

    @Override
    public String toString() {
        return "EstacoesSMP{" +
                "cidade='" + cidade + '\'' +
                ", operadora='" + operadora + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", codigoIBGE='" + codigoIBGE + '\'' +
                ", tecnologia='" + tecnologia + '\'' +
                '}';
    }
}


