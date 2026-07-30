package com.medafrica.mavex.service.analytics;

import com.medafrica.mavex.dto.analytics.BrevoQuotaResponse;
import com.medafrica.mavex.model.email.EmailProviderConfig;
import com.medafrica.mavex.repository.EmailProviderConfigRepository;
import com.medafrica.mavex.service.email.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrevoAccountService {

    private static final String BREVO_ACCOUNT_URL = "https://api.brevo.com/v3/account";

    private final EmailProviderConfigRepository configRepository;
    private final EncryptionService             encryptionService;
    private final RestClient                    restClient;

    @Cacheable(value = "brevoQuota", unless = "#result.available == false")
    public BrevoQuotaResponse getEmailQuota() {
        try {
            EmailProviderConfig config = configRepository.findByActiveTrue()
                .orElse(null);

            if (config == null || config.getBrevoApiKeyEncrypted() == null) {
                return BrevoQuotaResponse.builder()
                    .available(false)
                    .errorMessage("Impossible de récupérer le quota Brevo : aucune clé API configurée.")
                    .build();
            }

            String apiKey = encryptionService.decrypt(config.getBrevoApiKeyEncrypted());

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                .uri(BREVO_ACCOUNT_URL)
                .header("api-key", apiKey)
                .retrieve()
                .body(Map.class);

            Integer remainingCredits = extractRemainingCredits(response);
            if (remainingCredits == null) {
                return BrevoQuotaResponse.builder()
                    .available(false)
                    .errorMessage("Impossible de récupérer le quota Brevo : réponse inattendue.")
                    .build();
            }

            return BrevoQuotaResponse.builder()
                .remainingCredits(remainingCredits)
                .available(true)
                .build();

        } catch (Exception e) {
            log.warn("Echec de récupération du quota Brevo : {}", e.getMessage());
            return BrevoQuotaResponse.builder()
                .available(false)
                .errorMessage("Impossible de récupérer le quota Brevo.")
                .build();
        }
    }

    private Integer extractRemainingCredits(Map<String, Object> response) {
        if (response == null) return null;

        Object planObj = response.get("plan");
        if (!(planObj instanceof List<?> planList)) return null;

        for (Object entry : planList) {
            if (entry instanceof Map<?, ?> planEntry
                    && "sendLimit".equals(planEntry.get("creditsType"))
                    && planEntry.get("credits") instanceof Number credits) {
                return credits.intValue();
            }
        }
        return null;
    }
}
