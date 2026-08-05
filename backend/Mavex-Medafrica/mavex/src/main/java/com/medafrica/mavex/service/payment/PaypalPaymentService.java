package com.medafrica.mavex.service.payment;

import com.medafrica.mavex.dto.payment.PaypalConfigResponse;
import com.medafrica.mavex.dto.payment.PaypalOrderResult;
import com.medafrica.mavex.model.enums.PaymentGatewayMode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaypalPaymentService {

    private static final String SANDBOX_TOKEN_URL     = "https://api-m.sandbox.paypal.com/v1/oauth2/token";
    private static final String PRODUCTION_TOKEN_URL   = "https://api-m.paypal.com/v1/oauth2/token";
    private static final String SANDBOX_ORDERS_URL     = "https://api-m.sandbox.paypal.com/v2/checkout/orders";
    private static final String PRODUCTION_ORDERS_URL  = "https://api-m.paypal.com/v2/checkout/orders";
    private static final String SANDBOX_VERIFY_URL      = "https://api-m.sandbox.paypal.com/v1/notifications/verify-webhook-signature";
    private static final String PRODUCTION_VERIFY_URL   = "https://api-m.paypal.com/v1/notifications/verify-webhook-signature";

    private final PaypalConfigService configService;
    private final RestClient          restClient;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    private String getAccessToken() {
        PaypalConfigResponse config = configService.getCurrentConfig()
                .orElseThrow(() -> new IllegalStateException("Aucune configuration PayPal enregistree."));

        String clientId     = config.getClientId();
        String clientSecret = configService.getDecryptedSecret();
        String tokenUrl     = config.getMode() == PaymentGatewayMode.PRODUCTION
                ? PRODUCTION_TOKEN_URL
                : SANDBOX_TOKEN_URL;

        String credentials = Base64.getEncoder().encodeToString(
                (clientId + ":" + clientSecret).getBytes());

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .header("Authorization", "Basic " + credentials)
                    .body("grant_type=client_credentials")
                    .retrieve()
                    .body(Map.class);

            Object token = (response != null) ? response.get("access_token") : null;
            if (token == null) {
                throw new IllegalStateException("Reponse PayPal invalide : access_token absent.");
            }
            return (String) token;

        } catch (Exception e) {
            throw new IllegalStateException("Echec de recuperation du token PayPal : " + e.getMessage(), e);
        }
    }

    public PaypalOrderResult createOrder(String referenceId, BigDecimal amount, String currencyCode) {
        String accessToken = getAccessToken();
        boolean isProduction = configService.getCurrentConfig()
                .orElseThrow(() -> new IllegalStateException("Aucune configuration PayPal enregistree."))
                .getMode() == PaymentGatewayMode.PRODUCTION;
        String ordersUrl = isProduction ? PRODUCTION_ORDERS_URL : SANDBOX_ORDERS_URL;

        Map<String, Object> body = Map.of(
                "intent", "CAPTURE",
                "purchase_units", List.of(Map.of(
                        "reference_id", referenceId,
                        "amount", Map.of(
                                "currency_code", currencyCode,
                                "value", amount.toPlainString()
                        )
                )),
                "application_context", Map.of(
                        "return_url", frontendUrl + "/pay-success",
                        "cancel_url", frontendUrl + "/pay-cancelled"
                )
        );

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(ordersUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + accessToken)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null || response.get("id") == null) {
                throw new IllegalStateException("Reponse PayPal invalide : id de commande absent.");
            }
            String paypalOrderId = (String) response.get("id");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> links = (List<Map<String, Object>>) response.get("links");
            String approveUrl = extractApproveUrl(links);
            if (approveUrl == null) {
                throw new IllegalStateException("Reponse PayPal invalide : lien approve absent.");
            }

            return new PaypalOrderResult(paypalOrderId, approveUrl);

        } catch (Exception e) {
            throw new IllegalStateException("Echec de creation de la commande PayPal : " + e.getMessage(), e);
        }
    }

    private String extractApproveUrl(List<Map<String, Object>> links) {
        if (links == null) return null;
        for (Map<String, Object> link : links) {
            if ("approve".equals(link.get("rel"))) {
                return (String) link.get("href");
            }
        }
        return null;
    }

    public boolean captureOrder(String paypalOrderId) {
        String accessToken = getAccessToken();
        boolean isProduction = configService.getCurrentConfig()
                .orElseThrow(() -> new IllegalStateException("Aucune configuration PayPal enregistree."))
                .getMode() == PaymentGatewayMode.PRODUCTION;
        String baseUrl = isProduction ? PRODUCTION_ORDERS_URL : SANDBOX_ORDERS_URL;
        String captureUrl = baseUrl + "/" + paypalOrderId + "/capture";

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(captureUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            return response != null && "COMPLETED".equals(response.get("status"));

        } catch (Exception e) {
            throw new IllegalStateException("Echec de capture de la commande PayPal : " + e.getMessage(), e);
        }
    }

    public boolean verifyWebhookSignature(String authAlgo, String certUrl, String transmissionId,
                                           String transmissionSig, String transmissionTime,
                                           Map<String, Object> webhookEventBody) {
        PaypalConfigResponse config = configService.getCurrentConfig()
                .orElseThrow(() -> new IllegalStateException("Aucune configuration PayPal enregistree."));
        String webhookId = config.getWebhookId();

        String accessToken = getAccessToken();
        String verifyUrl = config.getMode() == PaymentGatewayMode.PRODUCTION
                ? PRODUCTION_VERIFY_URL
                : SANDBOX_VERIFY_URL;

        Map<String, Object> body = Map.of(
                "auth_algo", authAlgo,
                "cert_url", certUrl,
                "transmission_id", transmissionId,
                "transmission_sig", transmissionSig,
                "transmission_time", transmissionTime,
                "webhook_id", webhookId,
                "webhook_event", webhookEventBody
        );

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(verifyUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + accessToken)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            return response != null && "SUCCESS".equals(response.get("verification_status"));

        } catch (Exception e) {
            throw new IllegalStateException("Echec de verification de signature webhook PayPal : " + e.getMessage(), e);
        }
    }
}
