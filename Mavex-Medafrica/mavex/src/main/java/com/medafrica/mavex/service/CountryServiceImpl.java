package com.medafrica.mavex.service;

import com.medafrica.mavex.model.country.Country;
import com.medafrica.mavex.repository.CountryRepository;
import com.medafrica.mavex.service.interfaces.CountryService;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CountryServiceImpl implements CountryService {

    private final CountryRepository countryRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String API_URL =
            "https://restcountries.com/v3.1/all?fields=cca2,name";

    @Override
    public void importCountries() {

        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                API_URL,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        );

        List<Map<String, Object>> body = response.getBody();

        if (body == null) return;

        for (Map<String, Object> item : body) {

            try {
                String code = (String) item.get("cca2");

                Object nameObj = item.get("name");
                if (!(nameObj instanceof Map<?, ?> rawMap)) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> nameMap = (Map<String, Object>) rawMap;

                String countryName = (String) nameMap.get("common");

                if (code == null || countryName == null) continue;

                if (countryRepository.existsById(code)) continue;

                Country country = Country.builder()
                        .code(code)
                        .name(countryName)
                        .build();

                countryRepository.save(country);

            } catch (Exception e) {
                System.out.println("Error parsing country: " + e.getMessage());
            }
        }
    }

    @Override
    public List<Country> getAllCountries() {
        return countryRepository.findAll();
    }
}
