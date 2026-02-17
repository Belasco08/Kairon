package com.kairon.controller;

import com.kairon.dto.request.ServiceRequest;
import com.kairon.dto.request.ServiceUpdateRequest;
import com.kairon.dto.response.ServiceListResponse;
import com.kairon.dto.response.ServiceResponse;
import com.kairon.security.util.SecurityUtils;
import com.kairon.service.ServiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/services")
@RequiredArgsConstructor
@Tag(name = "Services", description = "Service management endpoints")
public class ServiceController {

    private final ServiceService serviceService;

    @PostMapping
    @PreAuthorize("hasRole('OWNER') or hasRole('PROFESSIONAL')")
    @Operation(summary = "Create a new service")
    public ResponseEntity<ServiceResponse> createService(
            @Valid @RequestBody ServiceRequest request) {

        String companyId = SecurityUtils.getCurrentCompanyId();

        ServiceResponse response =
                serviceService.createService(companyId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all services with pagination and filters")
    public ResponseEntity<ServiceListResponse> getAllServices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Boolean onlineBooking,
            @RequestParam(required = false) String professionalId) {

        String companyId = SecurityUtils.getCurrentCompanyId();

        Pageable pageable =
                PageRequest.of(page, size, Sort.by("name").ascending());

        ServiceListResponse response =
                serviceService.getAllServices(
                        companyId,
                        pageable,
                        category,
                        isActive,
                        onlineBooking,
                        professionalId
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/public")
    @Operation(summary = "Get services for public booking (active and online only)")
    public ResponseEntity<List<ServiceResponse>> getPublicServices(
            @RequestParam String companyId,
            @RequestParam(required = false) String professionalId) {

        List<ServiceResponse> response =
                serviceService.getServicesForPublic(companyId, professionalId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get service by ID")
    public ResponseEntity<ServiceResponse> getServiceById(
            @PathVariable String id) {

        String companyId = SecurityUtils.getCurrentCompanyId();

        ServiceResponse response =
                serviceService.getServiceById(companyId, id);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER') or hasRole('PROFESSIONAL')")
    @Operation(summary = "Update service")
    public ResponseEntity<ServiceResponse> updateService(
            @PathVariable String id,
            @Valid @RequestBody ServiceUpdateRequest request) {

        String companyId = SecurityUtils.getCurrentCompanyId();

        ServiceResponse response =
                serviceService.updateService(companyId, id, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER') or hasRole('PROFESSIONAL')")
    @Operation(summary = "Delete service (soft delete)")
    public ResponseEntity<Void> deleteService(@PathVariable String id) {

        String companyId = SecurityUtils.getCurrentCompanyId();

        serviceService.deleteService(companyId, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('OWNER') or hasRole('PROFESSIONAL')")
    @Operation(summary = "Toggle service active status")
    public ResponseEntity<ServiceResponse> toggleServiceStatus(
            @PathVariable String id,
            @RequestParam boolean active) {

        String companyId = SecurityUtils.getCurrentCompanyId();

        ServiceResponse response =
                serviceService.toggleServiceStatus(companyId, id, active);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/online-booking")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Toggle online booking availability")
    public ResponseEntity<ServiceResponse> toggleOnlineBooking(
            @PathVariable String id,
            @RequestParam boolean onlineBooking) {

        String companyId = SecurityUtils.getCurrentCompanyId();

        ServiceResponse response =
                serviceService.toggleOnlineBooking(companyId, id, onlineBooking);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/assign/{professionalId}")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Assign service to professional")
    public ResponseEntity<ServiceResponse> assignToProfessional(
            @PathVariable String id,
            @PathVariable String professionalId) {

        String companyId = SecurityUtils.getCurrentCompanyId();

        ServiceResponse response =
                serviceService.assignToProfessional(companyId, id, professionalId);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/unassign")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Unassign service from professional")
    public ResponseEntity<ServiceResponse> unassignFromProfessional(
            @PathVariable String id) {

        String companyId = SecurityUtils.getCurrentCompanyId();

        ServiceResponse response =
                serviceService.unassignFromProfessional(companyId, id);

        return ResponseEntity.ok(response);
    }
}
