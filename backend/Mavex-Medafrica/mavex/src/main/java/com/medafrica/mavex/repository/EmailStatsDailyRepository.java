package com.medafrica.mavex.repository;

import com.medafrica.mavex.model.analytics.EmailStatsDaily;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EmailStatsDailyRepository extends JpaRepository<EmailStatsDaily, Long> {

    Optional<EmailStatsDaily> findByStatDate(LocalDate date);

    List<EmailStatsDaily> findByStatDateBetweenOrderByStatDateAsc(LocalDate start, LocalDate end);

}
