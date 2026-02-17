package com.kairon.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String projectDir = System.getProperty("user.dir");
        String uploadPath = "file:///" + projectDir + "/uploads/";

        System.out.println("📂 Mapeando imagens em: " + uploadPath);

        // 1. Mapeia o padrão novo (/uploads/...)
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath);

        // 2. Mapeia o padrão antigo/híbrido (/api/uploads/...)
        // Isso faz a foto da empresa voltar a funcionar sem mexer no banco!
        registry.addResourceHandler("/api/uploads/**")
                .addResourceLocations(uploadPath);
    }
}