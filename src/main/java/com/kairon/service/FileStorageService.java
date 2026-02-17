package com.kairon.service;

import com.kairon.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    // Simulação de salvamento local. Em produção, use S3, Cloudinary ou Firebase.
    private final Path rootLocation = Paths.get("uploads");

    public FileStorageService() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage", e);
        }
    }

    public String uploadFile(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new BusinessException("Failed to store empty file.");
            }

            // Gera um nome único para o arquivo
            String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

            // Salva o arquivo na pasta 'uploads' do projeto
            Files.copy(file.getInputStream(), this.rootLocation.resolve(filename));

            // Retorna a URL (Aqui você retornaria a URL do S3/Cloudinary)
            // Para teste local, retornamos apenas o nome ou um caminho relativo
            return "/api/uploads/" + filename;

        } catch (IOException e) {
            throw new BusinessException("Failed to store file: " + e.getMessage());
        }
    }
}