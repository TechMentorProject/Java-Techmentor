package geral;

import estacoes_smp.BancoDeDados;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.usermodel.XSSFComment;

import java.io.UnsupportedEncodingException;
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
        BancoDeDados banco = new BancoDeDados();
        int indiceColuna = new CellReference(referenciaCelula).getCol();
        String _valorFormatado = "";
        // Verifica e converte o valor lido da célula para UTF-8
        if (valorFormatado != null) {
            // Corrige possíveis problemas de encoding com substituição de caracteres inválidos
            _valorFormatado = corrigirEncoding(valorFormatado);
            if(_valorFormatado.contains("�")) {
                _valorFormatado = valorFormatado;
            }
        }
        // Preenche a lista até a coluna desejada
        while (linhaAtual.size() < indiceColuna) {
            linhaAtual.add(null);
        }
        linhaAtual.add(_valorFormatado);
    }

    public String corrigirEncoding(String valor) {
        try {
            // Primeiro, assume que os dados estão em ISO-8859-1 e reencoda para UTF-8
            byte[] bytes = valor.getBytes("ISO-8859-1");
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Caso o encoding não seja suportado, retorne o valor original
            e.printStackTrace();
            return valor;
        }
    }


    // Finaliza a leitura da linha atual, adicionando-a aos dados da planilha se não estiver vazia
    @Override
    public void endRow(int numeroLinha) {
        if (!linhaAtual.isEmpty()) {  // Adiciona a linha ao conjunto de dados, se não estiver vazia
            dadosPlanilha.add(linhaAtual);
        }
    }
}
