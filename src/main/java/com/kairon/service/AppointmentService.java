package com.kairon.service;

import com.kairon.domain.entity.*;
import com.kairon.domain.enums.AppointmentStatus;
import com.kairon.domain.enums.FinancialType;
import com.kairon.domain.enums.PlanType;
import com.kairon.domain.enums.Role;
import com.kairon.dto.request.*;
import com.kairon.dto.response.*;
import com.kairon.exception.BusinessException;
import com.kairon.repository.*;
import com.kairon.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final ProfessionalRepository professionalRepository;
    private final ServiceRepository serviceRepository;
    private final ClientRepository clientRepository;
    private final AppointmentItemRepository appointmentItemRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final FinancialRecordRepository financialRecordRepository;

    /* ================= CREATE ================= */

    @Transactional
    public AppointmentResponse createAppointment(String companyId, AppointmentRequest request) {
        String currentUserId = null;
        try { currentUserId = SecurityUtils.getCurrentUserId(); } catch (Exception e) { /* Ignora */ }

        String professionalIdToUse = request.getProfessionalId();
        if (currentUserId != null) {
            User currentUser = userRepository.findById(currentUserId).orElse(null);
            if (currentUser != null && currentUser.getRole() == Role.PROFESSIONAL) {
                Professional selfProfile = professionalRepository.findByUserId(currentUserId)
                        .orElseThrow(() -> new BusinessException("Professional profile not found"));
                professionalIdToUse = selfProfile.getId();
            }
        }

        if (professionalIdToUse == null) throw new BusinessException("Professional ID is required");

        Professional professional = professionalRepository.findByIdAndCompanyId(professionalIdToUse, companyId)
                .orElseThrow(() -> new BusinessException("Professional not found"));

        List<Services> services = serviceRepository.findAllById(request.getServiceIds());
        if (services.isEmpty()) throw new BusinessException("At least one service must be selected");

        BigDecimal totalPrice = services.stream().map(Services::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalDuration = services.stream().mapToInt(Services::getDuration).sum();

        LocalDateTime start = request.getStartTime();
        LocalDateTime end = start.plusMinutes(totalDuration);

        if (appointmentRepository.existsByProfessionalAndDateOverlap(professional.getId(), start, end)) {
            throw new BusinessException("Este horário já foi preenchido.");
        }

        Client client = clientRepository.findByPhoneAndCompanyId(request.getClientPhone(), companyId)
                .orElseGet(() -> createClient(companyId, request));

        Appointment appointment = Appointment.builder()
                .startTime(start).endTime(end).status(AppointmentStatus.PENDING).totalPrice(totalPrice)
                .company(professional.getCompany()).professional(professional).client(client).timezone(companyId).build();

        appointmentRepository.save(appointment);

        appointmentItemRepository.saveAll(services.stream().map(s -> AppointmentItem.builder()
                .appointment(appointment).service(s).price(s.getPrice()).duration(s.getDuration()).build()).toList());

        return buildResponse(appointment);
    }

    /* ================= GET ================= */

    @Transactional(readOnly = true)
    public AppointmentResponse getAppointment(String companyId, String appointmentId) {
        return appointmentRepository.findByIdAndCompanyId(appointmentId, companyId)
                .map(this::buildResponse).orElseThrow(() -> new BusinessException("Appointment not found"));
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> listAppointments(String companyId, LocalDate date, String filterProfessionalId) {
        if (date == null) date = LocalDate.now();
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        String currentUserId = SecurityUtils.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId).orElseThrow();

        if (currentUser.getRole() == Role.PROFESSIONAL) {
            Professional prof = professionalRepository.findByUserId(currentUserId).orElseThrow();
            return appointmentRepository.findByCompanyAndProfessionalAndDateRange(companyId, prof.getId(), startOfDay, endOfDay)
                    .stream().map(this::buildResponse).collect(Collectors.toList());
        } else {
            if (filterProfessionalId != null && !filterProfessionalId.isEmpty()) {
                return appointmentRepository.findByCompanyAndProfessionalAndDateRange(companyId, filterProfessionalId, startOfDay, endOfDay)
                        .stream().map(this::buildResponse).collect(Collectors.toList());
            } else {
                return appointmentRepository.findByCompanyAndDateRange(companyId, startOfDay, endOfDay)
                        .stream().map(this::buildResponse).collect(Collectors.toList());
            }
        }
    }

    public List<AppointmentResponse> getAppointmentsByProfessional(String companyId, String professionalId) {
        return appointmentRepository.findByCompanyIdAndProfessionalId(companyId, professionalId).stream().map(this::buildResponse).toList();
    }


    // 👇 ADICIONE ESSA LINHA PARA MANTER A SESSÃO ABERTA PARA LER OS SERVIÇOS
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentsByClient(String companyId, String clientId) {
        return appointmentRepository.findByCompanyIdAndClientId(companyId, clientId).stream().map(this::buildResponse).toList();
    }


    public List<AppointmentResponse> getAppointmentsByDate(String companyId, LocalDate date) {
        return appointmentRepository.findByCompanyAndDateRange(companyId, date.atStartOfDay(), date.plusDays(1).atStartOfDay()).stream().map(this::buildResponse).toList();
    }

    /* ================= UPDATE ================= */

    @Transactional
    public AppointmentResponse updateAppointmentStatus(String companyId, String appointmentId, AppointmentStatusRequest request) {
        Appointment appointment = appointmentRepository.findByIdAndCompanyId(appointmentId, companyId)
                .orElseThrow(() -> new BusinessException("Agendamento não encontrado"));

        AppointmentStatus newStatus;
        try { newStatus = AppointmentStatus.valueOf(request.getStatus().toString()); }
        catch (Exception e) { throw new BusinessException("Status inválido: " + request.getStatus()); }

        if (appointment.getStatus() == newStatus) return buildResponse(appointment);

        appointment.setStatus(newStatus);
        if (request.getReason() != null) appointment.setCancellationReason(request.getReason());

        if (newStatus == AppointmentStatus.COMPLETED) {
            processFinancialSplit(appointment);
        }

        return buildResponse(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentResponse updateAppointmentServices(String companyId, String appointmentId, AppointmentUpdateRequest request) {
        Appointment appointment = appointmentRepository.findByIdAndCompanyId(appointmentId, companyId).orElseThrow();
        appointmentItemRepository.deleteByAppointmentId(appointmentId);
        List<Services> services = serviceRepository.findAllById(request.getServiceIds());
        BigDecimal totalPrice = services.stream().map(Services::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        appointment.setTotalPrice(totalPrice);
        appointmentItemRepository.saveAll(services.stream().map(s -> AppointmentItem.builder()
                .id(UUID.randomUUID().toString()).appointment(appointment).service(s).price(s.getPrice()).duration(s.getDuration()).build()).toList());
        return buildResponse(appointment);
    }

    // 👇 ADICIONADO: Método Genérico para Atualizar Data e Serviços juntos
    @Transactional
    public AppointmentResponse updateAppointment(String companyId, String appointmentId, AppointmentUpdateRequest request) {
        Appointment appointment = appointmentRepository.findByIdAndCompanyId(appointmentId, companyId)
                .orElseThrow(() -> new BusinessException("Agendamento não encontrado"));

        int totalDuration = 0;

        // 1. Atualiza os Serviços se foram enviados
        if (request.getServiceIds() != null && !request.getServiceIds().isEmpty()) {

            // Remove os itens antigos
            appointmentItemRepository.deleteByAppointmentId(appointmentId);

            // Busca os novos serviços
            List<Services> services = serviceRepository.findAllById(request.getServiceIds());
            if (services.isEmpty()) {
                throw new BusinessException("Os serviços selecionados não são válidos");
            }

            BigDecimal totalPrice = services.stream().map(Services::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
            totalDuration = services.stream().mapToInt(Services::getDuration).sum();

            appointment.setTotalPrice(totalPrice);

            // 👇 CORREÇÃO: Usando Collectors.toSet() para compatibilidade com a Entidade Appointment
            Set<AppointmentItem> newItems = services.stream().map(s -> AppointmentItem.builder()
                    .id(UUID.randomUUID().toString())
                    .appointment(appointment)
                    .service(s)
                    .price(s.getPrice())
                    .duration(s.getDuration())
                    .build()).collect(Collectors.toSet());

            appointmentItemRepository.saveAll(newItems);
            appointment.setAppointmentServices(newItems);
        } else {
            // Mantém a duração dos serviços que já estavam para recalcular o EndTime
            if (appointment.getAppointmentServices() != null) {
                totalDuration = appointment.getAppointmentServices().stream()
                        .mapToInt(AppointmentItem::getDuration).sum();
            }
        }

        // 2. Atualiza a Data/Hora se foi enviada
        if (request.getStartTime() != null) {
            LocalDateTime start = request.getStartTime();
            LocalDateTime end = start.plusMinutes(totalDuration);

            // Verifica conflito de horário APENAS se o horário mudou
            if (!start.equals(appointment.getStartTime()) && appointmentRepository.existsByProfessionalAndDateOverlapAndIdNot(appointment.getProfessional().getId(), start, end, appointmentId)) {
                throw new BusinessException("Este horário já está ocupado por outro agendamento.");
            }

            appointment.setStartTime(start);
            appointment.setEndTime(end);
        }

        // 3. Salva a atualização principal
        Appointment updatedAppointment = appointmentRepository.save(appointment);

        return buildResponse(updatedAppointment);
    }


    /* ================= FINANCIAL (TRAVA PLUS APLICADA) ================= */

    private void processFinancialSplit(Appointment appointment) {
        Professional prof = appointment.getProfessional();
        BigDecimal totalAmount = appointment.getTotalPrice();
        Company company = appointment.getCompany();

        String serviceTitle = "Serviço Agendado";
        if (appointment.getAppointmentServices() != null && !appointment.getAppointmentServices().isEmpty()) {
            serviceTitle = appointment.getAppointmentServices().iterator().next().getService().getName();
        }

        System.out.println(">>> PROCESSANDO FINANCEIRO: " + appointment.getId());

        FinancialRecord incomeRecord = FinancialRecord.builder()
                .type(FinancialType.APPOINTMENT)
                .amount(totalAmount)
                .title(serviceTitle)
                .description("Cliente: " + appointment.getClient().getName())
                .category("SERVICO")
                .appointment(appointment)
                .company(company)
                .professional(prof)
                .paymentMethod("DINHEIRO")
                .status("PAID")
                .referenceDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        financialRecordRepository.save(incomeRecord);

        if (company.getPlan() == PlanType.FREE) {
            return;
        }

        BigDecimal commissionRate = prof.getCommissionPercentage() != null ? prof.getCommissionPercentage() : new BigDecimal("50.00");

        if (commissionRate.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal commissionAmount = totalAmount.multiply(commissionRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            FinancialRecord commissionRecord = FinancialRecord.builder()
                    .type(FinancialType.EXPENSE)
                    .amount(commissionAmount)
                    .title("Comissão: " + prof.getName())
                    .description("Referente ao serviço " + serviceTitle)
                    .category("COMISSAO")
                    .appointment(appointment)
                    .company(company)
                    .professional(prof)
                    .paymentMethod("INTERNO")
                    .status("PENDING")
                    .referenceDate(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .build();

            financialRecordRepository.save(commissionRecord);
        }
    }

    /* ================= HELPERS ================= */

    private Client createClient(String companyId, AppointmentRequest request) {
        Company company = companyRepository.findById(companyId).orElseThrow();
        return clientRepository.save(Client.builder().name(request.getClientName()).phone(request.getClientPhone()).email(request.getClientEmail()).company(company).build());
    }

    public AvailabilityResponse getAvailability(String companyId, AppointmentAvailabilityRequest request) {
        return AvailabilityResponse.builder().professionalId(request.getProfessionalId()).date(request.getDate().toString()).availableSlots(List.of("08:00", "09:00")).build();
    }

    private AppointmentResponse buildResponse(Appointment appointment) {
        if (appointment == null) return null;
        List<AppointmentServiceResponse> servicesList = appointment.getAppointmentServices() != null
                ? appointment.getAppointmentServices().stream().map(i -> AppointmentServiceResponse.builder()
                .id(i.getService().getId()).name(i.getService().getName()).price(i.getPrice().doubleValue()).duration(i.getDuration()).build()).toList() : List.of();

        return AppointmentResponse.builder()
                .id(appointment.getId())
                .startTime(appointment.getStartTime())
                .endTime(appointment.getEndTime())
                .status(appointment.getStatus().name())
                .totalPrice(appointment.getTotalPrice().doubleValue())
                .clientName(appointment.getClient() != null ? appointment.getClient().getName() : "Cliente Removido")
                .clientPhone(appointment.getClient() != null ? appointment.getClient().getPhone() : null)
                .clientId(appointment.getClient() != null ? appointment.getClient().getId() : null)
                .professionalName(appointment.getProfessional() != null ? appointment.getProfessional().getName() : "Profissional Removido")
                .professionalId(appointment.getProfessional() != null ? appointment.getProfessional().getId() : null)
                .services(servicesList)
                .serviceNames(servicesList.stream().map(AppointmentServiceResponse::getName).collect(Collectors.toList()))
                .build();
    }
}