package com.kairon;

import org.flywaydb.core.internal.jdbc.JdbcTemplate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableScheduling;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
@EnableJpaAuditing
@EnableScheduling // 👈 Adiciona isso aqui!
public class KarionBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(KarionBackendApplication.class, args);
	}


	// 👇 ADICIONE ESTE BLOCO AQUI 👇
	@Bean
	public CommandLineRunner forceDatabaseUpdate(JdbcTemplate jdbcTemplate) {
		return args -> {
			try {
				// Força a criação da coluna direto no banco de dados com SQL nativo
				jdbcTemplate.execute("ALTER TABLE appointments ADD COLUMN IF NOT EXISTS is_paid BOOLEAN DEFAULT true");
				System.out.println("✅ [KAIRON] Coluna is_paid verificada/criada com sucesso no banco!");
			} catch (Exception e) {
				System.out.println("⚠️ [KAIRON] Aviso: " + e.getMessage());
			}
		};
	}
}
