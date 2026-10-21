package com.karthik.eks.agent.service;

import com.karthik.eks.agent.domain.Workflow;
import com.karthik.eks.agent.dto.CreateWorkflowRequest;
import com.karthik.eks.agent.dto.WorkflowDto;
import com.karthik.eks.agent.repository.WorkflowRepository;
import com.karthik.eks.agent.repository.WorkflowStepRepository;
import com.karthik.eks.common.exception.BusinessException;
import com.karthik.eks.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing workflows and their execution
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowStepRepository workflowStepRepository;
    private final AgentService agentService;

    /**
     * Create a new workflow
     */
    public WorkflowDto createWorkflow(CreateWorkflowRequest request, Long tenantId, Long userId) {
        log.info("Creating new workflow: {} for agent: {} (tenant: {})", request.getName(), request.getAgentId(), tenantId);

        // Verify agent exists and belongs to tenant
        agentService.getAgentById(request.getAgentId(), tenantId);

        Workflow workflow = Workflow.builder()
            .name(request.getName())
            .description(request.getDescription())
            .agent(null)
            .inputParameters(request.getInputParameters())
            .priority(request.getPriority() != null ? request.getPriority() : "MEDIUM")
            .status("ACTIVE")
            .executionStatus("PENDING")
            .tenantId(tenantId)
            .userId(userId)
            .build();

        Workflow saved = workflowRepository.save(workflow);
        log.info("Workflow created with ID: {}", saved.getId());

        return mapToDto(saved);
    }

    /**
     * Get workflow by ID
     */
    @Transactional(readOnly = true)
    public WorkflowDto getWorkflowById(Long workflowId, Long tenantId) {
        Workflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new ResourceNotFoundException("Workflow not found with ID: " + workflowId));

        if (!workflow.getTenantId().equals(tenantId)) {
            throw new BusinessException("Workflow does not belong to tenant: " + tenantId, HttpStatus.FORBIDDEN);
        }

        return mapToDto(workflow);
    }

    /**
     * Get workflows by agent ID
     */
    @Transactional(readOnly = true)
    public List<WorkflowDto> getWorkflowsByAgent(Long agentId, Long tenantId) {
        List<Workflow> workflows = workflowRepository.findByAgentId(agentId);
        return workflows.stream()
            .filter(w -> w.getTenantId().equals(tenantId))
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    /**
     * Get workflows for a tenant
     */
    @Transactional(readOnly = true)
    public List<WorkflowDto> getWorkflowsByTenant(Long tenantId) {
        List<Workflow> workflows = workflowRepository.findByTenantId(tenantId);
        return workflows.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    /**
     * Get running workflows
     */
    @Transactional(readOnly = true)
    public List<WorkflowDto> getRunningWorkflows(Long tenantId) {
        List<Workflow> workflows = workflowRepository.findRunningWorkflows(tenantId);
        return workflows.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    /**
     * Start workflow execution
     */
    public WorkflowDto startWorkflow(Long workflowId, Long tenantId) {
        log.info("Starting workflow execution: {} (tenant: {})", workflowId, tenantId);

        Workflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new ResourceNotFoundException("Workflow not found with ID: " + workflowId));

        if (!workflow.getTenantId().equals(tenantId)) {
            throw new BusinessException("Workflow does not belong to tenant: " + tenantId, HttpStatus.FORBIDDEN);
        }

        workflow.setExecutionStatus("RUNNING");
        workflow.setExecutionStartTime(LocalDateTime.now());

        Workflow updated = workflowRepository.save(workflow);
        log.info("Workflow execution started");
        return mapToDto(updated);
    }

    /**
     * Complete workflow
     */
    public WorkflowDto completeWorkflow(Long workflowId, String result, Long tenantId) {
        log.info("Completing workflow: {} (tenant: {})", workflowId, tenantId);

        Workflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new ResourceNotFoundException("Workflow not found with ID: " + workflowId));

        if (!workflow.getTenantId().equals(tenantId)) {
            throw new BusinessException("Workflow does not belong to tenant: " + tenantId, HttpStatus.FORBIDDEN);
        }

        LocalDateTime endTime = LocalDateTime.now();
        workflow.setExecutionStatus("COMPLETED");
        workflow.setExecutionEndTime(endTime);
        workflow.setOutputResult(result);
        if (workflow.getExecutionStartTime() != null) {
            workflow.setExecutionDurationMs(
                java.time.temporal.ChronoUnit.MILLIS.between(workflow.getExecutionStartTime(), endTime)
            );
        }

        Workflow updated = workflowRepository.save(workflow);
        log.info("Workflow completed successfully");
        return mapToDto(updated);
    }

    /**
     * Fail workflow
     */
    public WorkflowDto failWorkflow(Long workflowId, String errorMessage, Long tenantId) {
        log.error("Failing workflow: {} with error: {}", workflowId, errorMessage);

        Workflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new ResourceNotFoundException("Workflow not found with ID: " + workflowId));

        if (!workflow.getTenantId().equals(tenantId)) {
            throw new BusinessException("Workflow does not belong to tenant: " + tenantId, HttpStatus.FORBIDDEN);
        }

        LocalDateTime endTime = LocalDateTime.now();
        workflow.setExecutionStatus("FAILED");
        workflow.setExecutionEndTime(endTime);
        workflow.setErrorMessage(errorMessage);
        if (workflow.getExecutionStartTime() != null) {
            workflow.setExecutionDurationMs(
                java.time.temporal.ChronoUnit.MILLIS.between(workflow.getExecutionStartTime(), endTime)
            );
        }

        Workflow updated = workflowRepository.save(workflow);
        return mapToDto(updated);
    }

    /**
     * Pause workflow
     */
    public WorkflowDto pauseWorkflow(Long workflowId, Long tenantId) {
        log.info("Pausing workflow: {} (tenant: {})", workflowId, tenantId);

        Workflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new ResourceNotFoundException("Workflow not found with ID: " + workflowId));

        if (!workflow.getTenantId().equals(tenantId)) {
            throw new BusinessException("Workflow does not belong to tenant: " + tenantId, HttpStatus.FORBIDDEN);
        }

        workflow.setExecutionStatus("PAUSED");
        Workflow updated = workflowRepository.save(workflow);
        return mapToDto(updated);
    }

    /**
     * Resume paused workflow
     */
    public WorkflowDto resumeWorkflow(Long workflowId, Long tenantId) {
        log.info("Resuming workflow: {} (tenant: {})", workflowId, tenantId);

        Workflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new ResourceNotFoundException("Workflow not found with ID: " + workflowId));

        if (!workflow.getTenantId().equals(tenantId)) {
            throw new BusinessException("Workflow does not belong to tenant: " + tenantId, HttpStatus.FORBIDDEN);
        }

        workflow.setExecutionStatus("RUNNING");
        Workflow updated = workflowRepository.save(workflow);
        return mapToDto(updated);
    }

    /**
     * Delete workflow
     */
    public void deleteWorkflow(Long workflowId, Long tenantId) {
        log.info("Deleting workflow: {} (tenant: {})", workflowId, tenantId);

        Workflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new ResourceNotFoundException("Workflow not found with ID: " + workflowId));

        if (!workflow.getTenantId().equals(tenantId)) {
            throw new BusinessException("Workflow does not belong to tenant: " + tenantId, HttpStatus.FORBIDDEN);
        }

        workflowRepository.deleteById(workflowId);
        log.info("Workflow deleted successfully");
    }

    /**
     * Map Workflow entity to WorkflowDto
     */
    private WorkflowDto mapToDto(Workflow workflow) {
        Long agentId = workflow.getAgent() != null ? workflow.getAgent().getId() : null;

        return WorkflowDto.builder()
            .id(workflow.getId())
            .name(workflow.getName())
            .description(workflow.getDescription())
            .status(workflow.getStatus())
            .executionStatus(workflow.getExecutionStatus())
            .agentId(agentId)
            .inputParameters(workflow.getInputParameters())
            .outputResult(workflow.getOutputResult())
            .errorMessage(workflow.getErrorMessage())
            .executionStartTime(workflow.getExecutionStartTime())
            .executionEndTime(workflow.getExecutionEndTime())
            .executionDurationMs(workflow.getExecutionDurationMs())
            .priority(workflow.getPriority())
            .retryCount(workflow.getRetryCount())
            .tenantId(workflow.getTenantId())
            .userId(workflow.getUserId())
            .createdAt(workflow.getCreatedAt())
            .updatedAt(workflow.getUpdatedAt())
            .build();
    }
}
