package com.kairon.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.kairon.domain.entity.User;
import com.kairon.dto.request.UserUpdateRequest;
import com.kairon.dto.response.UserResponse;
import com.kairon.exception.BusinessException;
import com.kairon.repository.UserRepository;
import com.kairon.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Cloudinary cloudinary; // 👈 O Cloudinary foi injetado aqui

    // 1. Pegar Perfil Atual
    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile() {
        String userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado"));
        return mapToResponse(user);
    }

    // 2. Atualizar Perfil (Nome, Telefone, Senha)
    @Transactional
    public UserResponse updateProfile(UserUpdateRequest request) {
        String userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado"));

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        // Lógica de Troca de Senha
        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            if (request.getCurrentPassword() == null || !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new BusinessException("A senha atual está incorreta.");
            }
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        return mapToResponse(userRepository.save(user));
    }

    // 3. Upload de Avatar (Agora via Cloudinary) ☁️
    @Transactional
    public UserResponse uploadAvatar(MultipartFile file) {
        String userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado"));

        try {
            // Gera um ID único para a imagem no Cloudinary
            String publicId = "kairon_avatar_" + userId + "_" + UUID.randomUUID().toString();

            // Faz o upload direto do fluxo de bytes (sem salvar no disco local)
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "public_id", publicId,
                    "folder", "kairon/avatars" // Organiza numa pasta lá no Cloudinary
            ));

            // O Cloudinary devolve a URL segura (https) da imagem
            String secureUrl = uploadResult.get("secure_url").toString();

            // Salvamos a URL da nuvem direto no banco de dados
            user.setAvatar(secureUrl);
            return mapToResponse(userRepository.save(user));

        } catch (IOException ex) {
            throw new BusinessException("Erro ao processar o arquivo de imagem.");
        } catch (Exception ex) {
            throw new BusinessException("Falha na comunicação com o servidor de imagens.");
        }
    }

    private UserResponse mapToResponse(User user) {
        // 👇 1. Verificamos qual é o plano atual gravado no banco
        String plan = "FREE";
        if (user.getCompany() != null && user.getCompany().getPlan() != null) {
            plan = user.getCompany().getPlan().name();
        }

        // 👇 2. Devolvemos a "mochila" completa pro celular!
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .role(user.getRole().name())
                .companyId(user.getCompany() != null ? user.getCompany().getId() : null) // Sempre bom mandar a empresa
                .plan(plan) // 👈 O SEGREDO ESTÁ AQUI! Agora o celular sabe que você é PLUS.
                .build();
    }
}