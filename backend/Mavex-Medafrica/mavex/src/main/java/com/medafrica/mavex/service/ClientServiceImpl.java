package com.medafrica.mavex.service;

import com.medafrica.mavex.dto.client.BulkActionRequest;
import com.medafrica.mavex.dto.client.ClientPatchRequest;
import com.medafrica.mavex.dto.client.ClientRequestDTO;
import com.medafrica.mavex.dto.client.ClientResponseDTO;
import com.medafrica.mavex.dto.client.ClientSearchCriteria;
import com.medafrica.mavex.dto.client.ClientStatsResponse;
import com.medafrica.mavex.model.actor.Client;
import com.medafrica.mavex.model.country.Country;
import com.medafrica.mavex.model.enums.OrderStatus;
import com.medafrica.mavex.model.logistics.Order;
import com.medafrica.mavex.repository.ClientRepository;
import com.medafrica.mavex.repository.CountryRepository;
import com.medafrica.mavex.repository.OrderRepository;
import com.medafrica.mavex.repository.specification.ClientSpecification;
import com.medafrica.mavex.service.interfaces.ClientService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final CountryRepository countryRepository;
    private final OrderRepository orderRepository;

    @Override
    public List<ClientResponseDTO> findAll() {
        return clientRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ClientResponseDTO> findAllActive() {
        return clientRepository.findByActiveTrue()
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Page<ClientResponseDTO> search(ClientSearchCriteria criteria, Pageable pageable) {
        return clientRepository.findAll(ClientSpecification.build(criteria), pageable)
                .map(this::toResponseDTO);
    }

    @Override
    public ClientStatsResponse getStats() {
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime startOfNextMonth = startOfMonth.plusMonths(1);

        long total    = clientRepository.count();
        long active   = clientRepository.countByActiveTrue();
        long inactive = clientRepository.countByActiveFalse();
        long newThisMonth = clientRepository.countByCreatedAtBetween(startOfMonth, startOfNextMonth);

        return ClientStatsResponse.builder()
                .totalClients(total)
                .activeClients(active)
                .inactiveClients(inactive)
                .newThisMonth(newThisMonth)
                .build();
    }

    @Override
    public ClientResponseDTO findById(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Client introuvable avec l'id : " + id));
        return toResponseDTO(client);
    }

    @Override
    @Transactional
    public ClientResponseDTO create(ClientRequestDTO dto) {
        if (clientRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Un client avec cet email existe déjà : " + dto.getEmail());
        }
        Country country = countryRepository.findById(dto.getCountryCode())
                .orElseThrow(() -> new EntityNotFoundException("Pays introuvable : " + dto.getCountryCode()));
        Client client = Client.builder()
                .fullName(dto.getFullName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .address(dto.getAddress())
                .city(dto.getCity())
                .state(dto.getState())
                .zipCode(dto.getZipCode())
                .country(country)
                .build();

        return toResponseDTO(clientRepository.save(client));
    }

    @Override
    @Transactional
    public ClientResponseDTO update(Long id, ClientRequestDTO dto) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Client introuvable avec l'id : " + id));

        if (!client.getEmail().equals(dto.getEmail()) && clientRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Un client avec cet email existe déjà : " + dto.getEmail());
        }

        String ancienEmail = client.getEmail();

        Country country = countryRepository.findById(dto.getCountryCode())
                .orElseThrow(() -> new EntityNotFoundException("Pays introuvable : " + dto.getCountryCode()));
        client.setFullName(dto.getFullName());
        client.setEmail(dto.getEmail());
        client.setPhone(dto.getPhone());
        client.setAddress(dto.getAddress());
        client.setCity(dto.getCity());
        client.setState(dto.getState());
        client.setZipCode(dto.getZipCode());
        client.setCountry(country);

        clientRepository.save(client);

        if (!ancienEmail.equals(dto.getEmail())) {
            markOrdersOutdated(client.getId(), ancienEmail);
        }

        return toResponseDTO(client);
    }

    @Override
    @Transactional
    public ClientResponseDTO patch(Long id, ClientPatchRequest dto) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Client introuvable avec l'id : " + id));

        if (dto.getEmail() != null
                && !client.getEmail().equals(dto.getEmail())
                && clientRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Un client avec cet email existe déjà : " + dto.getEmail());
        }

        String ancienEmail = client.getEmail();

        if (dto.getFullName()  != null) client.setFullName(dto.getFullName());
        if (dto.getEmail()     != null) client.setEmail(dto.getEmail());
        if (dto.getPhone()     != null) client.setPhone(dto.getPhone());
        if (dto.getAddress()   != null) client.setAddress(dto.getAddress());
        if (dto.getCity()      != null) client.setCity(dto.getCity());
        if (dto.getState()     != null) client.setState(dto.getState());
        if (dto.getZipCode()   != null) client.setZipCode(dto.getZipCode());

        if (dto.getCountryCode() != null) {
            Country country = countryRepository.findById(dto.getCountryCode())
                    .orElseThrow(() -> new EntityNotFoundException("Pays introuvable : " + dto.getCountryCode()));
            client.setCountry(country);
        }

        if (dto.getActive() != null) client.setActive(dto.getActive());

        clientRepository.save(client);

        if (dto.getEmail() != null && !ancienEmail.equals(dto.getEmail())) {
            markOrdersOutdated(client.getId(), ancienEmail);
        }

        return toResponseDTO(client);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!clientRepository.existsById(id)) {
            throw new EntityNotFoundException("Client introuvable avec l'id : " + id);
        }
        long orderCount = orderRepository.countByClientId(id);
        if (orderCount > 0) {
            throw new IllegalStateException(
                "Ce client possède " + orderCount + " order(s) et ne peut pas être supprimé."
            );
        }
        clientRepository.deleteById(id);
    }

    @Override
    @Transactional
    public int bulkDelete(BulkActionRequest req) {
        List<Client> clients = clientRepository.findAllById(req.getIds());
        List<String> blocked = clients.stream()
            .filter(c -> orderRepository.countByClientId(c.getId()) > 0)
            .map(c -> c.getFullName() != null ? c.getFullName() : "Client #" + c.getId())
            .toList();
        if (!blocked.isEmpty()) {
            throw new IllegalStateException(
                "Impossible de supprimer : " + String.join(", ", blocked) +
                " possède des orders liés."
            );
        }
        clientRepository.deleteAll(clients);
        return clients.size();
    }

    @Override
    @Transactional
    public int bulkActivate(BulkActionRequest req) {
        List<Client> clients = clientRepository.findAllById(req.getIds());
        clients.forEach(c -> c.setActive(true));
        clientRepository.saveAll(clients);
        return clients.size();
    }

    @Override
    @Transactional
    public int bulkDeactivate(BulkActionRequest req) {
        List<Client> clients = clientRepository.findAllById(req.getIds());
        clients.forEach(c -> c.setActive(false));
        clientRepository.saveAll(clients);
        return clients.size();
    }

    private void markOrdersOutdated(Long clientId, String ancienEmail) {
        List<Order> orders = orderRepository.findByClientId(clientId);
        for (Order order : orders) {
            if (order.getStatus() == OrderStatus.EMAIL_SENT
                    && ancienEmail.equals(order.getEmailSentToAddress())) {
                order.setStatus(OrderStatus.EMAIL_OUTDATED);
                order.setEmailOutdatedReason("L'email du client a changé depuis le dernier envoi.");
            }
        }
        orderRepository.saveAll(orders);
    }

    private ClientResponseDTO toResponseDTO(Client client) {
        return ClientResponseDTO.builder()
                .id(client.getId())
                .fullName(client.getFullName())
                .email(client.getEmail())
                .phone(client.getPhone())
                .address(client.getAddress())
                .city(client.getCity())
                .state(client.getState())
                .zipCode(client.getZipCode())
                .country(client.getCountry())
                .createdAt(client.getCreatedAt())
                .active(client.isActive())
                .build();
    }
}
