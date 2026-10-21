package com.karthik.eks.agent.domain;

import com.karthik.eks.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "workflow_steps", indexes = {
    @Index(name = "idx_step_workflow_id", columnList = "workflow_id"),
    @Index(name = "idx_step_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowStep extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    /**
     * Step order in the workflow
     */
    @Column(nullable = false)
    private Integer stepOrder;

    /**
     * Step type: ACTION, DECISION, WAIT, PARALLEL, etc.
     */
    @Column(nullable = false, length = 50)
    private String stepType;

    /**
     * Status: PENDING, RUNNING, COMPLETED, FAILED, SKIPPED
     */
    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "PENDING";

    /**
     * Parent workflow
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    /**
     * Action to execute
     */
    @Column(columnDefinition = "TEXT")
    private String action;

    /**
     * Input for this step as JSON
     */
    @Column(columnDefinition = "TEXT")
    private String input;

    /**
     * Output from this step as JSON
     */
    @Column(columnDefinition = "TEXT")
    private String output;

    /**
     * Error message if step fails
     */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Execution start time
     */
    private LocalDateTime executionStartTime;

    /**
     * Execution end time
     */
    private LocalDateTime executionEndTime;

    /**
     * Step execution duration in milliseconds
     */
    private Long executionDurationMs;

    /**
     * Retry count for this step
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * Maximum retries allowed for this step
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer maxRetries = 3;

    /**
     * Condition for step execution (if applicable)
     */
    @Column(columnDefinition = "TEXT")
    private String condition;

    /**
     * Next step name for conditional routing
     */
    @Column(length = 100)
    private String nextStepOnSuccess;

    /**
     * Next step on failure
     */
    @Column(length = 100)
    private String nextStepOnFailure;

    /**
     * Version for optimistic locking
     */
    @Version
    private Long version;
}
