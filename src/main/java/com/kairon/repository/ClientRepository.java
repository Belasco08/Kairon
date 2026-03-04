package com.kairon.repository;

import com.kairon.domain.entity.Client;
import com.kairon.domain.enums.AppointmentStatus; // 👈 NOVO IMPORT
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime; // 👈 NOVO IMPORT
import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, String> {

    // Métodos existentes
    List<Client> findByCompanyId(String companyId);

    Page<Client> findByCompanyId(String companyId, Pageable pageable);

    Optional<Client> findByPhoneAndCompanyId(String phone, String companyId);

    Optional<Client> findByIdAndCompanyId(String id, String companyId);

    // Método com paginação
    @Query("SELECT c FROM Client c WHERE c.company.id = :companyId AND " +
            "(LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "c.phone LIKE CONCAT('%', :search, '%'))")
    Page<Client> searchByCompanyId(
            @Param("companyId") String companyId,
            @Param("search") String search,
            Pageable pageable);

    // Método existente para busca sem paginação (para autocomplete)
    @Query("SELECT c FROM Client c WHERE c.company.id = :companyId AND " +
            "(LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "c.phone LIKE CONCAT('%', :search, '%'))")
    List<Client> searchByCompanyId(@Param("companyId") String companyId, @Param("search") String search);

    // Busca clientes que têm agendamento com o profissional específico
    @Query("SELECT DISTINCT c FROM Client c " +
            "JOIN c.appointments a " +
            "WHERE c.company.id = :companyId " +
            "AND a.professional.id = :professionalId")
    Page<Client> findByCompanyIdAndProfessionalId(
            @Param("companyId") String companyId,
            @Param("professionalId") String professionalId,
            Pageable pageable);

    // Método de busca (Search) também filtrado pelo profissional
    @Query("SELECT DISTINCT c FROM Client c " +
            "JOIN c.appointments a " +
            "WHERE c.company.id = :companyId " +
            "AND a.professional.id = :professionalId " +
            "AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR c.phone LIKE CONCAT('%', :search, '%'))")
    Page<Client> searchByCompanyIdAndProfessionalId(
            @Param("companyId") String companyId,
            @Param("professionalId") String professionalId,
            @Param("search") String search,
            Pageable pageable);


    /* =========================================================================
       👇 NOVAS QUERIES: RADAR DE CLIENTES SUMIDOS ("DINHEIRO NA MESA") 👇
       ========================================================================= */

    // 1. Busca clientes sumidos da Empresa toda (Para o OWNER)
    @Query("SELECT DISTINCT c FROM Client c " +
            "WHERE c.company.id = :companyId " +
            "AND (SELECT MAX(a.startTime) FROM Appointment a WHERE a.client.id = c.id AND a.status = :completedStatus) <= :cutoffDate " +
            "AND NOT EXISTS (" +
            "    SELECT 1 FROM Appointment a2 WHERE a2.client.id = c.id AND a2.startTime >= :now AND a2.status IN :pendingStatuses" +
            ")")
    Page<Client> findMissingClients(
            @Param("companyId") String companyId,
            @Param("cutoffDate") LocalDateTime cutoffDate,
            @Param("now") LocalDateTime now,
            @Param("completedStatus") AppointmentStatus completedStatus,
            @Param("pendingStatuses") List<AppointmentStatus> pendingStatuses,
            Pageable pageable);

    // 2. Busca clientes sumidos apenas do Profissional (Para a equipe)
    @Query("SELECT DISTINCT c FROM Client c " +
            "WHERE c.company.id = :companyId " +
            "AND (SELECT MAX(a.startTime) FROM Appointment a WHERE a.client.id = c.id AND a.professional.id = :professionalId AND a.status = :completedStatus) <= :cutoffDate " +
            "AND NOT EXISTS (" +
            "    SELECT 1 FROM Appointment a2 WHERE a2.client.id = c.id AND a2.professional.id = :professionalId AND a2.startTime >= :now AND a2.status IN :pendingStatuses" +
            ")")
    Page<Client> findMissingClientsByProfessional(
            @Param("companyId") String companyId,
            @Param("professionalId") String professionalId,
            @Param("cutoffDate") LocalDateTime cutoffDate,
            @Param("now") LocalDateTime now,
            @Param("completedStatus") AppointmentStatus completedStatus,
            @Param("pendingStatuses") List<AppointmentStatus> pendingStatuses,
            Pageable pageable);

}