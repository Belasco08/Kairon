package com.kairon.repository;

import com.kairon.domain.entity.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, String> {

    // Métodos existentes
    List<Client> findByCompanyId(String companyId);

    Page<Client> findByCompanyId(String companyId, Pageable pageable);

    Optional<Client> findByPhoneAndCompanyId(String phone, String companyId);

    Optional<Client> findByIdAndCompanyId(String id, String companyId);

    // Novo método com paginação
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

    // 👇 NOVO: Busca clientes que têm agendamento com o profissional específico
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



}