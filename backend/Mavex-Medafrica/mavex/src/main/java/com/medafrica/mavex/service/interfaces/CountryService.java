package com.medafrica.mavex.service.interfaces;

import com.medafrica.mavex.model.country.Country;

import java.util.List;

public interface CountryService {

    void importCountries();

    List<Country> getAllCountries();
}
