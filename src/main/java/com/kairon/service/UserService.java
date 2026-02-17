package com.kairon.service;

import com.kairon.domain.entity.User;
import com.kairon.dto.request.UserUpdateRequest;
import com.kairon.dto.response.UserResponse;
import com.kairon.exception.BusinessException;
import com.kairon.repository.UserRepository;
import com.kairon.security.util.SecurityUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ⚠️ CORREÇÃO CRUCIAL:
    // Usamos System.getProperty("user.dir") para garantir que estamos na raiz do projeto,
    // exatamente igual ao que configuramos no WebConfig.
    private final Path fileStorageLocation = Paths.get(System.getProperty("user.dir") + "/uploads").toAbsolutePath().normalize();

    @PostConstruct
    public void init() {
        try {
            // Cria a pasta uploads se ela não existir
            Files.createDirectories(this.fileStorageLocation);
            // Log para você conferir no console onde está salvando
            System.out.println("✅ UserService pronto. Salvando arquivos em: " + this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Não foi possível criar o diretório de uploads.", ex);
        }
    }

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

    // 3. Upload de Avatar
    @Transactional
    public UserResponse uploadAvatar(MultipartFile file) {
        String userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado"));

        try {
            // Gerar nome único
            String fileName = userId + "_" + UUID.randomUUID().toString() + ".jpg";
            Path targetLocation = this.fileStorageLocation.resolve(fileName);

            // Salvar o arquivo físico na pasta
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Salvar no banco APENAS o caminho relativo
            // O Front-end vai adicionar o "http://ip:porta" antes disso
            String relativePath = "/uploads/" + fileName;

            user.setAvatar(relativePath);
            return mapToResponse(userRepository.save(user));

        } catch (IOException ex) {
            throw new BusinessException("Não foi possível salvar a imagem. Tente novamente.");
        }
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .role(user.getRole().name())
                .build();
    }
}