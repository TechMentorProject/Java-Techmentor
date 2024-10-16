package domain;

public class Municipio {

    private int idMunicipio;
    private String ano;
    private String cidade;
    private String operadora;
    private int domiciliosCobertosPorcentagem;
    private int areaCobertaPorcentagem;
    private String tecnologia;

    public Municipio() {
    }

    public Municipio(int idMunicipio, String ano, String cidade, String operadora, int domiciliosCobertosPorcentagem, int areaCobertaPorcentagem, String tecnologia) {
        this.idMunicipio = idMunicipio;
        this.ano = ano;
        this.cidade = cidade;
        this.operadora = operadora;
        this.domiciliosCobertosPorcentagem = domiciliosCobertosPorcentagem;
        this.areaCobertaPorcentagem = areaCobertaPorcentagem;
        this.tecnologia = tecnologia;
    }

    public int getIdMunicipio() {
        return idMunicipio;
    }

    public void setIdMunicipio(int idMunicipio) {
        this.idMunicipio = idMunicipio;
    }

    public String getAno() {
        return ano;
    }

    public void setAno(String ano) {
        this.ano = ano;
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

    public int getDomiciliosCobertosPorcentagem() {
        return domiciliosCobertosPorcentagem;
    }

    public void setDomiciliosCobertosPorcentagem(int domiciliosCobertosPorcentagem) {
        this.domiciliosCobertosPorcentagem = domiciliosCobertosPorcentagem;
    }

    public int getAreaCobertaPorcentagem() {
        return areaCobertaPorcentagem;
    }

    public void setAreaCobertaPorcentagem(int areaCobertaPorcentagem) {
        this.areaCobertaPorcentagem = areaCobertaPorcentagem;
    }

    public String getTecnologia() {
        return tecnologia;
    }

    public void setTecnologia(String tecnologia) {
        this.tecnologia = tecnologia;
    }

    @Override
    public String toString() {
        return "Municipio{" +
                "idMunicipio=" + idMunicipio +
                ", ano='" + ano + '\'' +
                ", cidade='" + cidade + '\'' +
                ", operadora='" + operadora + '\'' +
                ", domiciliosCobertosPorcentagem=" + domiciliosCobertosPorcentagem +
                ", areaCobertaPorcentagem=" + areaCobertaPorcentagem +
                ", tecnologia='" + tecnologia + '\'' +
                '}';
    }
}


