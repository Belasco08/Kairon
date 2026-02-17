package com.kairon.repository;

import com.kairon.domain.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, String> {

    Optional<Company> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("SELECT c FROM Company c WHERE c.id = :id AND c.isActive = true")
    Optional<Company> findActiveById(@Param("id") String id);
}