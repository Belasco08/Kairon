package com.kairon.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kairon.component.PlanGuard;
import com.kairon.domain.entity.Company;
import com.kairon.domain.entity.Professional;
import com.kairon.domain.entity.Services;
import com.kairon.domain.entity.User;
import com.kairon.domain.enums.PlanType;
import com.kairon.domain.enums.Role;
import com.kairon.dto.request.AssignUserToProfessionalRequest;
import com.kairon.dto.request.ProfessionalRequest;
import com.kairon.dto.request.ProfessionalUpdateRequest;
import com.kairon.dto.response.ProfessionalResponse;
import com.kairon.dto.response.ProfessionalWithServicesResponse;
import com.kairon.dto.response.ServiceResponse;
import com.kairon.exception.BusinessException;
import com.kairon.repository.CompanyRepository;
import com.kairon.repository.ProfessionalRepository;
import com.kairon.repository.ServiceRepository;
import com.kairon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfessionalService {

    private final ProfessionalRepository professionalRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
    private final PlanGuard planGuard;

    // ===================================================================================
    // 1. CREATE
    // ===================================================================================
    @Transactional
    public ProfessionalResponse createProfessional(String companyId, ProfessionalRequest request) {

        // 1. Validação de Empresa e Plano
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException("Company not found"));

        if (company.getPlan() == PlanType.FREE) {
            long currentPros = professionalRepository.countByCompanyId(companyId);
            if (currentPros >= 1) {
                throw new BusinessException("No plano Grátis, você só pode ter 1 profissional. Faça o upgrade para adicionar sua equipe!");
            }
        }

        // 2. Validações de Duplicidade
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BusinessException("Este e-mail já está registrado no sistema.");
        }

        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            boolean exists = professionalRepository.findByCompanyId(companyId).stream()
                    .anyMatch(p -> request.getPhone() != null && request.getPhone().equals(p.getPhone()));
            if (exists) {
                throw new BusinessException("Este telefone já está registrado para outro profissional desta empresa.");
            }
        }

        // 3. Criação do Usuário (Login)
        User newUser = new User();
        newUser.setName(request.getName());
        newUser.setEmail(request.getEmail());
        newUser.setPhone(request.getPhone());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setCompany(company);
        newUser.setActive(true);
        newUser.setRole("OWNER".equalsIgnoreCase(request.getRole()) ? Role.OWNER : Role.PROFESSIONAL);

        newUser = userRepository.save(newUser);

        // 4. Criação do Profissional
        Professional professional = Professional.builder()
                .name(request.getName())
                .phone(request.getPhone())
                .email(request.getEmail()) // Agora mapeado corretamente
                .description(request.getDescription())
                .photoUrl(request.getPhotoUrl())
                .company(company)
                .isActive(true)
                .user(newUser)
                .commissionPercentage(
                        request.getCommissionPercentage() != null
                                ? BigDecimal.valueOf(request.getCommissionPercentage())
                                : BigDecimal.ZERO
                )
                .build();

        professional = professionalRepository.save(professional);

        // 5. Vínculo Bidirecional
        newUser.setProfessional(professional);
        userRepository.save(newUser);

        return mapToResponse(professional);
    }

    // ===================================================================================
    // 2. UPDATE
    // ===================================================================================
    @Transactional
    public ProfessionalResponse updateProfessional(
            String companyId,
            String professionalId,
            ProfessionalUpdateRequest request
    ) {
        log.info("Updating professional: {} for company: {}", professionalId, companyId);

        Professional professional = professionalRepository
                .findByIdAndCompanyId(professionalId, companyId)
                .orElseThrow(() -> new BusinessException("Professional not found"));

        // --- 1. VALIDAÇÃO E ATUALIZAÇÃO DE TELEFONE ---
        if (request.getPhone() != null && !request.getPhone().equals(professional.getPhone())) {
            // Verifica se outro profissional NA MESMA EMPRESA já usa esse telefone
            boolean exists = professionalRepository.findByCompanyId(companyId).stream()
                    .anyMatch(p -> !p.getId().equals(professionalId) && request.getPhone().equals(p.getPhone()));

            if (exists) throw new BusinessException("Telefone já utilizado por outro profissional.");

            professional.setPhone(request.getPhone());
        }

        // --- 2. VALIDAÇÃO E ATUALIZAÇÃO DE EMAIL (LOGIN) ---
        // Se o email foi enviado E ele é diferente do atual
        if (request.getEmail() != null && !request.getEmail().equals(professional.getEmail())) {
            // Verifica se o email já existe em QUALQUER usuário do sistema
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new BusinessException("Este e-mail já está em uso por outro usuário.");
            }
            professional.setEmail(request.getEmail());
        }

        // --- 3. ATUALIZAÇÃO DE CAMPOS SIMPLES ---
        if (request.getName() != null) professional.setName(request.getName());
        if (request.getDescription() != null) professional.setDescription(request.getDescription());
        if (request.getPhotoUrl() != null) professional.setPhotoUrl(request.getPhotoUrl());
        if (request.getWorkHours() != null) professional.setWorkHours(request.getWorkHours());
        if (request.getDaysOff() != null) professional.setDaysOff(request.getDaysOff());

        // Comissão
        if (request.getCommissionPercentage() != null) {
            double commission = request.getCommissionPercentage();
            if (commission < 0 || commission > 100) {
                throw new BusinessException("A comissão deve ser entre 0% e 100%");
            }
            professional.setCommissionPercentage(BigDecimal.valueOf(commission));
        }

        // Status Ativo/Inativo
        if (request.getIsActive() != null) {
            professional.setActive(request.getIsActive());
        }

        // --- 4. SINCRONIZAÇÃO COM USUÁRIO (CRÍTICO) ---
        // Atualiza a tabela Users para garantir que o Login continue funcionando
        if (professional.getUser() != null) {
            User linkedUser = professional.getUser();
            boolean userChanged = false;

            // Sincroniza Nome
            if (request.getName() != null && !request.getName().equals(linkedUser.getName())) {
                linkedUser.setName(request.getName());
                userChanged = true;
            }

            // Sincroniza Email (já validado acima)
            if (request.getEmail() != null && !request.getEmail().equals(linkedUser.getEmail())) {
                linkedUser.setEmail(request.getEmail());
                userChanged = true;
            }

            // Sincroniza Telefone
            if (request.getPhone() != null && !request.getPhone().equals(linkedUser.getPhone())) {
                linkedUser.setPhone(request.getPhone());
                userChanged = true;
            }

            // Sincroniza Status
            if (request.getIsActive() != null && request.getIsActive() != linkedUser.isActive()) {
                linkedUser.setActive(request.getIsActive());
                userChanged = true;
            }

            // Sincroniza Senha
            if (request.getPassword() != null && !request.getPassword().isEmpty()) {
                linkedUser.setPassword(passwordEncoder.encode(request.getPassword()));
                userChanged = true;
            }

            if (userChanged) {
                userRepository.save(linkedUser);
                log.info("User synced for professional: {}", professional.getName());
            }
        }

        // Salva o Profissional
        professional = professionalRepository.save(professional);
        log.info("Professional updated successfully: {}", professionalId);

        return mapToResponse(professional);
    }

    // ===================================================================================
