import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class ResendMailService {

    private static final String SCRIPT_URL = "https://script.google.com/macros/s/AKfycbxnM2WQwqcesxVNHtcu-7Oa39USHuv_cPFDgxVfZz943uTdQCT5XtKNpDqh4gWXynA6/exec";

    public void sendOtpEmail(String recipientEmail, String otpCode) {
        try {
            String jsonPayload = String.format("{\"email\":\"%s\",\"otp\":\"%s\"}", recipientEmail, otpCode);

            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SCRIPT_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                System.out.println("OTP email dispatched via Google Apps Script to: " + recipientEmail);
            } else {
                System.err.println("Failed to send email via script. Status code: " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("Error sending email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}