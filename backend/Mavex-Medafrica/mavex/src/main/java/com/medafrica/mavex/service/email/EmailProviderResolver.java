package com.medafrica.mavex.service.email;

import com.medafrica.mavex.model.email.EmailProviderConfig;
import com.medafrica.mavex.model.enums.EmailProviderType;
import com.medafrica.mavex.repository.EmailProviderConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Selectionne l'implementation EmailProviderService correspondant au provider actif en base.
 * Spring injecte automatiquement toutes les implementations de EmailProviderService dans la liste.
 */
@Component
@RequiredArgsConstructor
public class EmailProviderResolver {

    private final EmailProviderConfigRepository configRepository;
    private final List<EmailProviderService>    providers;

    /**
     * Retourne l'implementation du provider actuellement actif.
     *
     * @throws IllegalStateException si aucun provider n'est actif en base
     * @throws IllegalStateException si le type actif n'a pas d'implementation enregistree
     */
    public EmailProviderService resolve() {
        EmailProviderConfig activeConfig = configRepository.findByActiveTrue()
                .orElseThrow(() -> new IllegalStateException(
                        "Aucun provider email actif. Configurez un provider dans Parametres > Provider Email."));

        EmailProviderType activeType = activeConfig.getProviderType();

        return providers.stream()
                .filter(p -> p.getType() == activeType)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Aucune implementation trouvee pour le provider : " + activeType
                        + ". Verifiez que le bean est bien enregistre dans le contexte Spring."));
    }
}
