package com.karthik.eks.agent.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowDto {
    private Long id;
    private String name;
    private String description;
    private String status;
    private String executionStatus;
    private Long agentId;
    private String inputParameters;
    private String outputResult;
    private String errorMessage;
    private List<WorkflowStepDto> steps;
    private LocalDateTime executionStartTime;
    private LocalDateTime executionEndTime;
    private Long executionDurationMs;
    private String priority;
    private Integer retryCount;
    private Long tenantId;
    private Long userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
