package infrastructure.processing.workbook;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ManipularArquivo {

    public List<List<Object>> lerPlanilha(String caminhoArquivo) throws Exception {
        List<List<Object>> linhasPlanilha = new ArrayList<>();

        try (OPCPackage pacoteExcel = OPCPackage.open(new BufferedInputStream(new FileInputStream(caminhoArquivo)))) {

            XSSFReader leitorExcel = new XSSFReader(pacoteExcel);
            XMLReader leitorXML = org.apache.poi.util.XMLHelper.newXMLReader();
            XSSFSheetXMLHandler manipularPlanilha = new XSSFSheetXMLHandler(leitorExcel.getStylesTable(), new ReadOnlySharedStringsTable(pacoteExcel), new ManipularPlanilha(linhasPlanilha), false);

            try (InputStream fluxoFolha = leitorExcel.getSheetsData().next()) {
                leitorXML.setContentHandler(manipularPlanilha);
                leitorXML.parse(new InputSource(fluxoFolha));
            }
        }
        return linhasPlanilha;
    }
}