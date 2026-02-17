package com.kairon.repository;

import com.kairon.domain.entity.Professional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProfessionalRepository extends JpaRepository<Professional, String> {

    long countByCompanyId(String companyId);

    List<Professional> findByCompanyId(String companyId);

    List<Professional> findByCompanyIdAndIsActive(String companyId, boolean isActive);

    Optional<Professional> findByIdAndCompanyId(String id, String companyId);


    @Query("SELECT p FROM Professional p WHERE p.company.id = :companyId AND p.isActive = true")
    List<Professional> findActiveByCompanyId(@Param("companyId") String companyId);

    Optional<Professional> findByUserId(String userId);
}