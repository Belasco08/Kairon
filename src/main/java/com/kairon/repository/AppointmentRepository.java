package com.kairon.repository;

import com.kairon.domain.entity.Appointment;
import com.kairon.domain.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, String> {

    // --- MÉTODOS BÁSICOS ---
    List<Appointment> findByCompanyId(String companyId);

    List<Appointment> findByCompanyIdAndProfessionalId(String companyId, String professionalId);

    @Query("SELECT a FROM Appointment a WHERE a.company.id = :companyId AND a.client.id = :clientId ORDER BY a.startTime DESC")
    List<Appointment> findByCompanyIdAndClientId(
            @Param("companyId") String companyId,
            @Param("clientId") String clientId
    );

    Optional<Appointment> findByIdAndCompanyId(String id, String companyId);

    // Método antigo usado na agenda
    List<Appointment> findByProfessionalIdAndStartTimeBetween(String professionalId, LocalDateTime start, LocalDateTime end);

    // ============================================
    //      MÉTODOS PARA O FINANCEIRO (EXTRATO)
    // ============================================

    // 1. Busca cortes concluídos num intervalo de datas (Para somar no Dashboard)
    List<Appointment> findByCompanyIdAndStartTimeBetweenAndStatus(
            String companyId,
            LocalDateTime start,
            LocalDateTime end,
            AppointmentStatus status
    );

    // 2. Busca histórico completo de cortes concluídos (Para o Extrato unificado)
    List<Appointment> findByCompanyIdAndStatusOrderByStartTimeDesc(
            String companyId,
            AppointmentStatus status
    );

    List<Appointment> findByCompanyIdAndStatus(String companyId, AppointmentStatus status);

    // ============================================
    //              VALIDAÇÕES (AGENDA)
    // ============================================

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Appointment a " +
            "WHERE a.professional.id = :professionalId " +
            "AND a.status <> com.kairon.domain.enums.AppointmentStatus.CANCELLED " +
            "AND (a.startTime < :endTime AND a.endTime > :startTime)")
    boolean existsByProfessionalAndDateOverlap(
            @Param("professionalId") String professionalId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Appointment a " +
            "WHERE a.professional.id = :professionalId " +
            "AND a.id <> :appointmentId " +
            "AND a.status <> com.kairon.domain.enums.AppointmentStatus.CANCELLED " +
            "AND a.status <> com.kairon.domain.enums.AppointmentStatus.NO_SHOW " +
            "AND (a.startTime < :endTime AND a.endTime > :startTime)")
    boolean existsByProfessionalAndDateOverlapAndIdNot(
            @Param("professionalId") String professionalId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("appointmentId") String appointmentId
    );

    // ============================================
    //              LISTAGEM (AGENDA VISUAL)
    // ============================================

    // 1. Agenda GERAL (Para o Dono ver tudo)
    @Query("SELECT a FROM Appointment a " +
            "WHERE a.company.id = :companyId " +
            "AND a.startTime >= :startOfDay " +
            "AND a.startTime <= :endOfDay " +
            "AND a.status <> com.kairon.domain.enums.AppointmentStatus.CANCELLED " +
            "ORDER BY a.startTime ASC")
    List<Appointment> findByCompanyAndDateRange(
            @Param("companyId") String companyId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );

    // 2. Agenda FILTRADA (Para o Profissional ou Filtro do Dono)
    @Query("SELECT a FROM Appointment a " +
            "WHERE a.company.id = :companyId " +
            "AND a.professional.id = :professionalId " +
            "AND a.startTime >= :startOfDay " +
            "AND a.startTime <= :endOfDay " +
            "ORDER BY a.startTime ASC")
    List<Appointment> findByCompanyAndProfessionalAndDateRange(
            @Param("companyId") String companyId,
            @Param("professionalId") String professionalId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );

    // ============================================
    //              CONTAGENS ANTIGAS
    // ============================================

    @Query("SELECT COUNT(a) FROM Appointment a " +
            "WHERE a.company.id = :companyId " +
            "AND a.status = com.kairon.domain.enums.AppointmentStatus.COMPLETED " +
            "AND a.startTime >= :start " +
            "AND a.startTime <= :end")
    Long countCompletedAppointments(
            @Param("companyId") String companyId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT COUNT(a) FROM Appointment a " +
            "WHERE a.company.id = :companyId " +
            "AND a.professional.id = :professionalId " +
            "AND a.status = com.kairon.domain.enums.AppointmentStatus.COMPLETED " +
            "AND a.startTime >= :startDate " +
            "AND a.startTime <= :endDate")
    Long countCompletedAppointmentsByProfessional(
            @Param("companyId") String companyId,
            @Param("professionalId") String professionalId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // =========================================================================
    // 👇 NOVAS QUERIES: GAMIFICAÇÃO DA EQUIPE (O "VIDEOGAME" DO PROFISSIONAL)
    // =========================================================================

    // 1. Calcula quanto o profissional já faturou HOJE (Soma dos cortes concluídos dele)
    @Query("SELECT SUM(a.totalPrice) FROM Appointment a " +
            "WHERE a.company.id = :companyId " +
            "AND a.professional.id = :professionalId " +
            "AND a.status = :status " +
            "AND a.startTime >= :startOfDay " +
            "AND a.startTime <= :endOfDay")
    BigDecimal sumProfessionalRevenueByDateRangeAndStatus(
            @Param("companyId") String companyId,
            @Param("professionalId") String professionalId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay,
            @Param("status") AppointmentStatus status
    );

    // 👇 NOVA BUSCA PARA A MÁQUINA DE AVALIAÇÕES
    List<Appointment> findTop20ByCompanyIdAndStatusOrderByStartTimeDesc(String companyId, AppointmentStatus status);

}