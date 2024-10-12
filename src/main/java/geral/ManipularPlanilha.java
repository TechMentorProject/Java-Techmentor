package geral;

import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.usermodel.XSSFComment;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ManipularPlanilha implements XSSFSheetXMLHandler.SheetContentsHandler {
    private List<List<Object>> dadosPlanilha;
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

        while (linhaAtual.size() < indiceColuna) {
            linhaAtual.add(null);
        }
        linhaAtual.add(valorFormatado);
    }

    // Finaliza a leitura da linha atual, adicionando-a aos dados da planilha se não estiver vazia
    @Override
    public void endRow(int numeroLinha) {
        if (!linhaAtual.isEmpty()) {  // Adiciona a linha ao conjunto de dados, se não estiver vazia
            dadosPlanilha.add(linhaAtual);
        }
    }
}
