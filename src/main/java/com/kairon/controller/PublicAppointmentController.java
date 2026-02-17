package com.kairon.controller;

import com.kairon.dto.request.AppointmentRequest;
import com.kairon.dto.response.AppointmentResponse;
import com.kairon.dto.response.ProfessionalResponse;
import com.kairon.dto.response.ServiceResponse;
import com.kairon.service.AppointmentService;
import com.kairon.service.ProfessionalService;
import com.kairon.service.ServiceService; // Certifique-se de ter este service
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/public/appointments")
@RequiredArgsConstructor
@Tag(name = "Public Booking", description = "Endpoints para agendamento externo (Clientes)")
public class PublicAppointmentController {

    private final AppointmentService appointmentService;
    private final ProfessionalService professionalService;
    // Injete o ServiceService se você tiver ele criado para buscar os serviços
    // private final ServiceService serviceService;

    @GetMapping("/{companyId}/professionals")
    @Operation(summary = "Lista profissionais para o cliente escolher")
    public ResponseEntity<List<ProfessionalResponse>> getPublicProfessionals(
            @PathVariable String companyId) {
        // Traz apenas profissionais ativos
        return ResponseEntity.ok(professionalService.getAllProfessionals(companyId, true));
    }

    /*
    @GetMapping("/{companyId}/services")
    @Operation(summary = "Lista serviços para o cliente escolher")
    public ResponseEntity<List<ServiceResponse>> getPublicServices(
            @PathVariable String companyId) {
        return ResponseEntity.ok(serviceService.getAllServices(companyId));
    }
    */


    @PostMapping("/{companyId}")
    @Operation(summary = "Cria o agendamento público (Cliente finaliza)")
    public ResponseEntity<AppointmentResponse> createPublicAppointment(
            @PathVariable String companyId,
            @Valid @RequestBody AppointmentRequest request) {

        // Reutiliza a lógica inteligente do Service
        return ResponseEntity.ok(appointmentService.createAppointment(companyId, request));
    }
}