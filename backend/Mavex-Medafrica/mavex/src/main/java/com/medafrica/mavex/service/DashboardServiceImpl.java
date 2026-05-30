package com.medafrica.mavex.service;

import com.medafrica.mavex.dto.dashboard.*;
import com.medafrica.mavex.model.enums.OrderStatus;
import com.medafrica.mavex.model.enums.ShipmentStatus;
import com.medafrica.mavex.repository.OrderRepository;
import com.medafrica.mavex.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl {

    private final ShipmentRepository shipmentRepository;
    private final OrderRepository    orderRepository;

    private static final DateTimeFormatter MONTH_FMT =
        DateTimeFormatter.ofPattern("MMM yyyy", Locale.FRENCH);

    @Transactional(readOnly = true)
    public DashboardKpiDTO getKpis() {
        long totalShipments  = shipmentRepository.count();
        long activeShipments = shipmentRepository.countByStatusNot(ShipmentStatus.CLOSED);
        long totalOrders     = orderRepository.count();
        long paidOrders      = orderRepository.countByStatus(OrderStatus.PAID);
        long pendingOrders   = orderRepository.countByStatus(OrderStatus.PENDING_PAYMENT);
        BigDecimal paidAmt   = orderRepository.sumPaidAmount();
        BigDecimal pendAmt   = orderRepository.sumPendingAmount();

        return DashboardKpiDTO.builder()
                .totalShipments(totalShipments)
                .activeShipments(activeShipments)
                .totalOrders(totalOrders)
                .paidOrders(paidOrders)
                .pendingOrders(pendingOrders)
                .totalPaidAmount(paidAmt != null ? paidAmt : BigDecimal.ZERO)
                .totalPendingAmount(pendAmt != null ? pendAmt : BigDecimal.ZERO)
                .build();
    }

    @Transactional(readOnly = true)
    public List<StatusCountDTO> getOrdersByStatus() {
        List<Object[]> rows = orderRepository.countGroupByStatus();
        List<StatusCountDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new StatusCountDTO(row[0].toString(), ((Number) row[1]).longValue()));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<MonthlyCountDTO> getShipmentsByMonth() {
        LocalDateTime from = LocalDateTime.now().minusMonths(5).withDayOfMonth(1)
                                          .withHour(0).withMinute(0).withSecond(0);
        List<Object[]> rows = shipmentRepository.countByMonth(from);

        Map<YearMonth, Long> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            int year  = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            long cnt  = ((Number) row[2]).longValue();
            map.put(YearMonth.of(year, month), cnt);
        }
        return buildMonthlyCount(map);
    }

    @Transactional(readOnly = true)
    public List<MonthlyRevenueDTO> getRevenueByMonth() {
        LocalDateTime from = LocalDateTime.now().minusMonths(5).withDayOfMonth(1)
                                          .withHour(0).withMinute(0).withSecond(0);
        List<Object[]> rows = orderRepository.revenueByMonth(from);

        Map<YearMonth, BigDecimal> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            int year   = ((Number) row[0]).intValue();
            int month  = ((Number) row[1]).intValue();
            BigDecimal amt = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;
            map.put(YearMonth.of(year, month), amt);
        }
        return buildMonthlyRevenue(map);
    }

    @Transactional(readOnly = true)
    public AlertsDTO getAlerts() {
        long neverEmailed  = orderRepository.countByStatus(OrderStatus.CREATED);
        long stuck         = shipmentRepository.countByStatus(ShipmentStatus.PROCESSING);
        LocalDateTime week = LocalDateTime.now().minusDays(7);
        long overdue       = orderRepository.countOverduePayments(week);

        return AlertsDTO.builder()
                .ordersNeverEmailed(neverEmailed)
                .stuckShipments(stuck)
                .overduePayments(overdue)
                .build();
    }

    /* ── Helpers ────────────────────────────────────────────── */

    private List<MonthlyCountDTO> buildMonthlyCount(Map<YearMonth, Long> data) {
        List<MonthlyCountDTO> result = new ArrayList<>();
        YearMonth current = YearMonth.now().minusMonths(5);
        for (int i = 0; i < 6; i++) {
            long count = data.getOrDefault(current, 0L);
            result.add(new MonthlyCountDTO(current.format(MONTH_FMT), count));
            current = current.plusMonths(1);
        }
        return result;
    }

    private List<MonthlyRevenueDTO> buildMonthlyRevenue(Map<YearMonth, BigDecimal> data) {
        List<MonthlyRevenueDTO> result = new ArrayList<>();
        YearMonth current = YearMonth.now().minusMonths(5);
        for (int i = 0; i < 6; i++) {
            BigDecimal amount = data.getOrDefault(current, BigDecimal.ZERO);
            result.add(new MonthlyRevenueDTO(current.format(MONTH_FMT), amount));
            current = current.plusMonths(1);
        }
        return result;
    }
}
