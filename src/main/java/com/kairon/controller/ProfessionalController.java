package com.kairon.controller;

import com.kairon.dto.request.ProfessionalRequest;
import com.kairon.dto.request.ProfessionalUpdateRequest;
import com.kairon.dto.response.ProfessionalResponse;
import com.kairon.dto.response.ProfessionalWithServicesResponse;
import com.kairon.dto.response.ServiceResponse;
import com.kairon.security.util.SecurityUtils;
import com.kairon.service.ProfessionalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/professionals")
@RequiredArgsConstructor
@Tag(name = "Professionals", description = "Professional management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class ProfessionalController {

    private final ProfessionalService professionalService;

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Create a new professional and user login")
    public ResponseEntity<Void> createProfessional(
            @Valid @RequestBody ProfessionalRequest request) {

        String companyId = SecurityUtils.getCurrentCompanyId();

        // Agora retorna void para evitar erro de serialização
        professionalService.createProfessional(companyId, request);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Update a professional")
    public ResponseEntity<ProfessionalResponse> updateProfessional(
            @PathVariable String id, // Esse é o ID do profissional
            @Valid @RequestBody ProfessionalUpdateRequest request // O corpo da requisição
    ) {
        // Pega o ID da empresa do token de segurança
        String companyId = SecurityUtils.getCurrentCompanyId();

        return ResponseEntity.ok(
                professionalService.updateProfessional(companyId, id, request)
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Delete a professional")
    public ResponseEntity<Void> deleteProfessional(@PathVariable String id) {

        String companyId = SecurityUtils.getCurrentCompanyId();

        professionalService.deleteProfessional(companyId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Get a professional by ID")
    public ResponseEntity<ProfessionalResponse> getProfessional(@PathVariable String id) {

        String companyId = SecurityUtils.getCurrentCompanyId();

        return ResponseEntity.ok(
                professionalService.getProfessional(companyId, id)
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('OWNER') or hasRole('PROFESSIONAL')")
    @Operation(summary = "Get all professionals")
    public ResponseEntity<List<ProfessionalResponse>> getAllProfessionals(
            @RequestParam(required = false) Boolean active) {

        String companyId = SecurityUtils.getCurrentCompanyId();

        return ResponseEntity.ok(
                professionalService.getAllProfessionals(companyId, active)
        );
    }

    // REMOVIDO: Endpoints de assign/unassign user não são mais necessários
    // pois o usuário é criado junto com o profissional.

    @GetMapping("/{id}/services")
    @PreAuthorize("hasRole('OWNER') or hasRole('PROFESSIONAL')")
    @Operation(summary = "Get services for a professional")
    public ResponseEntity<List<ServiceResponse>> getProfessionalServices(
            @PathVariable String id,
            @RequestParam(required = false) Boolean active) {

        String companyId = SecurityUtils.getCurrentCompanyId();

        return ResponseEntity.ok(
                professionalService.getProfessionalServices(companyId, id, active)
        );
    }

    @GetMapping("/{id}/with-services")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Get a professional with their services")
    public ResponseEntity<ProfessionalWithServicesResponse> getProfessionalWithServices(
            @PathVariable String id) {

        String companyId = SecurityUtils.getCurrentCompanyId();

        return ResponseEntity.ok(
                professionalService.getProfessionalWithServices(companyId, id)
        );
    }
}