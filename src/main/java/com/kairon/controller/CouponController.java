package com.kairon.controller;

import com.kairon.domain.entity.User;
import com.kairon.dto.request.CouponRequest;
import com.kairon.repository.UserRepository;
import com.kairon.security.util.SecurityUtils;
import com.kairon.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/coupons") // Se o seu projeto exige /api global, pode ser que a URL final seja /api/coupons
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;
    private final UserRepository userRepository;

    @PostMapping("/apply")
    public ResponseEntity<Map<String, String>> applyCoupon(@RequestBody CouponRequest request) {

        // 1. Pega o ID do usuário que está fazendo a requisição pelo Token (JWT)
        String currentUserId = SecurityUtils.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        // 2. Pega o ID da empresa vinculada a este usuário
        // ATENÇÃO: Se na sua classe User a empresa estiver mapeada como objeto, use: currentUser.getCompany().getId()
        String companyId = currentUser.getCompany().getId();

        // 3. Manda o serviço validar e aplicar os meses grátis
        String successMessage = couponService.applyCoupon(companyId, request.getCode());

        // 4. Devolve o JSON bonitinho pro React Native exibir no Alert
        Map<String, String> response = new HashMap<>();
        response.put("message", successMessage);

        return ResponseEntity.ok(response);
    }
}