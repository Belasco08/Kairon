package com.kairon.service;

import com.kairon.domain.entity.Company;
import com.kairon.domain.entity.Professional;
import com.kairon.domain.entity.Services;
import com.kairon.domain.entity.User;
import com.kairon.dto.request.ServiceRequest;
import com.kairon.dto.request.ServiceUpdateRequest;
import com.kairon.dto.response.ServiceListResponse;
import com.kairon.dto.response.ServiceResponse;
import com.kairon.exception.BusinessException;
import com.kairon.repository.ProfessionalRepository;
import com.kairon.repository.ServiceRepository;
import com.kairon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceService extends BaseService {

    private final ServiceRepository serviceRepository;
    private final ProfessionalRepository professionalRepository;
    private final UserRepository userRepository;

    private Company getCurrentCompany() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        String email = principal instanceof UserDetails
                ? ((UserDetails) principal).getUsername()
                : principal.toString();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found"));

        return user.getCompany();
    }

    // =========================
    // CREATE
    // =========================
    @Transactional
    public ServiceResponse createService(String companyId, ServiceRequest request) {
        validateCompanyAccess(companyId, getCurrentUserCompanyId());

        Professional professional = null;
        if (request.getProfessionalId() != null && !request.getProfessionalId().isBlank()) {
            professional = professionalRepository
                    .findByIdAndCompanyId(request.getProfessionalId(), companyId)
                    .orElseThrow(() -> new BusinessException("Professional not found in your company"));

            if (!professional.isActive()) {
                throw new BusinessException("Professional is not active");
            }
        }

        Services service = Services.builder()
                // ❌ NÃO setar ID
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .duration(request.getDuration())
                .color(request.getColor() != null ? request.getColor() : "#3B82F6")
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .onlineBooking(request.getOnlineBooking() != null ? request.getOnlineBooking() : true)
                .category(request.getCategory())
                .professional(professional)
                .company(getCurrentCompany()) // ✅ OBRIGATÓRIO
                .build();

        serviceRepository.save(service);
        return mapToResponse(service);
    }



    // =========================
    // LIST (ADMIN) - REFATORADO
    // =========================
    @Transactional(readOnly = true)
    public ServiceListResponse getAllServices(
            String companyId,
            Pageable pageable,
            String category,
            Boolean isActive,
            Boolean onlineBooking,
            String professionalId
    ) {
        // Validação de segurança
        validateCompanyAccess(companyId, getCurrentUserCompanyId());

        // Validação do Profissional (se filtro for passado)
        if (professionalId != null && !professionalId.isBlank()) {
            // Dica: Use existsBy para ser mais performático que buscar o objeto inteiro
            professionalRepository.findByIdAndCompanyId(professionalId, companyId)
                    .orElseThrow(() -> new BusinessException("Professional not found in your company"));
        }
        // 👇 ADICIONE ESTES LOGS DE DEBUG AQUI 👇
        System.out.println("========================================");
        System.out.println("🔍 DEBUG SERVICE SEARCH:");
        System.out.println("🏢 Company ID do Usuário (Token): " + companyId);
        System.out.println("🎭 Filtro Active: " + isActive);
        System.out.println("📅 Filtro Online: " + onlineBooking);
        System.out.println("========================================");

        // ✅ CHAMADA ÚNICA: O Repository cuida da lógica complexa de filtros
        Page<Services> servicesPage = serviceRepository.searchServices(
                companyId,
                category,
                isActive,
                onlineBooking,
                professionalId,
                pageable
        );

        // Mapeamento para DTO
        List<ServiceResponse> services = servicesPage.getContent()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ServiceListResponse.builder()
                .services(services)
                .total((int) servicesPage.getTotalElements())
                .page(servicesPage.getNumber())
                .size(servicesPage.getSize())
                .build();
    }

    // =========================
    // LIST (PUBLIC)
    // =========================
    @Transactional(readOnly = true)
    public List<ServiceResponse> getServicesForPublic(String companyId, String professionalId) {
        List<Services> services;

        if (professionalId != null && !professionalId.isBlank()) {
            services = serviceRepository.findAvailableByProfessionalId(companyId, professionalId);
        } else {
            services = serviceRepository.findActiveAndOnlineByCompanyId(companyId);
        }

        return services.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // =========================
    // GET BY ID
    // =========================
    @Transactional(readOnly = true)
    public ServiceResponse getServiceById(String companyId, String serviceId) {
        validateCompanyAccess(companyId, getCurrentUserCompanyId());

        Services service = serviceRepository.findByIdAndCompanyId(serviceId, companyId)
                .orElseThrow(() -> new BusinessException("Service not found"));

        return mapToResponse(service);
    }

    // =========================
    // UPDATE
    // =========================
    @Transactional
    public ServiceResponse updateService(String companyId, String serviceId, ServiceUpdateRequest request) {
        validateCompanyAccess(companyId, getCurrentUserCompanyId());

        Services service = serviceRepository.findByIdAndCompanyId(serviceId, companyId)
                .orElseThrow(() -> new BusinessException("Service not found"));

        if (request.getName() != null) service.setName(request.getName());
        if (request.getDescription() != null) service.setDescription(request.getDescription());
        if (request.getPrice() != null) service.setPrice(request.getPrice());
        if (request.getDuration() != null) service.setDuration(request.getDuration());
        if (request.getColor() != null) service.setColor(request.getColor());
        if (request.getIsActive() != null) service.setActive(request.getIsActive());
        if (request.getOnlineBooking() != null) service.setOnlineBooking(request.getOnlineBooking());
        if (request.getCategory() != null) service.setCategory(request.getCategory());

        if (request.getProfessionalId() != null) {
            if (request.getProfessionalId().isBlank()) {
                service.setProfessional(null);
            } else {
                Professional professional = professionalRepository
                        .findByIdAndCompanyId(request.getProfessionalId(), companyId)
                        .orElseThrow(() -> new BusinessException("Professional not found in your company"));

                if (!professional.isActive()) {
                    throw new BusinessException("Cannot assign service to inactive professional");
                }

                service.setProfessional(professional);
            }
        }

        serviceRepository.save(service);
        return mapToResponse(service);
    }

    // =========================
    // DELETE (SOFT)
    // =========================
    @Transactional
    public void deleteService(String companyId, String serviceId) {
        validateCompanyAccess(companyId, getCurrentUserCompanyId());

        Services service = serviceRepository.findByIdAndCompanyId(serviceId, companyId)
                .orElseThrow(() -> new BusinessException("Service not found"));

        service.setActive(false);
        service.setOnlineBooking(false);

        serviceRepository.save(service);
    }

    // =========================
    // TOGGLES
    // =========================
    @Transactional
    public ServiceResponse toggleServiceStatus(String companyId, String serviceId, boolean active) {
        validateCompanyAccess(companyId, getCurrentUserCompanyId());

        Services service = serviceRepository.findByIdAndCompanyId(serviceId, companyId)
                .orElseThrow(() -> new BusinessException("Service not found"));

        service.setActive(active);
        if (!active) service.setOnlineBooking(false);

        serviceRepository.save(service);
        return mapToResponse(service);
    }

    @Transactional
    public ServiceResponse toggleOnlineBooking(String companyId, String serviceId, boolean onlineBooking) {
        validateCompanyAccess(companyId, getCurrentUserCompanyId());

        Services service = serviceRepository.findByIdAndCompanyId(serviceId, companyId)
                .orElseThrow(() -> new BusinessException("Service not found"));

        if (!service.isActive() && onlineBooking) {
            throw new BusinessException("Cannot enable online booking for inactive service");
        }

        service.setOnlineBooking(onlineBooking);
        serviceRepository.save(service);
        return mapToResponse(service);
    }

    // =========================
    // PROFESSIONAL
    // =========================
    @Transactional
    public ServiceResponse assignToProfessional(String companyId, String serviceId, String professionalId) {
        validateCompanyAccess(companyId, getCurrentUserCompanyId());

        Services service = serviceRepository.findByIdAndCompanyId(serviceId, companyId)
                .orElseThrow(() -> new BusinessException("Service not found"));

        Professional professional = professionalRepository
                .findByIdAndCompanyId(professionalId, companyId)
                .orElseThrow(() -> new BusinessException("Professional not found in your company"));

        if (!professional.isActive()) {
            throw new BusinessException("Cannot assign service to inactive professional");
        }

        service.setProfessional(professional);
        serviceRepository.save(service);
        return mapToResponse(service);
    }

    @Transactional
    public ServiceResponse unassignFromProfessional(String companyId, String serviceId) {
        validateCompanyAccess(companyId, getCurrentUserCompanyId());

        Services service = serviceRepository.findByIdAndCompanyId(serviceId, companyId)
                .orElseThrow(() -> new BusinessException("Service not found"));

        service.setProfessional(null);
        serviceRepository.save(service);
        return mapToResponse(service);
    }

    // =========================
    // HELPERS
    // =========================
    private String getCurrentUserCompanyId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        String email = principal instanceof UserDetails
                ? ((UserDetails) principal).getUsername()
                : principal.toString();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found"));

        return user.getCompany().getId();
    }

    private ServiceResponse mapToResponse(Services service) {
        return ServiceResponse.builder()
                .id(service.getId())
                .name(service.getName())
                .description(service.getDescription())
                .price(service.getPrice())
                .duration(service.getDuration())
                .color(service.getColor())
                .isActive(service.isActive())
                .onlineBooking(service.isOnlineBooking())
                .category(service.getCategory())
                .professionalId(service.getProfessional() != null ? service.getProfessional().getId() : null)
                .professionalName(service.getProfessional() != null ? service.getProfessional().getName() : null)
                .createdAt(service.getCreatedAt())
                .updatedAt(service.getUpdatedAt())
                .build();
    }
}
