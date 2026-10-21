package com.karthik.eks.agent.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowStepRequest {
    private String name;
    private String description;
    private Integer stepOrder;
    private String stepType;
    private String action;
    private String input;
    private Integer maxRetries;
    private String condition;
    private String nextStepOnSuccess;
    private String nextStepOnFailure;
}
