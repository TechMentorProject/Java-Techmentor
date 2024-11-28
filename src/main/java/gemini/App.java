package gemini;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.GenerationConfig;
import com.google.cloud.vertexai.api.HarmCategory;
import com.google.cloud.vertexai.api.SafetySetting;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseStream;
import infrastructure.database.BancoOperacoes;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class App {
    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));

        BancoOperacoes bancoOperacoes = new BancoOperacoes();

        try {
            bancoOperacoes.conectar();

            try (VertexAI vertexAi = new VertexAI("projeto-teste-gemini-441423", "us-central1"); ) {
                GenerationConfig generationConfig =
                        GenerationConfig.newBuilder()
                                .setMaxOutputTokens(750)
                                .setTemperature(0.4F)
                                .setTopP(0.95F)
                                .build();
                List<SafetySetting> safetySettings = Arrays.asList(
                        SafetySetting.newBuilder()
                                .setCategory(HarmCategory.HARM_CATEGORY_HATE_SPEECH)
                                .setThreshold(SafetySetting.HarmBlockThreshold.HARM_BLOCK_THRESHOLD_UNSPECIFIED)
                                .build(),
                        SafetySetting.newBuilder()
                                .setCategory(HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT)
                                .setThreshold(SafetySetting.HarmBlockThreshold.HARM_BLOCK_THRESHOLD_UNSPECIFIED)
                                .build(),
                        SafetySetting.newBuilder()
                                .setCategory(HarmCategory.HARM_CATEGORY_SEXUALLY_EXPLICIT)
                                .setThreshold(SafetySetting.HarmBlockThreshold.HARM_BLOCK_THRESHOLD_UNSPECIFIED)
                                .build(),
                        SafetySetting.newBuilder()
                                .setCategory(HarmCategory.HARM_CATEGORY_HARASSMENT)
                                .setThreshold(SafetySetting.HarmBlockThreshold.HARM_BLOCK_THRESHOLD_UNSPECIFIED)
                                .build()
                );
                GenerativeModel model =
                        new GenerativeModel.Builder()
                                .setModelName("gemini-1.5-flash-002")
                                .setVertexAi(vertexAi)
                                .setGenerationConfig(generationConfig)
                                .setSafetySettings(safetySettings)
                                .build();

                var text1 = "de acordo com os dados do estado de São Paulo, que possui 14.822 antenas do tipo 4G de internet, que possui 569.958 habitantes, vale a pena investir em mais antenas 5G nesse estado?";

                var content = ContentMaker.fromMultiModalData(text1);
                ResponseStream<GenerateContentResponse> responseStream = model.generateContentStream(content);

                StringBuilder fullResponse = new StringBuilder();

                responseStream.stream().forEach(response -> {
                    response.getCandidatesList().forEach(candidate -> {
                        candidate.getContent().getPartsList().forEach(part -> {
                            fullResponse.append(part.getText());
                        });
                    });
                });

                String respostaGerada = fullResponse.toString();
                System.out.println(respostaGerada);

                inserirNoBanco(bancoOperacoes.getConexao(), text1, respostaGerada);

            } catch (IOException e) {
                System.err.println("Erro ao conectar ao Vertex AI: " + e.getMessage());
            }

        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("Erro ao conectar ou operar no banco de dados: " + e.getMessage());
        } finally {
            try {
                bancoOperacoes.fecharConexao();
            } catch (SQLException e) {
                System.err.println("Erro ao fechar a conexão com o banco: " + e.getMessage());
            }
        }
    }

    private static void inserirNoBanco(Connection conexao, String prompt, String resposta) throws SQLException {

        String sql = "INSERT INTO gemini (promptEnviado, respostaGemini) VALUES (?, ?)";

        try (PreparedStatement preparedStatement = conexao.prepareStatement(sql)) {
            preparedStatement.setString(1, prompt);
            preparedStatement.setString(2, resposta);

            int rowsInserted = preparedStatement.executeUpdate();
            conexao.commit();
            if (rowsInserted > 0) {
                System.out.println("Dados inseridos no banco com sucesso!");
            }
        }
    }
}
