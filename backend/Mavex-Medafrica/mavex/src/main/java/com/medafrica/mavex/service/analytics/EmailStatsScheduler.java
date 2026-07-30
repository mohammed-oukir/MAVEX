package com.medafrica.mavex.service.analytics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailStatsScheduler {

    private final EmailStatsService emailStatsService;

    /** Calcule chaque nuit à 1h00 les stats email de la journée qui vient de se terminer */
    @Scheduled(cron = "0 0 1 * * *")
    public void computeStatsForYesterday() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("Démarrage du calcul planifié des stats email pour {}", yesterday);
        emailStatsService.computeAndSaveStatsForDate(yesterday);
    }
}
