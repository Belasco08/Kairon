package com.kairon.controller;

import com.kairon.dto.request.*;
import com.kairon.dto.response.AppointmentResponse;
import com.kairon.dto.response.AvailabilityResponse; // Se não tiver, remova
import com.kairon.security.util.SecurityUtils;
import com.kairon.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "Endpoints de Agendamento")
public class AppointmentController {

    private final AppointmentService appointmentService;

    // --- CRIAR ---
    @PostMapping
    @PreAuthorize("hasRole('OWNER') or hasRole('PROFESSIONAL') or hasRole('CLIENT')")
    @Operation(summary = "Criar novo agendamento")
    public ResponseEntity<AppointmentResponse> createAppointment(
            @Valid @RequestBody AppointmentRequest request) {
        String companyId = SecurityUtils.getCurrentCompanyId();
        // Se for Client criando, o companyId pode vir do request ou do contexto
        // Ajuste conforme sua lógica de segurança
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(appointmentService.createAppointment(companyId, request));
    }

    // --- LISTAR (COM FILTROS) ---
    @GetMapping
    @Operation(summary = "Listar agendamentos (Filtro por Data e Profissional)")
    public ResponseEntity<List<AppointmentResponse>> getAppointments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String professionalId
    ) {
        String companyId = SecurityUtils.getCurrentCompanyId();
        return ResponseEntity.ok(appointmentService.listAppointments(companyId, date, professionalId));
    }

    // --- BUSCAR POR ID ---
    @GetMapping("/{id}")
    @Operation(summary = "Buscar agendamento por ID")
    public ResponseEntity<AppointmentResponse> getAppointment(@PathVariable String id) {
        String companyId = SecurityUtils.getCurrentCompanyId();
        return ResponseEntity.ok(appointmentService.getAppointment(companyId, id));
    }

    // --- ATUALIZAR STATUS (CONFIRMAR, CANCELAR, CONCLUIR) ---
    // 👇 É AQUI QUE O APP BATE QUANDO CLICA EM "CONFIRMAR"
    @PutMapping("/{id}/status")
    @Operation(summary = "Atualizar status do agendamento")
    public ResponseEntity<AppointmentResponse> updateAppointmentStatus(
            @PathVariable String id,
            @Valid @RequestBody AppointmentStatusRequest request) {

        String companyId = SecurityUtils.getCurrentCompanyId();
        return ResponseEntity.ok(appointmentService.updateAppointmentStatus(companyId, id, request));
    }

    // --- ATUALIZAR SERVIÇOS ---
    @PutMapping("/{id}/services")
    @Operation(summary = "Atualizar serviços do agendamento")
    public ResponseEntity<AppointmentResponse> updateAppointmentServices(
            @PathVariable String id,
            @Valid @RequestBody AppointmentUpdateRequest request) { // Crie esse DTO se não tiver, ou use AppointmentRequest

        String companyId = SecurityUtils.getCurrentCompanyId();
        return ResponseEntity.ok(appointmentService.updateAppointmentServices(companyId, id, request));
    }

    // ADICIONE ESTE NOVO ENDPOINT NO SEU CONTROLLER
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar agendamento completo (Data, Hora e Serviços)")
    public ResponseEntity<AppointmentResponse> updateAppointment(
            @PathVariable String id,
            @Valid @RequestBody AppointmentUpdateRequest request) {

        String companyId = SecurityUtils.getCurrentCompanyId();

        // Certifique-se de ter esse método genérico 'updateAppointment' no seu Service
        // para salvar tanto o novo 'startTime' quanto os novos 'serviceIds'
        return ResponseEntity.ok(appointmentService.updateAppointment(companyId, id, request));
    }
}