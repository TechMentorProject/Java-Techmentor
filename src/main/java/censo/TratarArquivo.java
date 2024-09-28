package censo;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class TratarArquivo {

    public List<List<Object>> LerArquivo(String caminhoArquivo) throws Exception {
        List<List<Object>> dadosExcel = new ArrayList<>();

        // Usando BufferedInputStream para melhorar a performance de leitura de arquivo
        try (OPCPackage opcPackage = OPCPackage.open(new BufferedInputStream(new FileInputStream(caminhoArquivo)))) {

            XSSFReader reader = new XSSFReader(opcPackage);
            XMLReader xmlReader = org.apache.poi.util.XMLHelper.newXMLReader();
            XSSFSheetXMLHandler sheetHandler = new XSSFSheetXMLHandler(
                    reader.getStylesTable(),
                    new ReadOnlySharedStringsTable(opcPackage),
                    new estacoes_smp.TratarArquivo.SheetContentsHandlerImpl(dadosExcel), false);

            // Lê a planilha e processa com stream
            try (InputStream sheetStream = reader.getSheetsData().next()) {
                xmlReader.setContentHandler(sheetHandler);
                xmlReader.parse(new InputSource(sheetStream));
            }
        }
        return dadosExcel;
    }

    private static class SheetContentsHandlerImpl implements XSSFSheetXMLHandler.SheetContentsHandler {
        private final List<List<Object>> dadosExcel;
        private List<Object> currentRow;

        public SheetContentsHandlerImpl(List<List<Object>> dadosExcel) {
            this.dadosExcel = dadosExcel;
        }

        @Override
        public void startRow(int rowNum) {
            currentRow = new ArrayList<>();  // Inicia uma nova linha
        }

        @Override
        public void endRow(int rowNum) {
            if (!currentRow.isEmpty()) dadosExcel.add(currentRow);  // Evitar cópia desnecessária da lista
        }

        @Override
        public void cell(String cellReference, String formattedValue, XSSFComment comment) {
            int currentCol = new CellReference(cellReference).getCol();

            // Preenche as células vazias, se houver
            while (currentRow.size() < currentCol) currentRow.add(null);
            currentRow.add(formattedValue);  // Adiciona a célula atual
        }
    }
}