// 3. DELETE (Soft Delete / Exclusão Lógica)
// ===================================================================================
    @Transactional
    public void deleteProfessional(String companyId, String professionalId) {
        log.info("Iniciando Soft Delete para o profissional: {} da empresa: {}", professionalId, companyId);

        Professional professional = professionalRepository
                .findByIdAndCompanyId(professionalId, companyId)
                .orElseThrow(() -> new BusinessException("Profissional não encontrado"));

        // 1. Em vez de deletar ou barrar, nós apenas INATIVAMOS o profissional.
        // Isso faz ele sumir do aplicativo (se as suas buscas filtrarem por ativos),
        // mas mantém o histórico financeiro e os agendamentos antigos intactos!
        professional.setActive(false);

        // 2. Desativa o acesso do Usuário (para ele não conseguir mais fazer login no Kairon)
        if (professional.getUser() != null) {
            User user = professional.getUser();
            user.setActive(false);
            // Nota: Assumindo que sua entidade User tem o método setIsActive().
        }

        // 3. Salva a alteração
        professionalRepository.save(professional);

        log.info("Profissional inativado (Soft Delete) com sucesso: {}", professionalId);
    }

    // ===================================================================================
    // 4. READ (GETs)
    // ===================================================================================

    public ProfessionalResponse getProfessional(String companyId, String professionalId) {
        Professional professional = professionalRepository
                .findByIdAndCompanyId(professionalId, companyId)
                .orElseThrow(() -> new BusinessException("Professional not found"));
        return mapToResponse(professional);
    }

    public List<ProfessionalResponse> getAllProfessionals(String companyId, Boolean activeOnly) {
        List<Professional> professionals;
        if (activeOnly != null && activeOnly) {
            professionals = professionalRepository.findByCompanyIdAndIsActive(companyId, true);
        } else {
            professionals = professionalRepository.findByCompanyId(companyId);
        }
        return professionals.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // ===================================================================================
    // 5. SERVICES & RELATIONS (Métodos que faltavam)
    // ===================================================================================

    @Transactional
    public ProfessionalResponse assignUserToProfessional(String companyId, String professionalId, AssignUserToProfessionalRequest request) {
        Professional professional = professionalRepository.findByIdAndCompanyId(professionalId, companyId)
                .orElseThrow(() -> new BusinessException("Professional not found"));
        User user = userRepository.findByIdAndCompanyId(request.getUserId(), companyId)
                .orElseThrow(() -> new BusinessException("User not found in company"));

        professionalRepository.findByUserId(request.getUserId()).ifPresent(p -> {
            if (!p.getId().equals(professionalId)) {
                throw new BusinessException("Usuário já está vinculado a outro profissional.");
            }
        });

        if (user.getRole() != Role.PROFESSIONAL && user.getRole() != Role.OWNER) {
            throw new BusinessException("O usuário deve ter o perfil PROFISSIONAL ou OWNER.");
        }
        if (!professional.isActive()) {
            throw new BusinessException("Não é possível vincular usuário a profissional inativo.");
        }

        professional.setUser(user);
        user.setProfessional(professional);
        professional = professionalRepository.save(professional);
        userRepository.save(user);

        return mapToResponse(professional);
    }

    @Transactional
    public ProfessionalResponse unassignUserFromProfessional(String companyId, String professionalId) {
        Professional professional = professionalRepository.findByIdAndCompanyId(professionalId, companyId)
                .orElseThrow(() -> new BusinessException("Professional not found"));

        if (professional.getUser() == null) {
            throw new BusinessException("Profissional não possui usuário vinculado.");
        }

        User user = professional.getUser();
        professional.setUser(null);
        user.setProfessional(null);
        professional = professionalRepository.save(professional);
        userRepository.save(user);

        return mapToResponse(professional);
    }

    public List<ServiceResponse> getProfessionalServices(String companyId, String professionalId, Boolean activeOnly) {
        professionalRepository.findByIdAndCompanyId(professionalId, companyId)
                .orElseThrow(() -> new BusinessException("Professional not found"));

        Boolean statusFilter = (activeOnly != null && activeOnly) ? true : null;

        Page<Services> servicesPage = serviceRepository.searchServices(
                companyId, null, statusFilter, null, professionalId, Pageable.unpaged()
        );

        return servicesPage.getContent().stream().map(this::mapToServiceResponse).collect(Collectors.toList());
    }

    public ProfessionalWithServicesResponse getProfessionalWithServices(String companyId, String professionalId) {
        Professional professional = professionalRepository.findByIdAndCompanyId(professionalId, companyId)
                .orElseThrow(() -> new BusinessException("Professional not found"));

        List<ServiceResponse> services = professional.getServices().stream()
                .map(this::mapToServiceResponse)
                .collect(Collectors.toList());

        return ProfessionalWithServicesResponse.builder()
                .id(professional.getId())
                .name(professional.getName())
                .description(professional.getDescription())
                .phone(professional.getPhone())
                .email(professional.getEmail())
                .photoUrl(professional.getPhotoUrl())
                .isActive(professional.isActive())
                .services(services)
                .createdAt(professional.getCreatedAt())
                .build();
    }

    // ===================================================================================
    // 6. MAPPERS
    // ===================================================================================

    private ProfessionalResponse mapToResponse(Professional professional) {
        return ProfessionalResponse.builder()
                .id(professional.getId())
                .name(professional.getName())
                .description(professional.getDescription())
                .phone(professional.getPhone())
                .email(professional.getEmail()) // Mapeando o novo campo
                .photoUrl(professional.getPhotoUrl())
                .isActive(professional.isActive())
                .workHours(professional.getWorkHours())
                .daysOff(professional.getDaysOff())
                .userId(professional.getUser() != null ? professional.getUser().getId() : null)
                .commissionPercentage(
                        professional.getCommissionPercentage() != null
                                ? professional.getCommissionPercentage().doubleValue()
                                : 0.0
                )
                .createdAt(professional.getCreatedAt())
                .updatedAt(professional.getUpdatedAt())
                .build();
    }

    private ServiceResponse mapToServiceResponse(Services service) {
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
                .build();
    }
}