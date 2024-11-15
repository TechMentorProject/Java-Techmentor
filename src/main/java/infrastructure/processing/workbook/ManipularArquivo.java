package infrastructure.processing.workbook;

import infrastructure.processing.workbook.ManipularPlanilha;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ManipularArquivo {

    public List<List<Object>> lerPlanilha(String caminhoArquivo, Boolean isProjecao) throws Exception {
        List<List<Object>> linhasPlanilha = new ArrayList<>();
        File tempFile = null;

        try (InputStream arquivoOriginal = new FileInputStream(caminhoArquivo)) {
            tempFile = File.createTempFile("arquivo_temp", ".xlsx");
            try (OutputStream tempOut = new FileOutputStream(tempFile)) {
                arquivoOriginal.transferTo(tempOut);
            }

            try (OPCPackage pacoteExcel = OPCPackage.open(tempFile)) {
                XSSFReader leitorExcel = new XSSFReader(pacoteExcel);
                XMLReader leitorXML = org.apache.poi.util.XMLHelper.newXMLReader();

                // Configuração de tabela de strings com encoding correto
                ReadOnlySharedStringsTable stringsTable = new ReadOnlySharedStringsTable(pacoteExcel);

                // Manipulador da planilha customizado
                XSSFSheetXMLHandler manipularPlanilha = new XSSFSheetXMLHandler(
                        leitorExcel.getStylesTable(),
                        stringsTable,
                        new ManipularPlanilhaCustomizada(linhasPlanilha, isProjecao),
                        false
                );

                try (InputStream fluxoFolha = leitorExcel.getSheetsData().next();
                     Reader readerUTF8 = new InputStreamReader(fluxoFolha, StandardCharsets.UTF_8)) {
                    leitorXML.setContentHandler(manipularPlanilha);
                    leitorXML.parse(new InputSource(readerUTF8));
                }
            }
        } catch (InvalidFormatException | SAXException | IOException e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
        return linhasPlanilha;
    }

    // Classe manipuladora que reprocessa cada string
    private static class ManipularPlanilhaCustomizada extends ManipularPlanilha {

        public ManipularPlanilhaCustomizada(List<List<Object>> linhasPlanilha, Boolean isProjecao) {
            super(linhasPlanilha, isProjecao);
        }


        public void cell(String cellReference, String formattedValue) {
            // Reinterpreta a string com UTF-8, tentando corrigir caracteres estranhos
            if (formattedValue != null) {
                formattedValue = new String(formattedValue.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
            }
            super.cell(cellReference, formattedValue, null);
        }
    }
}
