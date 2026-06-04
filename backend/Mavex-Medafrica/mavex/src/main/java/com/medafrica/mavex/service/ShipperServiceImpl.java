package com.medafrica.mavex.service;

import com.medafrica.mavex.dto.shipper.ShipperRequestDTO;
import com.medafrica.mavex.dto.shipper.ShipperResponseDTO;
import com.medafrica.mavex.model.actor.Shipper;
import com.medafrica.mavex.model.country.Country;
import com.medafrica.mavex.repository.CountryRepository;
import com.medafrica.mavex.repository.ShipperRepository;
import com.medafrica.mavex.service.interfaces.ShipperService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShipperServiceImpl implements ShipperService {

    private final ShipperRepository  shipperRepository;
    private final CountryRepository  countryRepository;

    @Override
    @Transactional
    public ShipperResponseDTO create(ShipperRequestDTO req) {

        if (shipperRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email déjà utilisé : " + req.getEmail());
        }

        Country country = resolveCountry(req.getCountryCode());

        Shipper shipper = Shipper.builder()
                .companyName(req.getCompanyName())
                .contactName(req.getContactName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .address(req.getAddress())
                .city(req.getCity())
                .country(country)
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
        shipper.setCountry(resolveCountry(req.getCountryCode()));

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
        if (req.getCountryCode() != null) shipper.setCountry(resolveCountry(req.getCountryCode()));

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

    @Override
    @Transactional
    public int bulkDelete(List<Long> ids) {
        List<Shipper> shippers = shipperRepository.findAllById(ids);
        shipperRepository.deleteAll(shippers);
        return shippers.size();
    }

    @Override
    @Transactional
    public int bulkActivate(List<Long> ids) {
        List<Shipper> shippers = shipperRepository.findAllById(ids);
        shippers.forEach(s -> s.setActive(true));
        shipperRepository.saveAll(shippers);
        return shippers.size();
    }

    @Override
    @Transactional
    public int bulkDeactivate(List<Long> ids) {
        List<Shipper> shippers = shipperRepository.findAllById(ids);
        shippers.forEach(s -> s.setActive(false));
        shipperRepository.saveAll(shippers);
        return shippers.size();
    }

    private Shipper findOrThrow(Long id) {
        return shipperRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Shipper introuvable : " + id));
    }

    private Country resolveCountry(String code) {
        if (code == null) return null;
        return countryRepository.findById(code)
                .orElseThrow(() -> new EntityNotFoundException("Pays introuvable : " + code));
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
