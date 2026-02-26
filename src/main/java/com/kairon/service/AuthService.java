package com.kairon.service;

import com.kairon.domain.entity.Company;
import com.kairon.domain.entity.Professional;
import com.kairon.domain.entity.User;
import com.kairon.domain.enums.PlanType;
import com.kairon.domain.enums.Role;
import com.kairon.dto.request.*;
import com.kairon.dto.response.AuthResponse;
import com.kairon.dto.response.TokenRefreshResponse;
import com.kairon.exception.BusinessException;
import com.kairon.repository.CompanyRepository;
import com.kairon.repository.ProfessionalRepository;
import com.kairon.repository.UserRepository;
import com.kairon.security.auth.UserDetailsImpl;
import com.kairon.security.auth.UserDetailsServiceImpl;
import com.kairon.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;

    private final ProfessionalRepository professionalRepository;
    // private final ObjectMapper objectMapper; // REMOVIDO: Não precisamos mais dele aqui

    /* =======================
       REGISTER
    ======================= */

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email já cadastrado");
        }

        String slug = generateSlug(request.getCompanyName());

        if (companyRepository.existsBySlug(slug)) {
            throw new BusinessException("Nome da empresa já está em uso");
        }

        Company company = companyRepository.save(
                Company.builder()
                        .name(request.getCompanyName())
                        .slug(slug)
                        .businessType(request.getBusinessType())
                        .businessHours(createDefaultBusinessHours())
                        .timezone("America/Sao_Paulo")
                        .currency("BRL")
                        .slotDuration(30)
                        .bufferTime(10)
                        .isActive(true)
                        .isVerified(false)
                        .plan(PlanType.PLUS)
                        .build()
        );

        User user = userRepository.save(
                User.builder()
                        .name(request.getName())
                        .email(request.getEmail())
                        .password(passwordEncoder.encode(request.getPassword()))
                        .phone(request.getPhone())
                        .role(Role.OWNER)
                        .company(company)
                        .isActive(true)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        // =========================================================
        // 🌟 O PULO DO GATO: CRIAR O PROFISSIONAL AUTOMATICAMENTE 🌟
        // =========================================================
        Professional professional = Professional.builder()
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .company(company)
                // .user(user) // <-- Descomente esta linha se a sua classe Professional tiver um campo "private User user;"
                .isActive(true)
                .build();

        professionalRepository.save(professional);
        // =========================================================

        return buildAuthResponse(user);
    }

    /* =======================
       LOGIN (Mantido igual)
    ======================= */
    @Transactional
    public AuthResponse login(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new BusinessException("Usuário não encontrado"));

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        // 👇 CORREÇÃO: Extraindo o plano antes de usar
        String plan = "PLUS"; // Valor padrão
        if (user.getCompany() != null && user.getCompany().getPlan() != null) {
            plan = user.getCompany().getPlan().name(); // Converte Enum para String (FREE ou PLUS)
        }

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .user(AuthResponse.UserResponse.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .role(user.getRole().name())
                        .companyId(user.getCompany() != null ? user.getCompany().getId() : null)
                        .phone(user.getPhone())
                        .avatar(user.getAvatar())
                        .plan(plan) // 👈 Agora a variável 'plan' existe!
                        .build())
                .build();
    }

    // ... (Métodos refresh, logout, profile e helpers mantidos iguais) ...

    @Transactional
    public TokenRefreshResponse refreshToken(RefreshTokenRequest request) {
        String email = jwtService.extractUsernameFromRefreshToken(request.getRefreshToken());
        UserDetailsImpl userDetails = (UserDetailsImpl) userDetailsService.loadUserByUsername(email);

        if (!jwtService.isRefreshTokenValid(request.getRefreshToken(), userDetails)) {
            throw new BusinessException("Refresh token inválido");
        }

        String newToken = jwtService.generateToken(userDetails);
        String newRefresh = jwtService.generateRefreshToken(userDetails);

        User user = userRepository.findById(userDetails.getId()).orElseThrow();
        user.setRefreshToken(newRefresh);

        return TokenRefreshResponse.builder()
                .token(newToken)
                .refreshToken(newRefresh)
                .build();
    }

    @Transactional
    public void logout(String email) {
        User user = userRepository.findActiveUserByEmail(email)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado"));
        user.setRefreshToken(null);
    }

    public AuthResponse getProfile(String email) {
        User user = userRepository.findActiveUserByEmail(email)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado"));
        return buildAuthResponse(user, null, null);
    }

    private AuthResponse buildAuthResponse(User user) {
        UserDetailsImpl userDetails = (UserDetailsImpl) userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);
        String refresh = jwtService.generateRefreshToken(userDetails);
        user.setRefreshToken(refresh);
        return buildAuthResponse(user, token, refresh);
    }

    private AuthResponse buildAuthResponse(User user, String token, String refresh) {
        return AuthResponse.builder()
                .user(AuthResponse.UserResponse.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .role(user.getRole().name())
                        .companyId(user.getCompany().getId())
                        .phone(user.getPhone())
                        .build())
                .token(token)
                .refreshToken(refresh)
                .build();
    }

    /* =======================
       UTILS
    ======================= */

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
    }

    // 👇 MÉTODO ATUALIZADO: Retorna Map<String, Object> em vez de ObjectNode
    private Map<String, Object> createDefaultBusinessHours() {
        Map<String, Object> hours = new HashMap<>();
        hours.put("monday", createDay("08:00", "18:00", true));
        hours.put("tuesday", createDay("08:00", "18:00", true));
        hours.put("wednesday", createDay("08:00", "18:00", true));
        hours.put("thursday", createDay("08:00", "18:00", true));
        hours.put("friday", createDay("08:00", "18:00", true));
        hours.put("saturday", createDay("09:00", "13:00", true));
        hours.put("sunday", createDay("09:00", "13:00", false));
        return hours;
    }

    private Map<String, Object> createDay(String start, String end, boolean active) {
        Map<String, Object> day = new HashMap<>();
        day.put("start", start);
        day.put("end", end);
        day.put("active", active);
        return day;
    }

    private final JavaMailSender mailSender; // Adicione esta linha

    public void forgotPassword(ForgotPasswordRequest request) {
        // 1. Acha o usuário
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("E-mail não encontrado."));

        // 2. Gera o token e salva no banco
        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        userRepository.save(user);

        // 3. Monta o e-mail
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("suportekairon@gmail.com");
        message.setTo(user.getEmail());
        message.setSubject("Kairon - Recuperação de Senha");

        // Manda o Token solto e também um "Deep Link" para o aplicativo
        message.setText("Olá!\n\nVocê solicitou a recuperação de senha no Kairon.\n\n" +
                "Copie o código abaixo e cole no aplicativo:\n" +
                "TOKEN: " + token + "\n\n" +
                "Se preferir, clique no link abaixo no seu celular para abrir o app direto:\n" +
                "kairon://reset-password?token=" + token + "\n\n" +
                "Se não foi você, apenas ignore este e-mail.");

        // 4. Dispara o e-mail de verdade!
        mailSender.send(message);
    }

    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByResetToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Token inválido ou expirado."));

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setResetToken(null);
        userRepository.save(user);
    }
}
