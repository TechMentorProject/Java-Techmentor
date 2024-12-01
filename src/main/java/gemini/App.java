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
import java.sql.*;
import java.util.ArrayList;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class App {
    public static void main(String[] args) {
        // Configura a saída padrão para mostrar caracteres especiais
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));

        // Cria uma instância da classe BancoOperacoes
        BancoOperacoes bancoOperacoes = new BancoOperacoes();

        try {
            // Tenta conectar ao banco de dados
            bancoOperacoes.conectar();

            // Gera prompts baseados nas informações do banco de dados
            List<String> prompts = gerarPrompts(bancoOperacoes.getConexao());

            // Tenta conectar ao Vertex AI
            try (VertexAI vertexAi = new VertexAI("projeto-teste-gemini-441423", "us-central1"); ) {
                // Configurações de geração de conteúdo
                GenerationConfig generationConfig =
                        GenerationConfig.newBuilder()
                                .setMaxOutputTokens(100)    // Número máximo de tokens gerados
                                .setTemperature(0.7F)       // Criatividade da geração
                                .setTopP(0.9F)              // Amostragem das respostas
                                .build();

                // Configurações de segurança
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

                // Cria um modelo gerativo
                GenerativeModel model =
                        new GenerativeModel.Builder()
                                .setModelName("gemini-1.5-flash-002")
                                .setVertexAi(vertexAi)
                                .setGenerationConfig(generationConfig)
                                .setSafetySettings(safetySettings)
                                .build();

                // Gera respostas para cada prompt
                for (String prompt : prompts) {
                    System.out.println("Gerando resposta para o prompt: " + prompt);

                    // Cria o conteúdo a ser gerado
                    var content = ContentMaker.fromMultiModalData(prompt);

                    // Gera a resposta
                    ResponseStream<GenerateContentResponse> responseStream = model.generateContentStream(content);

                    // Monta a resposta completa
                    StringBuilder fullResponse = new StringBuilder();
                    responseStream.stream().forEach(response -> {
                        response.getCandidatesList().forEach(candidate -> {
                            candidate.getContent().getPartsList().forEach(part -> {
                                fullResponse.append(part.getText());
                            });
                        });
                    });

                    // Extrai a resposta completa
                    String respostaCompleta = fullResponse.toString();

                    // Gera um prompt que pede para o Gemini resumir a resposta
                    String resumoPrompt = "Resuma o texto a seguir em no máximo 500 caracteres, mantendo os valores chave de informações: " + respostaCompleta;

                    // Cria o conteúdo a ser gerado para o resumo
                    var resumoContent = ContentMaker.fromMultiModalData(resumoPrompt);

                    // Gera o resumo
                    ResponseStream<GenerateContentResponse> resumoStream = model.generateContentStream(resumoContent);

                    // Monta o resumo final
                    StringBuilder resumoFinal = new StringBuilder();
                    resumoStream.stream().forEach(resumo -> {
                        resumo.getCandidatesList().forEach(candidate -> {
                            candidate.getContent().getPartsList().forEach(part -> {
                                resumoFinal.append(part.getText());
                            });
                        });
                    });

                    // Extrai o resumo final
                    String resumoResposta = resumoFinal.toString();

                    // Limita o resumo final a 500 caracteres
                    if (resumoResposta.length() > 500) {
                        resumoResposta = resumoResposta.substring(0, 497) + "...";
                    }

                    // Exibe o resumo da resposta
                    System.out.println("Resumo da resposta: " + resumoResposta);

                    // Insere a resposta resumida no banco de dados
                    inserirNoBanco(bancoOperacoes.getConexao(), resumoResposta);
                }

            } catch (IOException e) {
                // Exibe uma mensagem de erro caso não seja possível conectar ao Vertex AI
                System.err.println("Erro ao conectar ao Vertex AI: " + e.getMessage());
            }

        } catch (ClassNotFoundException | SQLException e) {
            // Exibe uma mensagem de erro caso não seja possível conectar ao banco de dados
            System.err.println("Erro ao conectar ou operar no banco de dados: " + e.getMessage());
        } finally {
            try {
                // Fecha a conexão com o banco de dados
                bancoOperacoes.fecharConexao();
            } catch (SQLException e) {
                // Exibe uma mensagem de erro caso não seja possível fechar a conexão com o banco de dados
                System.err.println("Erro ao fechar a conexão com o banco: " + e.getMessage());
            }
        }
    }

    // Metodo para gerar prompts baseados nas informações do banco de dados
    private static List<String> gerarPrompts(Connection conexao) throws SQLException {
        List<String> prompts = new ArrayList<>();

        try (
                Statement stmtEstado = conexao.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                Statement stmtCidade = conexao.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                Statement stmtBaseMunicipio = conexao.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                Statement stmtBaseProjecao = conexao.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                Statement stmtEstacoesSMP = conexao.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                ResultSet rsEstado = stmtEstado.executeQuery("SELECT * FROM estado");
                ResultSet rsCidade = stmtCidade.executeQuery("SELECT * FROM cidade");
                ResultSet rsBaseMunicipio = stmtBaseMunicipio.executeQuery("SELECT * FROM baseMunicipio");
                ResultSet rsBaseProjecao = stmtBaseProjecao.executeQuery("SELECT * FROM baseProjecaoPopulacional");
                ResultSet rsEstacoesSMP = stmtEstacoesSMP.executeQuery("SELECT * FROM baseEstacoesSMP");
        ) {
            // Prompts gerados
            while (rsEstado.next() && rsCidade.next() && rsBaseMunicipio.next() && rsBaseProjecao.next() && rsEstacoesSMP.next()) {

                // Prompt 1: Menor conectividade por região
                String regiao = rsEstado.getString("regiao");
                String estado = rsEstado.getString("nomeEstado");
                String cidade = rsCidade.getString("nomeCidade");
                Double conectividade = rsBaseMunicipio.getDouble("domiciliosCobertosPercentual");
                String prompt1 = String.format(
                        "A região %s do estado %s tem baixa conectividade na cidade %s com apenas %.2f%% de cobertura.",
                        regiao, estado, cidade, conectividade
                );
                prompts.add(prompt1);

                // Prompt 2: Crescimento populacional e conectividade
                Integer crescimentoPopulacional = rsBaseProjecao.getInt("projecao");
                String prompt2 = String.format(
                        "A cidade %s no estado %s está projetada para crescer em %d habitantes e possui conectividade de %.2f%%.",
                        cidade, estado, crescimentoPopulacional, conectividade
                );
                prompts.add(prompt2);

                // Prompt 3: Comparação de tecnologias (4G x 5G)
                String tecnologia = rsEstacoesSMP.getString("tecnologia");
                String operadora = rsEstacoesSMP.getString("operadora");
                String prompt3 = String.format(
                        "A cidade %s no estado %s possui infraestrutura de %s fornecida pela operadora %s.",
                        cidade, estado, tecnologia, operadora
                );
                prompts.add(prompt3);

                // Prompt 4: Menor número de operadoras
                Integer qtdOperadoras = rsEstacoesSMP.getInt("idEstacoesSMP");
                String prompt4 = String.format(
                        "A cidade %s no estado %s apresenta o menor número de operadoras atuantes, com apenas %d estações.",
                        cidade, estado, qtdOperadoras
                );
                prompts.add(prompt4);

                // Prompt 5: Decisão de investimento em estados com baixa conectividade
                if (conectividade < 30) {
                    String prompt5 = String.format(
                            "O estado %s, cidade %s, com %.2f%% de conectividade, deve ser considerado para investimentos futuros.",
                            estado, cidade, conectividade
                    );
                    prompts.add(prompt5);
                }
            }
        }

        return prompts;
    }


    // Metodo para contar o número de linhas em um ResultSet
    private static Integer contarLinhas(ResultSet resultSet) throws SQLException {
        Integer count = 0;
        while (resultSet.next()) {
            count++;
        }
        resultSet.beforeFirst();
        return count;
    }

    // Metodo para inserir a resposta no banco de dados
    private static void inserirNoBanco(Connection conexao, String resposta) throws SQLException {
        String sql = "INSERT INTO notificacao (texto, paraEmpresa, statusEnviada, fkCnpj) VALUES (?, ?, ?, ?)";

        try (PreparedStatement preparedStatement = conexao.prepareStatement(sql)) {
            preparedStatement.setString(1, resposta);
            preparedStatement.setBoolean(2, false);
            preparedStatement.setBoolean(3, false);
            preparedStatement.setString(4, null);

            Integer rowsInserted = preparedStatement.executeUpdate();
            conexao.commit();

            if (rowsInserted > 0) {
                System.out.println("Resposta do Gemini inserida no banco com sucesso!");
            }
        }
    }
}