package com.kairon.repository;

import com.kairon.domain.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, String> {
    List<Product> findByCompanyIdAndIsActiveTrue(String companyId);
}