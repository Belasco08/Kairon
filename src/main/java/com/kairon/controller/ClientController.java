package com.kairon.controller;

import com.kairon.dto.request.ClientRequest;
import com.kairon.dto.response.ClientListResponse;
import com.kairon.dto.response.ClientResponse;
import com.kairon.security.util.SecurityUtils;
import com.kairon.service.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
@Tag(name = "Clients", description = "Client management endpoints")
public class ClientController {

    private final ClientService clientService;

    @PostMapping
    @Operation(summary = "Create a new client")
    public ResponseEntity<ClientResponse> createClient(@Valid @RequestBody ClientRequest request) {
        String companyId = SecurityUtils.getCurrentCompanyId();
        ClientResponse response = clientService.createClient(companyId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get client by ID")
    public ResponseEntity<ClientResponse> getClient(
            @PathVariable String id) {

        String companyId = SecurityUtils.getCurrentCompanyId();

        ClientResponse response =
                clientService.getClient(companyId, id);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update client")
    public ResponseEntity<ClientResponse> updateClient(
            @PathVariable String id,
            @Valid @RequestBody ClientRequest request) {

        String companyId = SecurityUtils.getCurrentCompanyId();

        ClientResponse response =
                clientService.updateClient(companyId, id, request);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "List clients with pagination")
    public ResponseEntity<Page<ClientListResponse>> listClients(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String professionalId, // 👈 NOVO PARÂMETRO
            @PageableDefault(size = 20) Pageable pageable) {

        String companyId = SecurityUtils.getCurrentCompanyId();

        // Passamos o professionalId para o service
        Page<ClientListResponse> response =
                clientService.listClients(companyId, professionalId, search, pageable);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    @Operation(summary = "Search clients (autocomplete)")
    public ResponseEntity<List<ClientListResponse>> searchClients(
            @RequestParam String q) {

        String companyId = SecurityUtils.getCurrentCompanyId();
        // Se quiser filtrar autocomplete por profissional tbm, precisaria alterar o service searchClients
        // Por enquanto, mantemos global da empresa
        List<ClientListResponse> response =
                clientService.searchClients(companyId, q);

        return ResponseEntity.ok(response);
    }
}
