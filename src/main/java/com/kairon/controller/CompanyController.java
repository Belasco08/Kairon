package com.kairon.controller;

import com.kairon.dto.request.CompanyUpdateRequest;
import com.kairon.dto.response.CompanyResponse;
import com.kairon.service.CompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/companies") // 👈 A Rota que o Frontend chama
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Company", description = "Endpoints de gerenciamento da empresa")
public class CompanyController {

    private final CompanyService companyService;

    // --- ROTA PÚBLICA (Para o site de agendamento) ---
    @GetMapping("/public/{id}")
    @Operation(summary = "Buscar dados públicos da empresa")
    public ResponseEntity<CompanyResponse> getPublicCompany(@PathVariable String id) {
        return ResponseEntity.ok(companyService.getCompanyPublic(id));
    }

    // --- ROTA PRIVADA: BUSCAR POR ID ---
    @GetMapping("/{id}")
    @Operation(summary = "Buscar empresa por ID (Owner)")
    public ResponseEntity<CompanyResponse> getCompanyById(@PathVariable String id) {
        return ResponseEntity.ok(companyService.getCompanyById(id));
    }

    // --- ROTA PRIVADA: ATUALIZAR POR ID ---
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar empresa por ID (Owner)")
    public ResponseEntity<CompanyResponse> updateCompany(
            @PathVariable String id,
            @Valid @RequestBody CompanyUpdateRequest request) {
        // Passamos o ID e o Request para o Service
        return ResponseEntity.ok(companyService.update(id, request));
    }

    // --- ROTA PRIVADA: UPLOAD DE LOGO ---
    @PostMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload do Logo da empresa")
    public ResponseEntity<CompanyResponse> uploadLogo(
            @PathVariable String id,
            @RequestParam("logo") MultipartFile file) {
        return ResponseEntity.ok(companyService.uploadLogo(id, file));
    }
}