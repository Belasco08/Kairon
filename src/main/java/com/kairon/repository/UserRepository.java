package com.kairon.repository;

import com.kairon.domain.entity.User;
import com.kairon.domain.enums.Role;
import com.kairon.security.auth.UserAuthProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /* =========================
       BASIC
    ========================== */

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /* =========================
       AUTH / DOMAIN
    ========================== */

    @Query("""
        select u
        from User u
        where u.email = :email
          and u.isActive = true
    """)
    Optional<User> findActiveUserByEmail(
            @Param("email") String email
    );

    /* =========================
       COMPANY
    ========================== */

    List<User> findByCompanyId(String companyId);

    List<User> findByCompanyIdAndRole(String companyId, Role role);

    Optional<User> findByIdAndCompanyId(String id, String companyId);

    boolean existsByEmailAndCompanyId(String email, String companyId);

    Optional<User> findByEmailAndCompanyId(String email, String companyId);

    @Query("""
        select u
        from User u
        where u.email = :email
          and u.company.id = :companyId
          and u.isActive = true
    """)
    Optional<User> findActiveUserByEmailAndCompanyId(
            @Param("email") String email,
            @Param("companyId") String companyId
    );

    /* =========================
       SPRING SECURITY (PROJECTION)
    ========================== */

    @Query("""
        select
            u.id as id,
            u.email as email,
            u.password as password,
            u.role as role,
            u.company.id as companyId,
            u.isActive as isActive
        from User u
        where u.email = :email
          and u.isActive = true
    """)
    Optional<UserAuthProjection> findAuthDataByEmail(
            @Param("email") String email
    );

    Optional<User> findByResetToken(String resetToken);
}

