package slack;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;

public class SlackNotifier {

    public static void sendNotification(String message, boolean paraEmpresa) {
        try {
            String webhookUrl = paraEmpresa
                    ? System.getenv("SLACK_WEBHOOK_EMPRESA")
                    : System.getenv("SLACK_WEBHOOK_GERAL");

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
            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.out.println("Erro ao enviar a mensagem. Código de resposta: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void processNotifications() {
        String dbUrl = "jdbc:mysql://localhost:3306/techmentor";
        String dbUser = "root";
        String dbPassword = "root";
        String selectQuery = "SELECT texto, paraEmpresa, statusEnviada FROM notificacao WHERE statusEnviada = false";
        String updateQuery = "UPDATE notificacao SET statusEnviada = true WHERE texto = ?";
        int qtdMensagemEmpresa = 0;
        int qtdMensagemFunc = 0;

        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement selectStmt = connection.prepareStatement(selectQuery);
             PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {

            ResultSet rs = selectStmt.executeQuery();

            while (rs.next()) {
                String texto = rs.getString("texto");
                boolean paraEmpresa = rs.getBoolean("paraEmpresa");
                boolean statusEnviada = rs.getBoolean("statusEnviada");

                if (!statusEnviada) {
                    if (paraEmpresa) {
                        sendNotification(texto, true);
                        qtdMensagemEmpresa ++;
                    } else {
                        sendNotification(texto, false);
                        qtdMensagemFunc ++;
                    }
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

    public static void main(String[] args) {
        processNotifications();
    }
}
