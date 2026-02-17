package com.kairon.controller;

import com.kairon.domain.entity.*;
import com.kairon.repository.*;
import com.kairon.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
public class PublicBookingController {

    private final ServiceRepository serviceRepository;
    private final AppointmentRepository appointmentRepository;
    private final ClientRepository clientRepository;
    private final CompanyRepository companyRepository;
    // 👇 Usamos o Repositório correto agora
    private final ProfessionalRepository professionalRepository;
    private final NotificationService notificationService;

    // DTOs
    public record ServiceResponse(String id, String name, BigDecimal price, Integer duration) {}
    public record ProfessionalResponse(String id, String name) {}

    // 1. Listar Serviços
    @GetMapping("/services/{companyId}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ServiceResponse>> getServices(@PathVariable String companyId) {
        var services = serviceRepository.findByCompanyId(companyId);

        var response = services.stream()
                .map(s -> new ServiceResponse(s.getId(), s.getName(), s.getPrice(), s.getDuration()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    // 2. Listar Profissionais (CORRIGIDO: Busca na tabela Professional)
    @GetMapping("/professionals/{companyId}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ProfessionalResponse>> getProfessionals(@PathVariable String companyId) {
        // Busca apenas profissionais ATIVOS da empresa
        var professionals = professionalRepository.findActiveByCompanyId(companyId);

        var response = professionals.stream()
                // Assumindo que Professional tem getName(). Se o nome estiver no User, use p.getUser().getName()
                .map(p -> new ProfessionalResponse(p.getId(), p.getName()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    // 3. Ver Horários Disponíveis
    @GetMapping("/availability")
    @Transactional(readOnly = true)
    public ResponseEntity<List<String>> getAvailability(
            @RequestParam String professionalId,
            @RequestParam String date) {

        LocalDate localDate = LocalDate.parse(date);
        List<String> availableSlots = new ArrayList<>();

        // Horário fixo para teste (9h as 18h)
        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(18, 0);

        LocalDateTime dayStart = localDate.atStartOfDay();
        LocalDateTime dayEnd = localDate.atTime(23, 59);

        List<Appointment> existingApps = appointmentRepository.findByProfessionalIdAndStartTimeBetween(
                professionalId, dayStart, dayEnd
        );

        while (start.isBefore(end)) {
            final LocalTime currentSlot = start;
            boolean isBusy = existingApps.stream()
                    .anyMatch(app -> app.getStartTime().toLocalTime().equals(currentSlot));

            if (!isBusy) {
                availableSlots.add(currentSlot.toString());
            }
            start = start.plusHours(1);
        }

        return ResponseEntity.ok(availableSlots);
    }

    // 4. Salvar o Agendamento (CORRIGIDO)
    @PostMapping("/appointments")
    @Transactional
    public ResponseEntity<?> createPublicAppointment(@RequestBody PublicAppointmentRequest request) {

        // A. Empresa
        Company company = companyRepository.findById(request.companyId)
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));

        // B. Cliente (Sem setar ID manual)
        Client client = clientRepository.findByPhoneAndCompanyId(request.clientPhone, request.companyId)
                .orElseGet(() -> {
                    Client newClient = new Client();
                    newClient.setName(request.clientName);
                    newClient.setPhone(request.clientPhone);
                    newClient.setCompany(company);
                    return clientRepository.save(newClient);
                });

        // C. Serviço
        com.kairon.domain.entity.Services service = serviceRepository.findById(request.serviceId)
                .orElseThrow(() -> new RuntimeException("Serviço não encontrado"));

        // D. Profissional (BUSCA CORRETA NO REPOSITÓRIO DE PROFISSIONAIS)
        Professional professional = professionalRepository.findById(request.professionalId)
                .orElseThrow(() -> new RuntimeException("Profissional não encontrado"));

        // E. Cria Agendamento
        Appointment app = new Appointment();
        // ID gerado pelo banco

        app.setCompany(company);
        app.setClient(client);
        app.setProfessional(professional); // Agora o tipo bate! Sem cast.

        LocalDate date = LocalDate.parse(request.date);
        LocalTime time = LocalTime.parse(request.time);

        app.setStartTime(LocalDateTime.of(date, time));

        int duration = service.getDuration() != null ? service.getDuration() : 30;
        app.setEndTime(app.getStartTime().plusMinutes(duration));

        app.setTotalPrice(service.getPrice());
        app.setTimezone("America/Sao_Paulo");

        // 6. Item do Agendamento (CORRIGIDO: Adicionado duration)
        AppointmentItem item = new AppointmentItem();

        item.setAppointment(app);
        item.setService(service);
        item.setPrice(service.getPrice());
        item.setName(service.getName());

        // 👇 ADICIONE ESTA LINHA:
        // Pega a duração do serviço. Se for nulo, coloca 30 como padrão.
        int durationItem = service.getDuration() != null ? service.getDuration() : 30;
        item.setDuration(durationItem);

        // Garante que a lista de serviços não seja nula antes de adicionar
        if (app.getAppointmentServices() == null) {
            app.setAppointmentServices(new java.util.HashSet<>());
        }
        app.getAppointmentServices().add(item);

        // 7. Salva o agendamento (Isso salvará o cliente e o item em cascata)
        appointmentRepository.save(app);

        notificationService.notifyUpdate(request.companyId(), "APPOINTMENT_NEW");

        return ResponseEntity.ok().build();
    }

    public record PublicAppointmentRequest(
            String companyId,
            String serviceId,
            String professionalId,
            String date,
            String time,
            String clientName,
            String clientPhone
    ) {}
}