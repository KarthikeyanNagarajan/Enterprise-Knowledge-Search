package com.karthik.eks.identity.service;

import com.karthik.eks.common.exception.BusinessException;
import com.karthik.eks.common.exception.ResourceNotFoundException;
import com.karthik.eks.identity.entity.Tenant;
import com.karthik.eks.identity.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tenant Service.
 * 
 * Responsibilities:
 * - Tenant creation and management
 * - Tenant context provisioning
 * - Plan-based feature access control
 * 
 * Architecture Pattern:
 * TenantContext (ThreadLocal) stores current tenant during request.
 * All requests must be scoped to a tenant to ensure isolation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private static final ThreadLocal<Tenant> tenantContext = new ThreadLocal<>();

    /**
     * Create a new tenant.
     * 
     * Business Logic:
     * - Code must be unique and URL-safe
     * - Code cannot be changed after creation
     * - Initial plan type determines feature access
     * 
     * @param name Organization name
     * @param code Unique code (slug)
     * @param planType Subscription plan type
     * @return Created tenant
     */
    @Transactional
    public Tenant createTenant(String name, String code, Tenant.PlanType planType) {
        log.info("Creating tenant: {} with code: {}", name, code);

        // Validate code uniqueness
        if (tenantRepository.existsByCode(code)) {
            throw new BusinessException("Tenant code already exists: " + code, HttpStatus.BAD_REQUEST);
        }

        Tenant tenant = Tenant.builder()
                .name(name)
                .code(code)
                .planType(planType)
                .isActive(true)
                .build();
        tenant.setCreatedBy("SYSTEM");
        tenant.setUpdatedBy("SYSTEM");

        Tenant saved = tenantRepository.save(tenant);
        log.info("Tenant created successfully with ID: {}", saved.getId());
        return saved;
    }

    /**
     * Get tenant by ID.
     * 
     * @param tenantId the tenant ID
     * @return Tenant entity
     * @throws ResourceNotFoundException if tenant not found
     */
    @Transactional(readOnly = true)
    public Tenant getTenantById(Long tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + tenantId));
    }

    /**
     * Get tenant by code.
     * 
     * @param code the tenant code
     * @return Tenant entity
     * @throws ResourceNotFoundException if tenant not found
     */
    @Transactional(readOnly = true)
    public Tenant getTenantByCode(String code) {
        return tenantRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + code));
    }

    /**
     * Set current tenant context for the request.
     * 
     * This uses ThreadLocal to store the tenant for the current request.
     * Must be called at the beginning of request processing.
     * 
     * @param tenant the current tenant
     */
    public void setCurrentTenant(Tenant tenant) {
        tenantContext.set(tenant);
    }

    /**
     * Get current tenant context.
     * 
     * @return Current tenant for this request
     * @throws IllegalStateException if tenant context not set
     */
    public Tenant getCurrentTenant() {
        Tenant tenant = tenantContext.get();
        if (tenant == null) {
            throw new IllegalStateException("Tenant context not set");
        }
        return tenant;
    }

    /**
     * Clear tenant context (call at end of request).
     */
    public void clearTenantContext() {
        tenantContext.remove();
    }
}
