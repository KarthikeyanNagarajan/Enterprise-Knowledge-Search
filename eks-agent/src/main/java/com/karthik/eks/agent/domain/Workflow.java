package com.karthik.eks.agent.domain;

import com.karthik.eks.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "workflows", indexes = {
    @Index(name = "idx_workflow_agent_id", columnList = "agent_id"),
    @Index(name = "idx_workflow_status", columnList = "status"),
    @Index(name = "idx_workflow_tenant_id", columnList = "tenant_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Workflow extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    /**
     * Workflow status: DRAFT, ACTIVE, ARCHIVED, DEPRECATED
     */
    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "DRAFT";

    /**
     * Execution status: PENDING, RUNNING, COMPLETED, FAILED, PAUSED
     */
    @Column(nullable = false, length = 50)
    @Builder.Default
    private String executionStatus = "PENDING";

    /**
     * Parent Agent
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    /**
     * Input parameters as JSON
     */
    @Column(columnDefinition = "TEXT")
    private String inputParameters;

    /**
     * Output result as JSON
     */
    @Column(columnDefinition = "TEXT")
    private String outputResult;

    /**
     * Error message if workflow fails
     */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Workflow steps
     */
    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<WorkflowStep> steps = new ArrayList<>();

    /**
     * Execution start time
     */
    private LocalDateTime executionStartTime;

    /**
     * Execution end time
     */
    private LocalDateTime executionEndTime;

    /**
     * Execution duration in milliseconds
     */
    private Long executionDurationMs;

    /**
     * Priority: LOW, MEDIUM, HIGH, CRITICAL
     */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String priority = "MEDIUM";

    /**
     * Number of retry attempts
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * Tenant ID for multi-tenancy
     */
    @Column(nullable = false)
    private Long tenantId;

    /**
     * User ID who triggered the workflow
     */
    @Column(nullable = false)
    private Long userId;

    /**
     * Version for optimistic locking
     */
    @Version
    private Long version;
}
