package com.karthik.eks.agent.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentDto {
    private Long id;
    private String name;
    private String description;
    private String agentType;
    private String status;
    private String role;
    private String configuration;
    private Integer maxRetries;
    private Long executionTimeoutSeconds;
    private Long tenantId;
    private List<WorkflowDto> workflows;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
