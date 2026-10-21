package com.karthik.eks.agent.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowStepDto {
    private Long id;
    private String name;
    private String description;
    private Integer stepOrder;
    private String stepType;
    private String status;
    private Long workflowId;
    private String action;
    private String input;
    private String output;
    private String errorMessage;
    private LocalDateTime executionStartTime;
    private LocalDateTime executionEndTime;
    private Long executionDurationMs;
    private Integer retryCount;
    private Integer maxRetries;
    private String condition;
    private String nextStepOnSuccess;
    private String nextStepOnFailure;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
