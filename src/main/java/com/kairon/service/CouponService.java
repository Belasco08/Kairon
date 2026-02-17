package com.kairon.service;

import com.kairon.domain.entity.Company;
import com.kairon.domain.entity.Coupon;
import com.kairon.domain.enums.PlanType;
import com.kairon.exception.BusinessException;
import com.kairon.repository.CompanyRepository;
import com.kairon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final CompanyRepository companyRepository;

    @Transactional
    public String applyCoupon(String companyId, String code) {
        // 1. Busca a empresa
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException("Empresa não encontrada"));

        // 2. Busca o cupom no banco (ignorando maiúsculas/minúsculas)
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new BusinessException("Cupom inválido ou inexistente."));

        // 3. Validações de segurança
        if (coupon.isUsed()) {
            throw new BusinessException("Este cupom já foi utilizado.");
        }
        if (coupon.getExpiresAt() != null && coupon.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Este cupom já expirou.");
        }

        // 4. Aplica o benefício (Meses Grátis)
        if (coupon.getFreeMonths() != null && coupon.getFreeMonths() > 0) {
            company.setPlan(PlanType.PLUS); // Muda pro plano pago

            // Se a empresa já tem plano ativo, soma os meses. Se não, conta a partir de hoje.
            LocalDateTime currentExpiration = company.getPlanExpiresAt() != null && company.getPlanExpiresAt().isAfter(LocalDateTime.now())
                    ? company.getPlanExpiresAt()
                    : LocalDateTime.now();

            company.setPlanExpiresAt(currentExpiration.plusMonths(coupon.getFreeMonths()));
            companyRepository.save(company);
        }

        // 5. "Queima" o cupom para ninguém usar de novo (a não ser que seja cupom de influencer)
        // Se for cupom de influencer (ex: EMY10), você pode pular essa parte ou checar um campo 'isReusable'
        coupon.setUsed(true);
        coupon.setUsedByCompany(company);
        couponRepository.save(coupon);

        return "Cupom aplicado com sucesso! Você ganhou " + coupon.getFreeMonths() + " mês(es) de Kairon Plus!";
    }
}