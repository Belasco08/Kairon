package com.kairon.repository;

import com.kairon.domain.entity.Services;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRepository extends JpaRepository<Services, String> {

    List<Services> findByCompanyId(String companyId);


    // ✅ MÉTODO MÁGICO: Resolvemos todos os filtros de uma vez só
    // O uso de (:param IS NULL OR s.campo = :param) faz o filtro ser opcional
    @Query("SELECT s FROM Services s WHERE s.company.id = :companyId " +
            "AND (:category IS NULL OR s.category = :category) " +
            "AND (:isActive IS NULL OR s.isActive = :isActive) " +
            "AND (:onlineBooking IS NULL OR s.onlineBooking = :onlineBooking) " +
            "AND (:professionalId IS NULL OR s.professional.id = :professionalId)")
    Page<Services> searchServices(
            @Param("companyId") String companyId,
            @Param("category") String category,
            @Param("isActive") Boolean isActive,
            @Param("onlineBooking") Boolean onlineBooking,
            @Param("professionalId") String professionalId,
            Pageable pageable
    );

    // Queries Auxiliares (Mantive as que parecem ser usadas em outros lugares ou validações)
    Optional<Services> findByIdAndCompanyId(String id, String companyId);

    @Query("SELECT s FROM Services s WHERE s.company.id = :companyId AND s.isActive = true AND s.onlineBooking = true")
    List<Services> findActiveAndOnlineByCompanyId(@Param("companyId") String companyId);

    @Query("SELECT s FROM Services s WHERE s.company.id = :companyId " +
            "AND (s.professional.id = :professionalId OR s.professional IS NULL) " +
            "AND s.isActive = true")
    List<Services> findAvailableByProfessionalId(@Param("companyId") String companyId, @Param("professionalId") String professionalId);

    // Métodos para validação de existência (mais leves que buscar a entidade toda)
    boolean existsByProfessionalIdAndCompanyId(String professionalId, String companyId);

    // Query de Dashboard (Mantida intacta)
    @Query("""
                SELECT
                    s.id,
                    s.name,
                    SUM(
                        CASE
                            WHEN fr.type IN ('APPOINTMENT', 'ADJUSTMENT')
                            THEN fr.amount
                            ELSE 0
                        END
                    ) AS revenue,
                    COUNT(DISTINCT a.id) AS count
                FROM AppointmentItem ai
                JOIN ai.service s
                JOIN ai.appointment a
                JOIN a.financialRecords fr
                WHERE s.company.id = :companyId
                  AND a.completedAt >= :startDate
                  AND a.completedAt < :endDate
                GROUP BY s.id, s.name
                ORDER BY revenue DESC
            """)
    List<Object[]> findTopServicesByRevenue(
            @Param("companyId") String companyId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    // Em ServiceRepository.java

    // Top Serviços do Profissional
    @Query("""
        SELECT s.id, s.name, SUM(ai.price), COUNT(ai.id)
        FROM AppointmentItem ai
        JOIN ai.service s
        JOIN ai.appointment a
        WHERE a.company.id = :companyId
          AND a.professional.id = :professionalId
          AND a.status = 'COMPLETED'
          AND a.startTime >= :startDate
          AND a.startTime <= :endDate
        GROUP BY s.id, s.name
        ORDER BY SUM(ai.price) DESC
    """)
    List<Object[]> findTopServicesByRevenueAndProfessional(
            @Param("companyId") String companyId,
            @Param("professionalId") String professionalId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
}