package com.karthik.eks.identity.entity;

import com.karthik.eks.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Tenant entity for multi-tenant support.
 * 
 * A tenant represents a separate organization/workspace using the system.
 * 
 * Fields:
 * - name: Organization name
 * - code: Unique identifier for tenant (e.g., "acme-corp")
 * - planType: Subscription plan (FREE, PRO, ENTERPRISE)
 * - isActive: Whether tenant can access the system
 * 
 * Multi-tenancy Pattern:
 * All other entities contain a tenantId to ensure data isolation.
 * Even in a single database, we logically separate data per tenant.
 */
@Entity
@Table(name = "tenants", uniqueConstraints = {
        @UniqueConstraint(columnNames = "code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanType planType;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Subscription plan types.
     */
    public enum PlanType {
        FREE,
        PRO,
        ENTERPRISE
    }
}
