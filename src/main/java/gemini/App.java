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
                    String resumoPrompt = "Resuma o texto a seguir em no máximo 500 caracteres, mantendo os valores chave de informações, e faça sugestões de ações que devem ser tomadas em relação as mesmas: " + respostaCompleta;

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
                Statement stmtBaseMunicipio = conexao.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                Statement stmtBaseProjecao = conexao.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                Statement stmtEstacoesSMP = conexao.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

                // Consulta para cidade com menor conectividade
                ResultSet rsMenorConectividade = stmtBaseMunicipio.executeQuery(
                        "SELECT cidade.nomeCidade, estado.nomeEstado, baseMunicipio.domiciliosCobertosPercentual " +
                                "FROM baseMunicipio " +
                                "INNER JOIN cidade ON baseMunicipio.fkCidade = cidade.nomeCidade " +
                                "INNER JOIN estado ON cidade.fkEstado = estado.nomeEstado " +
                                "ORDER BY domiciliosCobertosPercentual ASC LIMIT 1"
                );

                // Consulta para estado com menor população
                ResultSet rsMenorPopulacao = stmtBaseProjecao.executeQuery(
                        "SELECT estado.nomeEstado, baseProjecaoPopulacional.ano, baseProjecaoPopulacional.projecao " +
                                "FROM baseProjecaoPopulacional " +
                                "INNER JOIN estado ON baseProjecaoPopulacional.fkEstado = estado.nomeEstado " +
                                "ORDER BY projecao ASC LIMIT 1"
                );

                // Consulta para cidade com menor infraestrutura tecnológica
                ResultSet rsMenorInfraestrutura = stmtEstacoesSMP.executeQuery(
                        "SELECT cidade.nomeCidade, estado.nomeEstado, baseEstacoesSMP.tecnologia, COUNT(baseEstacoesSMP.tecnologia) AS qtdEstacoes " +
                                "FROM baseEstacoesSMP " +
                                "INNER JOIN cidade ON baseEstacoesSMP.fkCidade = cidade.nomeCidade " +
                                "INNER JOIN estado ON cidade.fkEstado = estado.nomeEstado " +
                                "GROUP BY cidade.nomeCidade, estado.nomeEstado, baseEstacoesSMP.tecnologia " +
                                "ORDER BY qtdEstacoes ASC LIMIT 1"
                );
        ) {
            // Processar os resultados para conectividade
            if (rsMenorConectividade.next()) {
                String cidade = rsMenorConectividade.getString("nomeCidade");
                String estado = rsMenorConectividade.getString("nomeEstado");
                double conectividade = rsMenorConectividade.getDouble("domiciliosCobertosPercentual");
                String prompt1 = String.format(
                        "Dados identificados: Cidade: %s, Estado: %s, Conectividade: %.2f%%.",
                        cidade, estado, conectividade
                );
                prompts.add(prompt1);
            }

            // Processar os resultados para população
            if (rsMenorPopulacao.next()) {
                String estado = rsMenorPopulacao.getString("nomeEstado");
                int projecao = rsMenorPopulacao.getInt("projecao");
                int ano = rsMenorPopulacao.getInt("ano");
                String prompt2 = String.format(
                        "Dados identificados: Estado: %s, População projetada: %d, Ano: %d.",
                        estado, projecao, ano
                );
                prompts.add(prompt2);
            }

            // Processar os resultados para infraestrutura tecnológica
            if (rsMenorInfraestrutura.next()) {
                String cidade = rsMenorInfraestrutura.getString("nomeCidade");
                String estado = rsMenorInfraestrutura.getString("nomeEstado");
                String tecnologia = rsMenorInfraestrutura.getString("tecnologia");
                int qtdEstacoes = rsMenorInfraestrutura.getInt("qtdEstacoes");
                String prompt3 = String.format(
                        "Dados identificados: Cidade: %s, Estado: %s, Tecnologia: %s, Quantidade de estações: %d.",
                        cidade, estado, tecnologia, qtdEstacoes
                );
                prompts.add(prompt3);
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