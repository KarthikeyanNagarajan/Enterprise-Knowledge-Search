package com.karthik.eks.agent.domain;

import com.karthik.eks.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "agents", indexes = {
    @Index(name = "idx_agent_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_agent_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agent extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    /**
     * Agent type: AUTONOMOUS, REACTIVE, HIERARCHICAL, etc.
     */
    @Column(nullable = false, length = 50)
    private String agentType;

    /**
     * Agent status: ACTIVE, INACTIVE, ARCHIVED
     */
    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "ACTIVE";

    /**
     * Agent capability/role: DOCUMENT_PROCESSOR, QUERY_ANALYZER, DATA_TRANSFORMER, etc.
     */
    @Column(nullable = false, length = 100)
    private String role;

    /**
     * Configuration as JSON string
     */
    @Column(columnDefinition = "TEXT")
    private String configuration;

    /**
     * Maximum retry attempts for workflows
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer maxRetries = 3;

    /**
     * Timeout in seconds for workflow execution
     */
    @Column(nullable = false)
    @Builder.Default
    private Long executionTimeoutSeconds = 300L;

    /**
     * Tenant ID for multi-tenancy support
     */
    @Column(nullable = false)
    private Long tenantId;

    /**
     * Associated workflows
     */
    @OneToMany(mappedBy = "agent", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Workflow> workflows = new ArrayList<>();

    /**
     * Version for optimistic locking
     */
    @Version
    private Long version;
}
