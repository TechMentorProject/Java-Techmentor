package gemini;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.GenerationConfig;
import com.google.cloud.vertexai.api.HarmCategory;
import com.google.cloud.vertexai.api.SafetySetting;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class App {
    public static void main(String[] args) throws IOException {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));

        try (VertexAI vertexAi = new VertexAI("projeto-teste-gemini-441423", "us-central1")) {
            GenerationConfig generationConfig =
                    GenerationConfig.newBuilder()
                            .setMaxOutputTokens(8192)
                            .setTemperature(0.5F)
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

            var text1 = "de acordo com os dados do estado de São Paulo, que possui 14.822 antenas do tipo 4G de internet, que possui 569.958 habitantes, vale a pena investir em mais antenas 5G nesse estado?"; //primeiro Prompt criado para testes, os dados daqui eu tirei da cabeça, não são nem um pouco reais, logo mais eu vou mudar e adicionar maia para que façam sentido no nosso projeto, colocando variáveis e aplicando as outras mudanças que foram citadas sobre os prompts nas reuniões :)

            var content = ContentMaker.fromMultiModalData(text1);
            ResponseStream<GenerateContentResponse> responseStream = model.generateContentStream(content);

            StringBuilder fullResponse = new StringBuilder();


            responseStream.stream().forEach(response -> {
                response.getCandidatesList().forEach(candidate -> {
                    candidate.getContent().getPartsList().forEach(part -> {
                        String decodedText = new String(part.getText().getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                        fullResponse.append(decodedText);
                    });
                });
            });

            System.out.println(fullResponse.toString());
        }
    }
}
