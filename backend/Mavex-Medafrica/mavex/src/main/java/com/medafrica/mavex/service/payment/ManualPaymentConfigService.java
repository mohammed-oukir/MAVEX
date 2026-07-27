package com.medafrica.mavex.service.payment;

import com.medafrica.mavex.dto.payment.ManualPaymentConfigResponse;
import com.medafrica.mavex.model.payment.ManualPaymentConfig;
import com.medafrica.mavex.model.security.User;
import com.medafrica.mavex.repository.ManualPaymentConfigRepository;
import com.medafrica.mavex.service.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ManualPaymentConfigService {

    private final ManualPaymentConfigRepository configRepository;
    private final FileStorageService            fileStorageService;

    @Transactional
    public ManualPaymentConfigResponse uploadRib(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier RIB est obligatoire.");
        }
        if (!"application/pdf".equals(file.getContentType())) {
            throw new IllegalArgumentException("Le RIB doit être un fichier PDF.");
        }

        String storedPath = fileStorageService.store(file, "rib");

        ManualPaymentConfig config = findExisting()
                .orElseGet(ManualPaymentConfig::new);

        if (config.getId() != null && config.getRibFilePath() != null) {
            fileStorageService.delete(config.getRibFilePath());
        }

        config.setRibFilePath(storedPath);
        config.setRibFileName(file.getOriginalFilename());
        config.setUploadedAt(LocalDateTime.now());
        config.setUploadedBy(currentUser());

        return toResponse(configRepository.save(config));
    }

    public Optional<ManualPaymentConfigResponse> getCurrentRib() {
        return findExisting().map(this::toResponse);
    }

    // ---------------------------------------------------------------
    // HELPERS PRIVES
    // ---------------------------------------------------------------

    private Optional<ManualPaymentConfig> findExisting() {
        List<ManualPaymentConfig> all = configRepository.findAll();
        return all.isEmpty() ? Optional.empty() : Optional.of(all.get(0));
    }

    private ManualPaymentConfigResponse toResponse(ManualPaymentConfig config) {
        return ManualPaymentConfigResponse.builder()
                .ribFileName(config.getRibFileName())
                .uploadedAt(config.getUploadedAt())
                .build();
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }
}
