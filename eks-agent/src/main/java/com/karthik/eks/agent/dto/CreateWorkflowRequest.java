package com.karthik.eks.agent.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateWorkflowRequest {
    private String name;
    private String description;
    private Long agentId;
    private String inputParameters;
    private String priority;
    private List<WorkflowStepRequest> steps;
}
