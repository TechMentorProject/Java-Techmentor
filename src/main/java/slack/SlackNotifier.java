package slack;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;

public class SlackNotifier {

    public static boolean sendNotification(String message, String webhookUrl) {
        try {
            URL url = new URL(webhookUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            String payload = "{ \"text\": \"" + message + "\" }";

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = payload.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return true;
            } else {
                System.out.println("Erro ao enviar a mensagem. Código de resposta: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false; 
    }

    public static void processNotifications() {
        String dbUrl = "jdbc:mysql://localhost:3306/techmentor";
        String dbUser = "root";
        String dbPassword = "root";

        String selectQuery = "SELECT texto, paraEmpresa, statusEnviada, fkCnpj FROM notificacao WHERE statusEnviada = false";
        String webhookQuery = "SELECT webhook FROM empresa WHERE cnpj = ?";
        String updateQuery = "UPDATE notificacao SET statusEnviada = true WHERE texto = ?";

        int qtdMensagemEmpresa = 0;
        int qtdMensagemFunc = 0;

        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement selectStmt = connection.prepareStatement(selectQuery);
             PreparedStatement webhookStmt = connection.prepareStatement(webhookQuery);
             PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {

            ResultSet rs = selectStmt.executeQuery();

            while (rs.next()) {
                String texto = rs.getString("texto");
                boolean paraEmpresa = rs.getBoolean("paraEmpresa");
                boolean statusEnviada = rs.getBoolean("statusEnviada");
                String fkCnpj = rs.getString("fkCnpj");
                boolean enviadoComSucesso = false;

                if (!statusEnviada && paraEmpresa) {
                    webhookStmt.setString(1, fkCnpj);
                    ResultSet webhookRs = webhookStmt.executeQuery();

                    if (webhookRs.next()) {
                        String webhookUrl = webhookRs.getString("webhook");
                        enviadoComSucesso = sendNotification(texto, webhookUrl);
                        if (enviadoComSucesso) {
                            qtdMensagemEmpresa++;
                        }
                    } else {
                        System.out.println("Webhook não encontrado para o CNPJ: " + fkCnpj);
                    }
                } else if (!statusEnviada) {
                    String webhookUrl = System.getenv("SLACK_WEBHOOK_GERAL");
                    enviadoComSucesso = sendNotification(texto, webhookUrl);
                    if (enviadoComSucesso) {
                        qtdMensagemFunc++;
                    }
                }

                if (enviadoComSucesso) {
                    updateStmt.setString(1, texto);
                    updateStmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        System.out.println("Total de mensagens enviadas para empresas: " + qtdMensagemEmpresa);
        System.out.println("Total de mensagens enviadas para funcionários: " + qtdMensagemFunc);
    }
}
