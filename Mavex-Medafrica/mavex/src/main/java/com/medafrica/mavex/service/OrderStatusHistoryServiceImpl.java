package com.medafrica.mavex.service;

import com.medafrica.mavex.dto.order.OrderStatusHistoryResponse;
import com.medafrica.mavex.model.logistics.OrderStatusHistory;
import com.medafrica.mavex.repository.OrderStatusHistoryRepository;
import com.medafrica.mavex.service.interfaces.OrderStatusHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderStatusHistoryServiceImpl implements OrderStatusHistoryService {

    private final OrderStatusHistoryRepository historyRepository;

    @Override
    @Transactional(readOnly = true)
    public List<OrderStatusHistoryResponse> getHistoryByOrder(Long orderId) {
        return historyRepository.findByOrderIdOrderByChangedAtDesc(orderId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderStatusHistoryResponse> getHistoryByUser(Long userId) {
        return historyRepository.findByChangedByIdOrderByChangedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private OrderStatusHistoryResponse toResponse(OrderStatusHistory h) {
        return OrderStatusHistoryResponse.builder()
                .id(h.getId())
                .orderId(h.getOrder() != null ? h.getOrder().getId() : null)
                .hawb(h.getOrder() != null ? h.getOrder().getHawb() : null)
                .fromStatus(h.getFromStatus())
                .toStatus(h.getToStatus())
                .changedById(h.getChangedBy() != null ? h.getChangedBy().getId() : null)
                .changedByEmail(h.getChangedBy() != null ? h.getChangedBy().getEmail() : null)
                .note(h.getNote())
                .changedAt(h.getChangedAt())
                .build();
    }
}
