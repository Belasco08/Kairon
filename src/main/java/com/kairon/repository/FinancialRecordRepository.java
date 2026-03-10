package com.kairon.repository;

import com.kairon.domain.entity.FinancialRecord;
import com.kairon.domain.enums.FinancialType;
import com.kairon.dto.response.CategorySumResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialRecordRepository extends JpaRepository<FinancialRecord, String> {

    /* =========================
       BÁSICOS
       ========================= */

    List<FinancialRecord> findByCompanyId(String companyId);
    Page<FinancialRecord> findByCompanyId(String companyId, Pageable pageable);

    List<FinancialRecord> findByCompanyIdAndReferenceDateBetween(String companyId, LocalDateTime start, LocalDateTime end);

    List<FinancialRecord> findByCompanyIdAndProfessionalId(String companyId, String professionalId);

    // Para o Dono: Vê tudo da empresa
    List<FinancialRecord> findByCompanyIdOrderByReferenceDateDesc(String companyId);

    // Para o Profissional: Vê só o dele
    List<FinancialRecord> findByProfessionalIdOrderByReferenceDateDesc(String professionalId);

    Optional<FinancialRecord> findByIdAndCompanyId(String id, String companyId);

    /* =========================
       SOMAS (DASHBOARD) - SOMENTE CONTAS 'PAID'
       ========================= */

    // 1. Soma Global (Para o Owner)
    @Query("""
        SELECT COALESCE(SUM(fr.amount), 0)
        FROM FinancialRecord fr
        WHERE fr.company.id = :companyId
          AND fr.type IN :types
          AND fr.status = :status
          AND fr.referenceDate >= :startDate
          AND fr.referenceDate < :endDate
    """)
    BigDecimal sumAmountByDateRangeInternal(
            @Param("companyId") String companyId,
            @Param("types") List<FinancialType> types,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") String status
    );

    default BigDecimal sumAmountByDateRange(String companyId, List<FinancialType> types, LocalDateTime startDate, LocalDateTime endDate) {
        return sumAmountByDateRangeInternal(companyId, types, startDate, endDate, "PAID");
    }

    // 2. Soma por Profissional (Para o Profissional)
    @Query("""
        SELECT COALESCE(SUM(fr.amount), 0)
        FROM FinancialRecord fr
        WHERE fr.company.id = :companyId
          AND fr.professional.id = :professionalId
          AND fr.type IN :types
          AND fr.status = :status
          AND fr.referenceDate >= :startDate
          AND fr.referenceDate < :endDate
    """)
    BigDecimal sumAmountByProfessionalAndDateRangeInternal(
            @Param("companyId") String companyId,
            @Param("professionalId") String professionalId,
            @Param("types") List<FinancialType> types,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") String status
    );

    default BigDecimal sumAmountByProfessionalAndDateRange(String companyId, String professionalId, List<FinancialType> types, LocalDateTime startDate, LocalDateTime endDate) {
        return sumAmountByProfessionalAndDateRangeInternal(companyId, professionalId, types, startDate, endDate, "PAID");
    }

    /* =========================
       GRÁFICOS (DASHBOARD) - SOMENTE CONTAS 'PAID'
       ========================= */

    // 1. Gráfico Global (Owner)
    @Query("""
        SELECT
            DATE(fr.referenceDate),
            COALESCE(SUM(fr.amount), 0),
            COUNT(DISTINCT fr.id)
        FROM FinancialRecord fr
        WHERE fr.company.id = :companyId
          AND fr.referenceDate >= :startDate
          AND fr.referenceDate < :endDate
          AND fr.type IN :types
          AND fr.status = :status
        GROUP BY DATE(fr.referenceDate)
        ORDER BY DATE(fr.referenceDate)
    """)
    List<Object[]> findDailyRevenueInternal(
            @Param("companyId") String companyId,
            @Param("types") List<FinancialType> types,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") String status
    );

    default List<Object[]> findDailyRevenue(String companyId, List<FinancialType> types, LocalDateTime startDate, LocalDateTime endDate) {
        return findDailyRevenueInternal(companyId, types, startDate, endDate, "PAID");
    }

    // 2. Gráfico Filtrado (Profissional)
    @Query("""
        SELECT
            DATE(fr.referenceDate),
            COALESCE(SUM(fr.amount), 0),
            COUNT(DISTINCT fr.id)
        FROM FinancialRecord fr
        WHERE fr.company.id = :companyId
          AND fr.professional.id = :professionalId 
          AND fr.referenceDate >= :startDate
          AND fr.referenceDate < :endDate
          AND fr.type IN :types
          AND fr.status = :status
        GROUP BY DATE(fr.referenceDate)
        ORDER BY DATE(fr.referenceDate)
    """)
    List<Object[]> findDailyRevenueByProfessionalInternal(
            @Param("companyId") String companyId,
            @Param("professionalId") String professionalId,
            @Param("types") List<FinancialType> types,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") String status
    );

    default List<Object[]> findDailyRevenueByProfessional(String companyId, String professionalId, List<FinancialType> types, LocalDateTime startDate, LocalDateTime endDate) {
        return findDailyRevenueByProfessionalInternal(companyId, professionalId, types, startDate, endDate, "PAID");
    }

    // SOMA DESPESAS POR CATEGORIA
    @Query("SELECT new com.kairon.dto.response.CategorySumResponse(r.category, SUM(r.amount)) " +
            "FROM FinancialRecord r " +
            "WHERE r.company.id = :companyId " +
            "AND r.type = 'EXPENSE' " +
            "AND r.status = :status " +
            "AND r.referenceDate BETWEEN :start AND :end " +
            "GROUP BY r.category")
    List<CategorySumResponse> sumExpensesByCategoryInternal(
            @Param("companyId") String companyId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("status") String status
    );

    default List<CategorySumResponse> sumExpensesByCategory(String companyId, LocalDateTime start, LocalDateTime end) {
        return sumExpensesByCategoryInternal(companyId, start, end, "PAID");
    }
}