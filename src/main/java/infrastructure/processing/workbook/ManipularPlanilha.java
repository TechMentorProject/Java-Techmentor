package infrastructure.processing.workbook;

import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.usermodel.XSSFComment;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ManipularPlanilha implements XSSFSheetXMLHandler.SheetContentsHandler {
    private final List<List<Object>> dadosPlanilha;
    private List<Object> linhaAtual;
    private Boolean isProjecao;

    public ManipularPlanilha(List<List<Object>> dadosPlanilha, Boolean isProjecao) {
        this.dadosPlanilha = dadosPlanilha;
        this.isProjecao = isProjecao;
    }

    @Override
    public void startRow(int numeroLinha) {
        linhaAtual = new ArrayList<>();  // Inicializa uma nova linha
    }

    @Override
    public void cell(String referenciaCelula, String valorFormatado, XSSFComment comentario) {
        int indiceColuna = new CellReference(referenciaCelula).getCol();
        String _valorFormatado = "";

        if (valorFormatado != null) {
            _valorFormatado = corrigirEncoding(valorFormatado, isProjecao);
        }

        while (linhaAtual.size() < indiceColuna) {
            linhaAtual.add(null);
        }
        linhaAtual.add(_valorFormatado);
    }

    public String corrigirEncoding(String valor, Boolean isProjecao) {
        try {
            if(!isProjecao) {
                byte[] bytes = valor.getBytes(StandardCharsets.ISO_8859_1);
                valor = new String(bytes, StandardCharsets.UTF_8);
            }

            return substituirCaracteresCorrompidos(valor);
        } catch (Exception e) {
            return valor;
        }
    }

    private String substituirCaracteresCorrompidos(String valor) {
        return valor.replace("Dias d'�?vila", "Dias d'Ávila")
                .replace("�?gua Comprida", "Água Comprida")
                .replace("�?gua Boa", "Água Boa")
                .replace("�?rico Cardoso", "Érico Cardoso")
                .replace("�?gua Branca", "Água Branca")
                .replace("�?guas Formosas", "Águas Formosas")
                .replace("�?guas Vermelhas", "Águas Vermelhas")
                .replace("Olhos-d'�?gua", "Olhos-d'Água")
                .replace("Pingo-d'�?gua", "Pingo-d'Água")
                .replace("Mãe d'�?gua", "Mãe d'Água")
                .replace("Cachoeira dos �?ndios", "Cachoeira dos Índios")
                .replace("Olho d'�?gua", "Olho d'Água")
                .replace("�?gua Doce", "Água Doce")
                .replace("Palmeira dos �?ndios", "Palmeira dos Índios")
                .replace("�?gua Fria - BA", "Água Fria - BA")
                .replace("�?guia Branca - ES", "Águia Branca - ES")
                .replace("�?bidos - PA", "Óbidos - PA")
                .replace("�?gua Azul do Norte - PA", "Água Azul do Norte - PA")
                .replace("Olho D'�?gua do Piauí - PI", "Olho D'Água do Piauí - PI")
                .replace("Olho-d'�?gua do Borges - RN", "Olho-d'Água do Borges - RN")
                .replace("�?gua Nova - RN", "Água Nova - RN")
                .replace("�?guas Belas - PE", "Águas Belas - PE")
                .replace("�?gua Preta - PE", "Água Preta - PE")
                .replace("Paraíso das �?guas - MS", "Paraíso das Águas - MS")
                .replace("�?gua Clara - MS", "Água Clara - MS")
                .replace("Santo �?ngelo - RS", "Santo Ângelo - RS")
                .replace("�?urea - RS", "Âurea - RS");
    }

    @Override
    public void endRow(int numeroLinha) {
        if (!linhaAtual.isEmpty()) {
            dadosPlanilha.add(linhaAtual);
        }
    }
}
