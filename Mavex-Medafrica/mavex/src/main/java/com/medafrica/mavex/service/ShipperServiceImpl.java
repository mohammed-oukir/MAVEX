package com.medafrica.mavex.service;

import com.medafrica.mavex.dto.shipper.ShipperRequestDTO;
import com.medafrica.mavex.dto.shipper.ShipperResponseDTO;
import com.medafrica.mavex.model.actor.Shipper;
import com.medafrica.mavex.repository.ShipperRepository;
import com.medafrica.mavex.service.interfaces.ShipperService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShipperServiceImpl implements ShipperService {

    private final ShipperRepository shipperRepository;

    @Override
    @Transactional
    public ShipperResponseDTO create(ShipperRequestDTO req) {

        if (shipperRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email déjà utilisé : " + req.getEmail());
        }

        Shipper shipper = Shipper.builder()
                .companyName(req.getCompanyName())
                .contactName(req.getContactName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .address(req.getAddress())
                .city(req.getCity())
                .country(req.getCountryCode())
                .build();

        return toResponse(shipperRepository.save(shipper));
    }

    @Override
    @Transactional(readOnly = true)
    public ShipperResponseDTO getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShipperResponseDTO> list(Pageable pageable) {
        return shipperRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional
    public ShipperResponseDTO update(Long id, ShipperRequestDTO req) {

        Shipper shipper = findOrThrow(id);

        if (req.getEmail() != null && !req.getEmail().equalsIgnoreCase(shipper.getEmail())) {
            if (shipperRepository.existsByEmailAndIdNot(req.getEmail(), id)) {
                throw new IllegalArgumentException("Email déjà utilisé : " + req.getEmail());
            }
        }

        shipper.setCompanyName(req.getCompanyName());
        shipper.setContactName(req.getContactName());
        shipper.setEmail(req.getEmail());
        shipper.setPhone(req.getPhone());
        shipper.setAddress(req.getAddress());
        shipper.setCity(req.getCity());
        shipper.setCountry(req.getCountryCode());

        return toResponse(shipperRepository.save(shipper));
    }

    @Override
    @Transactional
    public ShipperResponseDTO patch(Long id, ShipperRequestDTO req) {

        Shipper shipper = findOrThrow(id);

        if (req.getCompanyName() != null) shipper.setCompanyName(req.getCompanyName());
        if (req.getContactName() != null) shipper.setContactName(req.getContactName());
        if (req.getPhone()       != null) shipper.setPhone(req.getPhone());
        if (req.getAddress()     != null) shipper.setAddress(req.getAddress());
        if (req.getCity()        != null) shipper.setCity(req.getCity());
        if (req.getCountryCode() != null) shipper.setCountry(req.getCountryCode());

        if (req.getEmail() != null && !req.getEmail().equalsIgnoreCase(shipper.getEmail())) {
            if (shipperRepository.existsByEmailAndIdNot(req.getEmail(), id)) {
                throw new IllegalArgumentException("Email déjà utilisé : " + req.getEmail());
            }
            shipper.setEmail(req.getEmail());
        }

        return toResponse(shipperRepository.save(shipper));
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        Shipper shipper = findOrThrow(id);
        shipper.setActive(false);
        shipperRepository.save(shipper);
    }

    @Override
    @Transactional
    public void activate(Long id) {
        Shipper shipper = findOrThrow(id);
        shipper.setActive(true);
        shipperRepository.save(shipper);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        shipperRepository.delete(findOrThrow(id));
    }

    private Shipper findOrThrow(Long id) {
        return shipperRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Shipper introuvable : " + id));
    }

    private ShipperResponseDTO toResponse(Shipper s) {
        return ShipperResponseDTO.builder()
                .id(s.getId())
                .companyName(s.getCompanyName())
                .contactName(s.getContactName())
                .email(s.getEmail())
                .phone(s.getPhone())
                .address(s.getAddress())
                .city(s.getCity())
                .countryCode(s.getCountry())
                .active(s.isActive())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
