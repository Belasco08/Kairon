package com.kairon.service;

import com.kairon.domain.entity.Client;
import com.kairon.domain.entity.Company;
import com.kairon.repository.ClientRepository;
import com.kairon.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetentionService {

    private final CompanyRepository companyRepository;
    private final ClientRepository clientRepository;

    // Roda todo dia às 09:00 da manhã no horário do servidor
    @Scheduled(cron = "0 0 9 * * ?")
    @Transactional(readOnly = true)
    public void dailyRetentionScan() {
        log.info("🤖 Iniciando varredura da Máquina de Retenção...");

        // Pega as barbearias ativas (Se você tiver um plano PLUS, pode filtrar só quem paga aqui)
        List<Company> companies = companyRepository.findAll();

        // Data limite: 25 dias atrás
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(25);

        for (Company company : companies) {
            List<Client> sleepingClients = clientRepository.findClientsForRetention(company.getId(), cutoffDate);

            if (!sleepingClients.isEmpty()) {
                log.info("🎯 Encontrados {} clientes sumidos na barbearia {}", sleepingClients.size(), company.getName());

                // ==========================================================
                // FUTURO: Aqui você poderia chamar uma API do WhatsApp
                // (tipo Z-API ou Evolution API) para mandar a mensagem sozinho.
                // ==========================================================
            }
        }
        log.info("✅ Varredura de retenção finalizada!");
    }

    // 👇 ROTA PARA O APLICATIVO PUXAR A LISTA DE QUEM ELE DEVE CHAMAR HOJE 👇
    @Transactional(readOnly = true)
    public List<Client> getClientsToRecover(String companyId) {
        // Puxa quem não corta há mais de 25 dias
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(25);
        return clientRepository.findClientsForRetention(companyId, cutoffDate);
    }
}