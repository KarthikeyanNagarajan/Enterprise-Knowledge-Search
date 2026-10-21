package com.karthik.eks.agent.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAgentRequest {
    private String name;
    private String description;
    private String agentType;
    private String role;
    private String configuration;
    private Integer maxRetries;
    private Long executionTimeoutSeconds;
}
