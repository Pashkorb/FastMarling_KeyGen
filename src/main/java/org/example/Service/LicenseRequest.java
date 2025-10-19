package org.example.Service;

import org.example.FeatureFlag;
import org.example.SoftwareVersion;

import java.time.LocalDate;
import java.util.List;

public record LicenseRequest(LocalDate expirationDate,
                             SoftwareVersion version,
                             List<FeatureFlag> features,
                             String company) {

    public LicenseRequest {
        if (expirationDate == null) {
            throw new IllegalArgumentException("expirationDate cannot be null");
        }
        if (version == null) {
            throw new IllegalArgumentException("version cannot be null");
        }
        List<FeatureFlag> normalizedFeatures = features == null ? List.of() : List.copyOf(features);
        String normalizedCompany = (company != null && !company.isBlank()) ? company : null;
        this.features = normalizedFeatures;
        this.company = normalizedCompany;
    }
}
