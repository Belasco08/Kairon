package com.kairon.service;

import com.kairon.domain.entity.Company;
import com.kairon.domain.entity.Coupon;
import com.kairon.domain.enums.AppointmentStatus;
import com.kairon.domain.enums.FinancialType;
import com.kairon.repository.AppointmentRepository;
import com.kairon.repository.CompanyRepository;
import com.kairon.repository.CouponRepository;
import com.kairon.repository.FinancialRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MilestoneService {

    private final CompanyRepository companyRepository;
    private final AppointmentRepository appointmentRepository;
    private final FinancialRecordRepository financialRecordRepository;
    private final CouponRepository couponRepository; // 👈 Repositório de cupons injetado

    // O seu Webhook do Discord
    private final String DISCORD_WEBHOOK_URL = "https://discord.com/api/webhooks/1471888750261833850/tPZUBaoCKZEZc20xBv6ED8KPT_TcZzkzrjVRqxWBP2QkSJfKWwSUDwwjAlDX_toKHSJn";

    // As metas (Estilo Kiwify)
    private final int[] MILESTONES = {10000, 50000, 100000, 1000000};
    private final String[] MILESTONE_NAMES = {"Bronze (10k)", "Prata (50k)", "Ouro (100k)", "Black (1 Milhão)"};
    private final int[] MILESTONE_PRIZES = {1, 2, 3, 12}; // Quantos meses grátis cada meta ganha

    // Esse comando faz o método rodar TODOS OS DIAS ao MEIO-DIA
    @Scheduled(cron = "0 0 12 * * *")
    @Transactional
    public void checkAndNotifyMilestones() {
        log.info("🤖 Iniciando verificação de Metas (Plaquinhas e Cupons)...");

        List<Company> companies = companyRepository.findAll();

        for (Company company : companies) {
            // 1. Calcula faturamento TOTAL da vida da empresa
            BigDecimal totalAppointments = appointmentRepository
                    .findByCompanyIdAndStatus(company.getId(), AppointmentStatus.COMPLETED)
                    .stream()
                    .map(a -> a.getTotalPrice())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalIncomes = financialRecordRepository
                    .findByCompanyId(company.getId())
                    .stream()
                    .filter(r -> r.getType() == FinancialType.INCOME)
                    .map(r -> r.getAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal lifetimeRevenue = totalAppointments.add(totalIncomes);
            int currentRevenue = lifetimeRevenue.intValue();

            // 2. Verifica se bateu alguma meta nova
            int lastAchieved = company.getLastMilestoneAchieved() != null ? company.getLastMilestoneAchieved() : 0;

            for (int i = 0; i < MILESTONES.length; i++) {
                int milestoneGoal = MILESTONES[i];

                // Se o faturamento passou da meta E a empresa ainda não tinha recebido essa placa
                if (currentRevenue >= milestoneGoal && lastAchieved < milestoneGoal) {

                    String milestoneName = MILESTONE_NAMES[i];
                    int freeMonths = MILESTONE_PRIZES[i];

                    // 👇 GERA O CUPOM ÚNICO NO BANCO DE DADOS
                    String shortHash = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
                    // Exemplo: KRN-BRONZE-X8B2C
                    String promoCode = "KRN-" + milestoneName.split(" ")[0].toUpperCase() + "-" + shortHash;

                    Coupon newCoupon = Coupon.builder()
                            .code(promoCode)
                            .freeMonths(freeMonths)
                            .discountPercentage(0.0) // Não é desconto em %, é mês grátis inteiro
                            .isUsed(false)
                            .expiresAt(LocalDateTime.now().plusMonths(3)) // O cara tem 3 meses pra ativar
                            .build();

                    couponRepository.save(newCoupon);

                    // Envia o alerta para o Discord COM o código do cupom!
                    sendDiscordAlert(company, milestoneName, currentRevenue, promoCode, freeMonths);

                    // Salva que a empresa já pegou essa meta para não avisar de novo
                    company.setLastMilestoneAchieved(milestoneGoal);
                    companyRepository.save(company);
                }
            }
        }
        log.info("✅ Verificação de metas e cupons concluída!");
    }

    private void sendDiscordAlert(Company company, String milestoneName, int currentRevenue, String promoCode, int freeMonths) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            // Monta a mensagem bonitona pro Discord
            String message = String.format(
                    "🎉 **META BATIDA! HORA DE ENVIAR A PLACA!** 🎉\n" +
                            "💈 **Empresa:** %s\n" +
                            "🏆 **Placa:** %s\n" +
                            "💰 **Faturamento Total:** R$ %,d\n" +
                            "📱 **Telefone:** %s\n\n" +
                            "🎁 **PRESENTE GERADO PELO SISTEMA:**\n" +
                            "O sistema criou um cupom de **%d mês(es) grátis** do Kairon Plus!\n" +
                            "🎟️ **Cupom:** `%s`\n" +
                            "*(Anote esse código no cartão de parabéns e envie junto com a placa!)*",
                    company.getName(), milestoneName, currentRevenue, company.getPhone(), freeMonths, promoCode
            );

            Map<String, String> payload = new HashMap<>();
            payload.put("content", message);
            payload.put("username", "Kairon Metas");

            restTemplate.postForEntity(DISCORD_WEBHOOK_URL, payload, String.class);
            log.info("Notificação enviada para o Discord: Empresa {}", company.getName());

        } catch (Exception e) {
            log.error("Erro ao enviar webhook do Discord", e);
        }
    }
}