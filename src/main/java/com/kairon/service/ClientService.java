package com.kairon.service;

import com.kairon.domain.entity.Appointment; // Importe sua entidade Appointment
import com.kairon.domain.entity.Client;
import com.kairon.domain.entity.Company;
import com.kairon.domain.enums.AppointmentStatus; // Importe seu Enum de Status
import com.kairon.dto.request.ClientRequest;
import com.kairon.dto.response.ClientListResponse;
import com.kairon.dto.response.ClientResponse;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

        // LÓGICA DE FILTRO: Se tiver professionalId, usa as queries novas
        if (professionalId != null && !professionalId.isBlank()) {
            clients = (search != null && !search.isBlank())
                    ? clientRepository.searchByCompanyIdAndProfessionalId(companyId, professionalId, search.trim(), pageable)
                    : clientRepository.findByCompanyIdAndProfessionalId(companyId, professionalId, pageable);
        } else {
            // Comportamento padrão (Admin ou sem filtro)
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

    /* =========================
       HELPERS DE CÁLCULO
       ========================= */

    private ClientResponse enrichClientResponse(Client client) {
        ClientResponse response = clientMapper.toResponse(client);
        calculateStatsForResponse(client, response);
        return response;
    }

    private void enrichListResponse(ClientListResponse response, Client client) {
        // Busca apenas agendamentos CONCLUÍDOS para calcular valores reais
        List<Appointment> completedAppointments = getCompletedAppointments(client);

        response.setTotalAppointments(completedAppointments.size());

        // Calcula total gasto
        double totalSpent = completedAppointments.stream()
                .mapToDouble(a -> a.getTotalPrice() != null ? a.getTotalPrice().doubleValue() : 0.0)
                .sum();
        response.setTotalSpent(totalSpent);

        // Calcula Última Visita
        LocalDateTime lastApp = completedAppointments.stream()
                .map(Appointment::getStartTime) // Pega a data de início do agendamento
                .max(LocalDateTime::compareTo)  // Pega a mais recente
                .orElse(null);

        if (lastApp != null) {
            // Converte para LocalDate se seu DTO usa LocalDate
            response.setLastAppointmentDate(lastApp.toLocalDate());
        } else {
            response.setLastAppointmentDate(null);
        }
    }

    private void calculateStatsForResponse(Client client, ClientResponse response) {
        List<Appointment> completedAppointments = getCompletedAppointments(client);

        // 1. Total de Agendamentos Concluídos
        response.setTotalAppointments(completedAppointments.size());

        // 2. Total Gasto (Soma dos preços)
        double totalSpent = completedAppointments.stream()
                .mapToDouble(a -> a.getTotalPrice() != null ? a.getTotalPrice().doubleValue() : 0.0)
                .sum();
        response.setTotalSpent(totalSpent);

        // 3. Última Visita (Maior data)
        LocalDateTime lastApp = completedAppointments.stream()
                .map(Appointment::getStartTime)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        response.setLastAppointment(lastApp);
    }

    private List<Appointment> getCompletedAppointments(Client client) {
        if (client.getAppointments() == null) return List.of();

        return client.getAppointments().stream()
                // IMPORTANTE: Ajuste 'AppointmentStatus.COMPLETED' para o nome exato no seu Enum
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                .toList();
    }
}