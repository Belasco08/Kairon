package com.kairon.service;

import com.kairon.domain.entity.Appointment;
import com.kairon.domain.entity.Client;
import com.kairon.domain.entity.Company;
import com.kairon.domain.enums.AppointmentStatus;
import com.kairon.dto.request.ClientRequest;
import com.kairon.dto.response.ClientListResponse;
import com.kairon.dto.response.ClientResponse;
import com.kairon.dto.response.MissingClientResponse; // 👈 NOVO IMPORT
import com.kairon.exception.BusinessException;
import com.kairon.mapper.ClientMapper;
import com.kairon.repository.ClientRepository;
import com.kairon.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit; // 👈 NOVO IMPORT
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final CompanyRepository companyRepository;
    private final ClientMapper clientMapper;
    private final BaseService baseService;

    @Transactional
    public ClientResponse createClient(String companyId, ClientRequest request) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException("Company not found"));

        clientRepository.findByPhoneAndCompanyId(request.getPhone(), companyId)
                .ifPresent(c -> {
                    throw new BusinessException("Client with this phone already exists");
                });

        Client client = clientMapper.toEntity(request);
        client.setId(UUID.randomUUID().toString());
        client.setCompany(company);

        return enrichClientResponse(clientRepository.save(client));
    }

    @Transactional(readOnly = true)
    public ClientResponse getClient(String companyId, String clientId) {
        Client client = clientRepository.findByIdAndCompanyId(clientId, companyId)
                .orElseThrow(() -> new BusinessException("Client not found"));

        baseService.validateResourceBelongsToCompany(client.getCompany().getId(), companyId);

        return enrichClientResponse(client);
    }

    @Transactional
    public ClientResponse updateClient(String companyId, String clientId, ClientRequest request) {
        Client client = clientRepository.findByIdAndCompanyId(clientId, companyId)
                .orElseThrow(() -> new BusinessException("Client not found"));

        baseService.validateResourceBelongsToCompany(client.getCompany().getId(), companyId);

        if (!client.getPhone().equals(request.getPhone())) {
            clientRepository.findByPhoneAndCompanyId(request.getPhone(), companyId)
                    .ifPresent(c -> {
                        if (!c.getId().equals(clientId)) {
                            throw new BusinessException("Another client already has this phone");
                        }
                    });
        }

        clientMapper.updateEntity(request, client);
        return enrichClientResponse(clientRepository.save(client));
    }

    @Transactional(readOnly = true)
    public Page<ClientListResponse> listClients(String companyId, String professionalId, String search, Pageable pageable) {
        baseService.validateCompanyAccess(companyId, companyId);

        Page<Client> clients;

        if (professionalId != null && !professionalId.isBlank()) {
            clients = (search != null && !search.isBlank())
                    ? clientRepository.searchByCompanyIdAndProfessionalId(companyId, professionalId, search.trim(), pageable)
                    : clientRepository.findByCompanyIdAndProfessionalId(companyId, professionalId, pageable);
        } else {
            clients = (search != null && !search.isBlank())
                    ? clientRepository.searchByCompanyId(companyId, search.trim(), pageable)
                    : clientRepository.findByCompanyId(companyId, pageable);
        }

        return clients.map(client -> {
            ClientListResponse response = clientMapper.toListResponse(client);
            enrichListResponse(response, client);
            return response;
        });
    }

    @Transactional(readOnly = true)
    public List<ClientListResponse> searchClients(String companyId, String searchTerm) {
        baseService.validateCompanyAccess(companyId, companyId);

        return clientRepository.searchByCompanyId(companyId, searchTerm)
                .stream()
                .map(client -> {
                    ClientListResponse response = clientMapper.toListResponse(client);
                    enrichListResponse(response, client);
                    return response;
                })
                .collect(Collectors.toList());
    }

    /* =========================================================
       👇 NOVO: SERVIÇO DE CLIENTES SUMIDOS (DINHEIRO NA MESA)
       ========================================================= */
    @Transactional(readOnly = true)
    public Page<MissingClientResponse> getMissingClients(String companyId, String professionalId, int daysAway, Pageable pageable) {
        baseService.validateCompanyAccess(companyId, companyId);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoffDate = now.minusDays(daysAway); // Data limite (ex: hoje menos 30 dias)
        List<AppointmentStatus> pendingStatuses = List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED);

        Page<Client> clients;
        if (professionalId != null && !professionalId.isBlank()) {
            clients = clientRepository.findMissingClientsByProfessional(companyId, professionalId, cutoffDate, now, AppointmentStatus.COMPLETED, pendingStatuses, pageable);
        } else {
            clients = clientRepository.findMissingClients(companyId, cutoffDate, now, AppointmentStatus.COMPLETED, pendingStatuses, pageable);
        }

        return clients.map(client -> {
            // Pega o último agendamento concluído para extrair a data exata e o serviço
            Appointment lastApp = getCompletedAppointments(client).stream()
                    .max(Comparator.comparing(Appointment::getStartTime))
                    .orElse(null);

            long daysSinceLastVisit = 0;
            String lastServiceName = "Serviço";

            if (lastApp != null) {
                daysSinceLastVisit = ChronoUnit.DAYS.between(lastApp.getStartTime(), now);
                // Pega o nome do primeiro serviço realizado naquele agendamento (se existir)
                if (lastApp.getAppointmentServices() != null && !lastApp.getAppointmentServices().isEmpty()) {
                    lastServiceName = lastApp.getAppointmentServices().iterator().next().getName();
                }
            }

            return MissingClientResponse.builder()
                    .id(client.getId())
                    .name(client.getName())
                    .phone(client.getPhone())
                    .daysAway(daysSinceLastVisit)
                    .lastService(lastServiceName)
                    .lastVisitDate(lastApp != null ? lastApp.getStartTime() : null)
                    .build();
        });
    }

    /* =========================
       HELPERS DE CÁLCULO
       ========================= */

    private ClientResponse enrichClientResponse(Client client) {
        ClientResponse response = clientMapper.toResponse(client);
        calculateStatsForResponse(client, response);
        return response;
    }

    private void enrichListResponse(ClientListResponse response, Client client) {
        List<Appointment> completedAppointments = getCompletedAppointments(client);

        response.setTotalAppointments(completedAppointments.size());

        double totalSpent = completedAppointments.stream()
                .mapToDouble(a -> a.getTotalPrice() != null ? a.getTotalPrice().doubleValue() : 0.0)
                .sum();
        response.setTotalSpent(totalSpent);

        LocalDateTime lastApp = completedAppointments.stream()
                .map(Appointment::getStartTime)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        if (lastApp != null) {
            response.setLastAppointmentDate(lastApp.toLocalDate());
        } else {
            response.setLastAppointmentDate(null);
        }
    }

    private void calculateStatsForResponse(Client client, ClientResponse response) {
        List<Appointment> completedAppointments = getCompletedAppointments(client);

        response.setTotalAppointments(completedAppointments.size());

        double totalSpent = completedAppointments.stream()
                .mapToDouble(a -> a.getTotalPrice() != null ? a.getTotalPrice().doubleValue() : 0.0)
                .sum();
        response.setTotalSpent(totalSpent);

        LocalDateTime lastApp = completedAppointments.stream()
                .map(Appointment::getStartTime)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        response.setLastAppointment(lastApp);
    }

    private List<Appointment> getCompletedAppointments(Client client) {
        if (client.getAppointments() == null) return List.of();

        return client.getAppointments().stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                .toList();
    }

    // 👇 MÁGICA DO RESGATE DE FIDELIDADE 👇
    @org.springframework.transaction.annotation.Transactional
    public void redeemFidelityReward(String companyId, String clientId) {
        Client client = clientRepository.findByIdAndCompanyId(clientId, companyId)
                .orElseThrow(() -> new BusinessException("Cliente não encontrado"));

        int currentStamps = client.getFidelityStamps() != null ? client.getFidelityStamps() : 0;

        if (currentStamps < 10) {
            throw new BusinessException("O cliente ainda não possui selos suficientes para o resgate.");
        }

        // Subtrai 10 selos (se ele tiver 11, sobra 1 para a próxima rodada)
        client.setFidelityStamps(currentStamps - 10);
        clientRepository.save(client);
    }
}