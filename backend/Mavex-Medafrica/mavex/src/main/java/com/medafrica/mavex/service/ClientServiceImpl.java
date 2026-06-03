package com.medafrica.mavex.service;

import com.medafrica.mavex.dto.client.ClientPatchRequest;
import com.medafrica.mavex.dto.client.ClientRequestDTO;
import com.medafrica.mavex.dto.client.ClientResponseDTO;
import com.medafrica.mavex.model.actor.Client;
import com.medafrica.mavex.model.country.Country;
import com.medafrica.mavex.repository.ClientRepository;
import com.medafrica.mavex.repository.CountryRepository;
import com.medafrica.mavex.service.interfaces.ClientService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final CountryRepository countryRepository;

    @Override
    public List<ClientResponseDTO> findAll() {
        return clientRepository.findAll()
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

        return toResponseDTO(clientRepository.save(client));
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

        return toResponseDTO(clientRepository.save(client));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!clientRepository.existsById(id)) {
            throw new EntityNotFoundException("Client introuvable avec l'id : " + id);
        }
        clientRepository.deleteById(id);
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
