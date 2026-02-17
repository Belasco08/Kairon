package com.kairon.component;

import com.kairon.domain.entity.Company;
import com.kairon.domain.enums.PlanType;
import com.kairon.exception.BusinessException;
import com.kairon.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlanGuard {

    private final CompanyRepository companyRepository;

    /**
     * Verifica se a empresa tem o plano PLUS.
     * Se não tiver, lança uma exceção que bloqueia a operação imediatamente.
     */
    public void checkPlusAccess(String companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException("Empresa não encontrada"));

        if (company.getPlan() != PlanType.PLUS) {
            throw new BusinessException("🔒 Funcionalidade exclusiva do plano PLUS. Faça o upgrade para liberar!");
        }
    }

    /**
     * Verifica se a empresa tem o plano FREE ou PLUS (útil se você criar um plano 'BASIC' no futuro).
     * Por enquanto, serve apenas como exemplo de expansão.
     */
    public boolean isPlus(String companyId) {
        return companyRepository.findById(companyId)
                .map(c -> c.getPlan() == PlanType.PLUS)
                .orElse(false);
    }
}