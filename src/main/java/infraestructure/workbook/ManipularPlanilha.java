package infraestructure.workbook;

import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.usermodel.XSSFComment;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ManipularPlanilha implements XSSFSheetXMLHandler.SheetContentsHandler {
    private final List<List<Object>> dadosPlanilha;
    private List<Object> linhaAtual;

    public ManipularPlanilha(List<List<Object>> dadosPlanilha) {
        this.dadosPlanilha = dadosPlanilha;
    }

    // Inicia a leitura de uma nova linha, criando a lista para armazenar os dados das células
    @Override
    public void startRow(int numeroLinha) {
        linhaAtual = new ArrayList<>();  // Inicializa uma nova linha
    }

    // Processa o conteúdo de cada célula, convertendo o valor para UTF-8 e preenchendo as células vazias se necessário
    @Override
    public void cell(String referenciaCelula, String valorFormatado, XSSFComment comentario) {
        int indiceColuna = new CellReference(referenciaCelula).getCol();
        String _valorFormatado = "";

        if (valorFormatado != null) {
            _valorFormatado = corrigirEncoding(valorFormatado);

            if(_valorFormatado.contains("�")) {
                _valorFormatado = valorFormatado;
            }
        }
        while (linhaAtual.size() < indiceColuna) {
            linhaAtual.add(null);
        }
        linhaAtual.add(_valorFormatado);
    }

    public String corrigirEncoding(String valor) {
        // Primeiro, assume que os dados estão em ISO-8859-1 e reencoda para UTF-8
        byte[] bytes = valor.getBytes(StandardCharsets.ISO_8859_1);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    // Finaliza a leitura da linha atual, adicionando-a aos dados da planilha se não estiver vazia
    @Override
    public void endRow(int numeroLinha) {
        if (!linhaAtual.isEmpty()) {
            dadosPlanilha.add(linhaAtual);
        }
    }
}
