package com.medafrica.mavex.service.analytics;

import com.medafrica.mavex.dto.analytics.EmailStatsDayDto;
import com.medafrica.mavex.dto.analytics.EmailStatsHistoryResponse;
import com.medafrica.mavex.dto.analytics.EmailStatsSummaryDto;
import com.medafrica.mavex.model.analytics.EmailStatsDaily;
import com.medafrica.mavex.repository.EmailLogRepository;
import com.medafrica.mavex.repository.EmailStatsDailyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailStatsService {

    private final EmailLogRepository        emailLogRepository;
    private final EmailStatsDailyRepository emailStatsDailyRepository;

    @Transactional
    public void computeAndSaveStatsForDate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end   = date.plusDays(1).atStartOfDay();

        long sent      = emailLogRepository.countBySentAtBetween(start, end);
        long delivered = emailLogRepository.countByDeliveredAtBetween(start, end);
        long opened    = emailLogRepository.countByOpenedAtBetween(start, end);
        long bounced   = emailLogRepository.countByBouncedAtBetween(start, end);

        EmailStatsDaily stats = emailStatsDailyRepository.findByStatDate(date)
            .orElseGet(() -> EmailStatsDaily.builder().statDate(date).build());

        stats.setEmailsSent((int) sent);
        stats.setEmailsDelivered((int) delivered);
        stats.setEmailsOpened((int) opened);
        stats.setEmailsBounced((int) bounced);

        emailStatsDailyRepository.save(stats);
        log.info("Stats email calculées pour {} — envoyés={}, livrés={}, ouverts={}, bounced={}",
            date, sent, delivered, opened, bounced);
    }

    public EmailStatsHistoryResponse getStatsHistory(int days) {
        LocalDate end   = LocalDate.now();
        LocalDate start = end.minusDays(days - 1);

        Map<LocalDate, EmailStatsDaily> byDate = emailStatsDailyRepository
            .findByStatDateBetweenOrderByStatDateAsc(start, end)
            .stream()
            .collect(Collectors.toMap(EmailStatsDaily::getStatDate, s -> s));

        List<EmailStatsDayDto> dayDtos = start.datesUntil(end.plusDays(1))
            .map(date -> {
                EmailStatsDaily s = byDate.get(date);
                return EmailStatsDayDto.builder()
                    .date(date)
                    .sent(s != null ? s.getEmailsSent() : 0)
                    .delivered(s != null ? s.getEmailsDelivered() : 0)
                    .opened(s != null ? s.getEmailsOpened() : 0)
                    .bounced(s != null ? s.getEmailsBounced() : 0)
                    .build();
            })
            .toList();

        int totalSent      = dayDtos.stream().mapToInt(EmailStatsDayDto::getSent).sum();
        int totalDelivered = dayDtos.stream().mapToInt(EmailStatsDayDto::getDelivered).sum();
        int totalOpened    = dayDtos.stream().mapToInt(EmailStatsDayDto::getOpened).sum();
        int totalBounced   = dayDtos.stream().mapToInt(EmailStatsDayDto::getBounced).sum();

        double deliveryRate = totalSent == 0
            ? 0.0
            : Math.round(((double) totalDelivered / totalSent) * 100 * 10) / 10.0;

        double openRate = totalDelivered == 0
            ? 0.0
            : Math.round(((double) totalOpened / totalDelivered) * 100 * 10) / 10.0;

        EmailStatsSummaryDto summary = EmailStatsSummaryDto.builder()
            .totalSent(totalSent)
            .totalDelivered(totalDelivered)
            .totalOpened(totalOpened)
            .totalBounced(totalBounced)
            .deliveryRate(deliveryRate)
            .openRate(openRate)
            .build();

        return EmailStatsHistoryResponse.builder()
            .days(dayDtos)
            .summary(summary)
            .build();
    }
}
