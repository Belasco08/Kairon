package com.kairon.controller;

import com.kairon.dto.request.AuthRequest;
import com.kairon.dto.request.RegisterRequest;
import com.kairon.dto.request.RefreshTokenRequest;
import com.kairon.dto.response.AuthResponse;
import com.kairon.dto.response.TokenRefreshResponse;
import com.kairon.security.auth.UserDetailsImpl;
import com.kairon.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints de autenticação e registro")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Registrar nova empresa e proprietário")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Autenticar usuário")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody AuthRequest request
    ) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token")
    public ResponseEntity<TokenRefreshResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout do usuário autenticado")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        authService.logout(userDetails.getEmail());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Perfil do usuário autenticado")
    public ResponseEntity<AuthResponse> me(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        return ResponseEntity.ok(
                authService.getProfile(userDetails.getEmail())
        );
    }
}
