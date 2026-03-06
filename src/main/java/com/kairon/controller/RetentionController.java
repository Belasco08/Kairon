package com.kairon.controller;

import com.kairon.domain.entity.Client;
import com.kairon.service.RetentionService;
import com.kairon.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/retention")
@RequiredArgsConstructor
public class RetentionController {

    private final RetentionService retentionService;

    @GetMapping("/recover")
    @Operation(summary = "Lista clientes que sumiram há mais de 25 dias")
    public ResponseEntity<List<Client>> getClientsToRecover() {
        String companyId = SecurityUtils.getCurrentCompanyId();
        return ResponseEntity.ok(retentionService.getClientsToRecover(companyId));
    }
}