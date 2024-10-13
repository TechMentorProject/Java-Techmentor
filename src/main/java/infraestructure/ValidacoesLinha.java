package infraestructure;

import java.util.List;

public class ValidacoesLinha {
    public boolean algumCampoInvalido(Object... campos) {
        for (Object campo : campos) {
            if (campo == null) {
                return true; // Se o campo for null, é inválido
            }
            if (campo instanceof String && ((String) campo).isEmpty()) {
                return true; // Se for uma String vazia, é inválido
            }
        }
        return false;
    }

    public String[] processarLinha(List<Object> linha) {
        String linhaConvertida = formatarLinha (linha);
        return linhaConvertida.split(";");
    }

    public String formatarLinha(List<Object> row) {
        StringBuilder linha = new StringBuilder();
        for (Object celula : row) {
            if (!linha.isEmpty()) {
                linha.append(";");
            }
            linha.append(celula != null ? celula.toString() : "");
        }
        return linha.toString();
    }

    public String buscarValorValido(List<Object> row) {
        StringBuilder linha = new StringBuilder();
        for (Object celula : row) {
            if (!linha.isEmpty()) {
                linha.append(";");  // Adicionar separador
            }
            linha.append(celula != null ? celula.toString() : "");
        }
        return linha.toString();
    }

    public String buscarValorValido(String[] valores, int index) {
        if (index < valores.length) {
            String valor = valores[index];
            if (!valor.isEmpty()) {
                return valor;
            }
        }
        return null;
    }
}
