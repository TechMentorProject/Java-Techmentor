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
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class TratarArquivo {

        // Método para buscar os arquivos no diretório
        public List<String> buscarArquivos(String diretorioBase) {
            File pasta = new File(diretorioBase);
            List<String> arquivos = new ArrayList<>();

            // Filtrar arquivos que contenham o nome 'Crescimento Populacional' e terminem com '.xlsx'
            File[] arquivosListados = pasta.listFiles((dir, nome) -> nome.contains("Crescimento Populacional") && nome.endsWith(".xlsx"));

            if (arquivosListados != null) {
                for (File arquivo : arquivosListados) {
                    arquivos.add(arquivo.getAbsolutePath());
                }
            }
            return arquivos;
        }

        // Método para processar os arquivos e inserir dados no banco de dados
        public void processarArquivosEDados(String diretorioBase, BancoDeDados bancoDeDados) throws Exception {
            // Buscar todos os arquivos no diretório base
            List<String> arquivos = buscarArquivos(diretorioBase);

            if (arquivos.isEmpty()) {
                System.out.println("Nenhum arquivo encontrado no diretório: " + diretorioBase);
                return;
            }

            // Conectar ao banco de dados
            bancoDeDados.conectar();

            // Processar cada arquivo encontrado
            for (String caminhoArquivo : arquivos) {
                System.out.println("Lendo arquivo: " + caminhoArquivo);

                try {
                    // Ler o arquivo Excel e obter os dados
                    List<List<Object>> dadosExcel = LerArquivo(caminhoArquivo);

                    // Inserir os dados no banco de dados
                    bancoDeDados.inserirDados(dadosExcel);

                } catch (Exception e) {
                    System.err.println("Erro ao processar o arquivo: " + caminhoArquivo);
                    e.printStackTrace();
                }
            }
        }

    // Método para ler todas as planilhas de um arquivo Excel
    public List<List<Object>> LerArquivo(String caminhoArquivo) throws Exception {
        List<List<Object>> dadosExcel = new ArrayList<>();

        // Abrir o arquivo Excel usando OPCPackage
        try (OPCPackage opcPackage = OPCPackage.open(new BufferedInputStream(new FileInputStream(caminhoArquivo)))) {
            XSSFReader reader = new XSSFReader(opcPackage);
            XMLReader xmlReader = org.apache.poi.util.XMLHelper.newXMLReader();
            ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(opcPackage);
            XSSFSheetXMLHandler.SheetContentsHandler handler = new SheetContentsHandlerImpl(dadosExcel);

            // Iterar por todas as planilhas
            XSSFReader.SheetIterator iter = (XSSFReader.SheetIterator) reader.getSheetsData();
            while (iter.hasNext()) {
                try (InputStream sheetStream = iter.next()) {
                    // Definir o conteúdo do handler e processar a planilha
                    xmlReader.setContentHandler(new XSSFSheetXMLHandler(reader.getStylesTable(), strings, handler, false));
                    xmlReader.parse(new InputSource(sheetStream));
                }
            }
        }
        return dadosExcel;
    }

    // Implementação do handler para processar células de cada planilha
    public static class SheetContentsHandlerImpl implements XSSFSheetXMLHandler.SheetContentsHandler {
        private final List<List<Object>> dadosExcel;
        private List<Object> currentRow;

        public SheetContentsHandlerImpl(List<List<Object>> dadosExcel) {
            this.dadosExcel = dadosExcel;
        }

        @Override
        public void startRow(int rowNum) {
            currentRow = new ArrayList<>();
        }

        @Override
        public void endRow(int rowNum) {
            if (!currentRow.isEmpty()) {
                dadosExcel.add(currentRow);
            }
        }

        @Override
        public void cell(String cellReference, String formattedValue, XSSFComment comment) {
            int currentCol = new CellReference(cellReference).getCol();
            while (currentRow.size() < currentCol) {
                currentRow.add(null);
            }
            currentRow.add(formattedValue);
        }
    }
}
