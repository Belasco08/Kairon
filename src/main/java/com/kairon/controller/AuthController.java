package com.kairon.controller;

import com.kairon.dto.request.*;
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

import java.util.Map;

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

    @PostMapping("/forgot-password")
    @Operation(summary = "Solicitar recuperação de palavra-passe")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        authService.forgotPassword(request);
        // Retornamos sempre sucesso por segurança (para não revelar se o e-mail existe a hackers)
        return ResponseEntity.ok(Map.of("message", "Se o e-mail existir, as instruções foram enviadas."));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Definir nova palavra-passe com token")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Palavra-passe alterada com sucesso."));
    }
}
