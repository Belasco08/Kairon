package com.kairon.service;

import com.kairon.domain.entity.Company;
import com.kairon.dto.request.CompanySettingsRequest;
import com.kairon.dto.response.CompanySettingsResponse;
import com.kairon.exception.EntityNotFoundException;
import com.kairon.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanySettingsService {

    private final CompanyRepository companyRepository;

    @Transactional(readOnly = true)
    public CompanySettingsResponse getCompanySettings(String companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new EntityNotFoundException("Company not found"));

        return CompanySettingsResponse.builder()
                .companyId(company.getId())
                .name(company.getName())
                .slug(company.getSlug())
                .businessType(company.getBusinessType())
                .isActive(company.isActive())
                .isVerified(company.isVerified())
                .timezone(company.getTimezone())

                // ✅ Correção: getBusinessHours() retorna Map<String, Object>
                .businessHours(company.getBusinessHours())

                .slotDuration(company.getSlotDuration())
                .bufferTime(company.getBufferTime())
                .currency(company.getCurrency())
                .logoUrl(company.getLogoUrl()) // ou getLogo() se sua entidade usar esse nome
                .website(company.getWebsite())
                .createdAt(company.getCreatedAt())
                .updatedAt(company.getUpdatedAt())
                .build();
    }

    @Transactional
    public CompanySettingsResponse updateCompanySettings(String companyId, CompanySettingsRequest request, String userCompanyId) {
        if (!companyId.equals(userCompanyId)) {
            throw new RuntimeException("Access denied");
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new EntityNotFoundException("Company not found"));

        // Atualização dos campos básicos (se não nulos)
        if (request.getName() != null) company.setName(request.getName());
        if (request.getBusinessType() != null) company.setBusinessType(request.getBusinessType());
        if (request.getWebsite() != null) company.setWebsite(request.getWebsite());
        if (request.getLogoUrl() != null) company.setLogoUrl(request.getLogoUrl());

        // ✅ Correção: Atualiza businessHours diretamente com o Map
        if (request.getBusinessHours() != null) {
            company.setBusinessHours(request.getBusinessHours());
        }

        if (request.getSlotDuration() != null) {
            company.setSlotDuration(request.getSlotDuration());
        }

        if (request.getBufferTime() != null) {
            company.setBufferTime(request.getBufferTime());
        }

        if (request.getCurrency() != null) {
            company.setCurrency(request.getCurrency());
        }

        Company savedCompany = companyRepository.save(company);

        return getCompanySettings(savedCompany.getId()); // Reutiliza o método de mapeamento acima
    }
}