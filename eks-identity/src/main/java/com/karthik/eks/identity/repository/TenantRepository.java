package com.karthik.eks.identity.repository;

import com.karthik.eks.identity.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Tenant entity.
 * 
 * Provides database access for tenant operations.
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {

    /**
     * Find tenant by code (unique identifier).
     */
    Optional<Tenant> findByCode(String code);

    /**
     * Check if tenant code already exists.
     */
    boolean existsByCode(String code);

    /**
     * Find tenant by name.
     */
    Optional<Tenant> findByName(String name);
}
