package com.kairon.controller;

import com.kairon.dto.request.CompanySettingsRequest;
import com.kairon.dto.response.CompanySettingsResponse;
import com.kairon.security.auth.UserDetailsImpl;
import com.kairon.service.CompanySettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Settings", description = "Company settings management endpoints")
public class CompanySettingsController {

    private final CompanySettingsService companySettingsService;

    @GetMapping("/company")
    @Operation(summary = "Get company settings")
    public ResponseEntity<CompanySettingsResponse> getCompanySettings() {
        String companyId = getCurrentUserCompanyId();
        return ResponseEntity.ok(
                companySettingsService.getCompanySettings(companyId)
        );
    }

    @PutMapping("/company")
    @Operation(summary = "Update company settings")
    public ResponseEntity<CompanySettingsResponse> updateCompanySettings(
            @Valid @RequestBody CompanySettingsRequest request
    ) {
        String companyId = getCurrentUserCompanyId();
        return ResponseEntity.ok(
                companySettingsService.updateCompanySettings(
                        companyId,
                        request,
                        companyId
                )
        );
    }

    /* =======================
       AUTH HELPER
    ======================= */
    private String getCurrentUserCompanyId() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof UserDetailsImpl userDetails)) {
            throw new RuntimeException("Invalid authentication principal");
        }

        if (userDetails.getCompanyId() == null) {
            throw new RuntimeException("User has no company");
        }

        return userDetails.getCompanyId();
    }
}
