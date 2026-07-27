package com.medafrica.mavex.service.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    @Value("${app.upload.dir}")
    private String uploadDir;

    /**
     * Sauvegarde le fichier dans {app.upload.dir}/{subFolder}/ avec un nom unique (UUID).
     * @return le chemin complet du fichier sauvegardé
     */
    public String store(MultipartFile file, String subFolder) {
        try {
            Path targetDir = Path.of(uploadDir, subFolder);
            Files.createDirectories(targetDir);

            String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
            String fileName = UUID.randomUUID() + (extension != null ? "." + extension : "");

            Path targetPath = targetDir.resolve(fileName);

            try (InputStream is = file.getInputStream()) {
                Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            return targetPath.toString();
        } catch (IOException e) {
            throw new UncheckedIOException("Echec du stockage du fichier: " + file.getOriginalFilename(), e);
        }
    }

    /** Supprime le fichier s'il existe. */
    public void delete(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(Path.of(filePath));
        } catch (IOException e) {
            log.warn("Impossible de supprimer le fichier: {}", filePath, e);
        }
    }
}
