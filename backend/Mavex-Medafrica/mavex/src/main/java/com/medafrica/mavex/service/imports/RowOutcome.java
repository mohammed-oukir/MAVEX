package com.medafrica.mavex.service.imports;

import com.medafrica.mavex.model.enums.ImportRowStatus;

/**
 * Résultat du traitement d'une ligne d'import, isolé dans sa propre transaction.
 * Permet de remonter le statut sans laisser une exception polluer la transaction
 * globale de l'import.
 */
public record RowOutcome(
        ImportRowStatus status,
        String reason,
        String warnings,
        String mawb
) {
    public static RowOutcome imported(String warnings, String mawb) {
        return new RowOutcome(ImportRowStatus.IMPORTED, null, warnings, mawb);
    }

    public static RowOutcome skipped(String reason) {
        return new RowOutcome(ImportRowStatus.SKIPPED, reason, null, null);
    }

    public static RowOutcome failed(String reason) {
        return new RowOutcome(ImportRowStatus.FAILED, reason, null, null);
    }
}
