package com.medafrica.mavex.service;

import com.medafrica.mavex.model.country.Country;
import com.medafrica.mavex.repository.CountryRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CountryService {

    private final CountryRepository countryRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String API_URL =
            "https://restcountries.com/v3.1/all?fields=cca2,name";

    /**
     * Import countries from external API into database
     */
    public void importCountries() {

        ResponseEntity<List> response =
                restTemplate.getForEntity(API_URL, List.class);

        List<Map<String, Object>> body = response.getBody();

        if (body == null) return;

        for (Map<String, Object> item : body) {

            try {
                // country code (US, MA, FR...)
                String code = (String) item.get("cca2");

                // name object
                Map<String, Object> nameMap =
                        (Map<String, Object>) item.get("name");

                String countryName = (String) nameMap.get("common");

                if (code == null || countryName == null) continue;

                // avoid duplicates
                if (countryRepository.existsById(code)) continue;

                Country country = Country.builder()
                        .code(code)
                        .name(countryName)
                        .build();

                countryRepository.save(country);

            } catch (Exception e) {
                // skip corrupted data but continue import
                System.out.println("Error parsing country: " + e.getMessage());
            }
        }
    }

    /**
     * Get all countries from DB
     */
    public List<Country> getAllCountries() {
        return countryRepository.findAll();
    }
}