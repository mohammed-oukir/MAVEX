package com.medafrica.mavex.data;

import com.medafrica.mavex.model.permission.PermissionCatalog;
import com.medafrica.mavex.repository.PermissionCatalogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Initialise le catalogue de permissions (module + action) au demarrage.
 * Idempotent : ne recree jamais une ligne (module, action) deja presente.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionCatalogInitializer implements CommandLineRunner {

    private final PermissionCatalogRepository permissionCatalogRepository;

    @Override
    public void run(String... args) {
        Set<String> existingKeys = permissionCatalogRepository.findAll().stream()
                .map(p -> key(p.getModule(), p.getAction()))
                .collect(Collectors.toSet());

        int created = 0;
        int skipped = 0;

        for (String[] row : PERMISSIONS) {
            String module      = row[0];
            String action       = row[1];
            String label        = row[2];
            String contextNote  = row[3];

            if (existingKeys.contains(key(module, action))) {
                skipped++;
                continue;
            }

            PermissionCatalog permission = PermissionCatalog.builder()
                    .module(module)
                    .action(action)
                    .label(label)
                    .contextNote(contextNote)
                    .build();

            permissionCatalogRepository.save(permission);
            existingKeys.add(key(module, action));
            created++;
        }

        log.info("✅ Catalogue de permissions : {} déjà présentes, {} créées.", skipped, created);
    }

    private String key(String module, String action) {
        return module + "::" + action;
    }

    /** module | action | label | contextNote (null si pas de note) */
    private static final String[][] PERMISSIONS = {
        { "SHIPMENTS", "VIEW",                     "Voir la liste des shipments",                     null },
        { "SHIPMENTS", "CREATE",                   "Créer un shipment",                               null },
        { "SHIPMENTS", "UPDATE",                   "Modifier un shipment",                            null },
        { "SHIPMENTS", "DELETE",                   "Supprimer un shipment",                           null },
        { "SHIPMENTS", "CHANGE_STATUS",             "Changer le statut d'un shipment",                 null },
        { "SHIPMENTS", "UPDATE_DUTY_RATE",          "Modifier le taux douanier",                       null },
        { "SHIPMENTS", "EXPORT",                    "Exporter (PDF/Excel)",                            null },

        { "ORDERS", "VIEW",                         "Voir la liste des commandes",                     null },
        { "ORDERS", "CREATE",                       "Créer une commande",                              "Utilisé dans : détail d'un shipment" },
        { "ORDERS", "UPDATE",                       "Modifier une commande",                           "Utilisé dans : détail d'un shipment" },
        { "ORDERS", "DELETE",                       "Supprimer une commande",                          "Utilisé dans : détail d'un shipment" },
        { "ORDERS", "CHANGE_STATUS",                "Changer le statut d'une commande",                "Utilisé dans : détail d'un shipment" },
        { "ORDERS", "SEND_EMAIL",                   "Envoyer un email de paiement (individuel)",       "Utilisé dans : détail d'un shipment" },
        { "ORDERS", "BULK_SEND_EMAIL",              "Envoyer les emails en masse",                     "Utilisé dans : détail d'un shipment" },
        { "ORDERS", "BULK_CHANGE_STATUS",           "Changer le statut en masse",                      null },
        { "ORDERS", "GENERATE_PAYMENT_LINK",        "Générer un lien de paiement",                     "Action sensible" },
        { "ORDERS", "EXPORT",                       "Exporter (PDF/Excel)",                            null },
        { "ORDERS", "VIEW_STATUS_HISTORY",          "Voir l'historique des statuts",                   null },
        { "ORDERS", "VIEW_EMAIL_LOGS",               "Voir les logs d'envoi email",                     null },

        { "CLIENTS", "VIEW",                        "Voir la liste des clients",                       null },
        { "CLIENTS", "CREATE",                      "Créer un client",                                 null },
        { "CLIENTS", "UPDATE",                      "Modifier un client",                              null },
        { "CLIENTS", "DELETE",                      "Supprimer un client",                             null },
        { "CLIENTS", "BULK_ACTIONS",                "Actions en masse (suppr./activ./désactiv.)",      null },

        { "SHIPPERS", "VIEW",                       "Voir la liste des shippers",                      null },
        { "SHIPPERS", "CREATE",                     "Créer un shipper",                                null },
        { "SHIPPERS", "UPDATE",                     "Modifier un shipper",                             null },
        { "SHIPPERS", "DELETE",                     "Supprimer un shipper",                            null },
        { "SHIPPERS", "ACTIVATE_DEACTIVATE",        "Activer/désactiver un shipper",                   null },
        { "SHIPPERS", "BULK_ACTIONS",               "Actions en masse",                                null },

        { "AIRLINES", "VIEW",                       "Voir la liste des compagnies",                    null },
        { "AIRLINES", "CREATE",                     "Créer une compagnie",                             null },
        { "AIRLINES", "UPDATE",                     "Modifier une compagnie",                          null },
        { "AIRLINES", "DELETE",                     "Supprimer une compagnie",                         null },

        { "IMPORTS", "VIEW",                        "Voir la liste des imports",                       null },
        { "IMPORTS", "PREPARE",                     "Préparer un import (upload/preview/modifier ligne)", "Aucun effet en base" },
        { "IMPORTS", "CONFIRM",                     "Confirmer un import",                             "Action sensible — écrit en base" },
        { "IMPORTS", "DELETE",                      "Supprimer un import confirmé",                    null },

        { "PAYMENTS", "VIEW",                       "Voir la liste des transactions",                  null },
        { "PAYMENTS", "EXPORT",                     "Exporter (PDF/Excel)",                            null },
        { "PAYMENTS", "SEND_RECEIPT",                "Renvoyer un reçu au client",                      null },

        { "EMAIL_SETTINGS", "VIEW_NOTIFICATION_TYPES",   "Voir les types de notification",              null },
        { "EMAIL_SETTINGS", "CREATE_NOTIFICATION_TYPE",  "Créer un type de notification",               null },
        { "EMAIL_SETTINGS", "UPDATE_NOTIFICATION_TYPE",  "Modifier un type de notification",            null },
        { "EMAIL_SETTINGS", "DELETE_NOTIFICATION_TYPE",  "Supprimer un type de notification",           null },
        { "EMAIL_SETTINGS", "VIEW_TEMPLATES",            "Voir les templates email",                    null },
        { "EMAIL_SETTINGS", "CREATE_TEMPLATE",           "Créer un template email",                     null },
        { "EMAIL_SETTINGS", "UPDATE_TEMPLATE",           "Modifier un template email (éditeur GrapesJS)", null },

        { "DASHBOARD_ANALYTICS", "VIEW",                 "Voir les statistiques d'emails et le quota Brevo", null },

        { "DASHBOARD", "VIEW",                           "Voir le tableau de bord général",             null },
    };
}
