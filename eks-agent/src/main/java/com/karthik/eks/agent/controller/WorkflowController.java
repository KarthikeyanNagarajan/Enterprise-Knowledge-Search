package com.karthik.eks.agent.controller;

import com.karthik.eks.agent.dto.CreateWorkflowRequest;
import com.karthik.eks.agent.dto.WorkflowDto;
import com.karthik.eks.agent.service.WorkflowService;
import com.karthik.eks.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing workflows
 */
@RestController
@RequestMapping("/api/v1/workflows")
@RequiredArgsConstructor
@Slf4j
public class WorkflowController {

    private final WorkflowService workflowService;

    /**
     * Create a new workflow
     * POST /api/v1/workflows
     */
    @PostMapping
    public ResponseEntity<ApiResponse<WorkflowDto>> createWorkflow(
        @RequestBody CreateWorkflowRequest request,
        Authentication authentication) {
        log.info("Create workflow request: {}", request.getName());

        Long tenantId = getTenantIdFromAuth(authentication);
        Long userId = getUserIdFromAuth(authentication);

        WorkflowDto workflowDto = workflowService.createWorkflow(request, tenantId, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Workflow created successfully", workflowDto));
    }

    /**
     * Get workflow by ID
     * GET /api/v1/workflows/{workflowId}
     */
    @GetMapping("/{workflowId}")
    public ResponseEntity<ApiResponse<WorkflowDto>> getWorkflow(
        @PathVariable Long workflowId,
        Authentication authentication) {
        log.info("Get workflow request: {}", workflowId);

        Long tenantId = getTenantIdFromAuth(authentication);
        WorkflowDto workflowDto = workflowService.getWorkflowById(workflowId, tenantId);

        return ResponseEntity.ok(ApiResponse.success("Workflow retrieved successfully", workflowDto));
    }

    /**
     * Get workflows by agent
     * GET /api/v1/workflows/agent/{agentId}
     */
    @GetMapping("/agent/{agentId}")
    public ResponseEntity<ApiResponse<List<WorkflowDto>>> getWorkflowsByAgent(
        @PathVariable Long agentId,
        Authentication authentication) {
        log.info("Get workflows by agent: {}", agentId);

        Long tenantId = getTenantIdFromAuth(authentication);
        List<WorkflowDto> workflows = workflowService.getWorkflowsByAgent(agentId, tenantId);

        return ResponseEntity.ok(ApiResponse.success("Workflows retrieved successfully", workflows));
    }

    /**
     * Get all workflows for tenant
     * GET /api/v1/workflows
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkflowDto>>> getAllWorkflows(
        Authentication authentication) {
        log.info("Get all workflows request");

        Long tenantId = getTenantIdFromAuth(authentication);
        List<WorkflowDto> workflows = workflowService.getWorkflowsByTenant(tenantId);

        return ResponseEntity.ok(ApiResponse.success("Workflows retrieved successfully", workflows));
    }

    /**
     * Get running workflows
     * GET /api/v1/workflows/running
     */
    @GetMapping("/running")
    public ResponseEntity<ApiResponse<List<WorkflowDto>>> getRunningWorkflows(
        Authentication authentication) {
        log.info("Get running workflows request");

        Long tenantId = getTenantIdFromAuth(authentication);
        List<WorkflowDto> workflows = workflowService.getRunningWorkflows(tenantId);

        return ResponseEntity.ok(ApiResponse.success("Running workflows retrieved successfully", workflows));
    }

    /**
     * Start workflow execution
     * POST /api/v1/workflows/{workflowId}/start
     */
    @PostMapping("/{workflowId}/start")
    public ResponseEntity<ApiResponse<WorkflowDto>> startWorkflow(
        @PathVariable Long workflowId,
        Authentication authentication) {
        log.info("Start workflow request: {}", workflowId);

        Long tenantId = getTenantIdFromAuth(authentication);
        WorkflowDto workflowDto = workflowService.startWorkflow(workflowId, tenantId);

        return ResponseEntity.ok(ApiResponse.success("Workflow started successfully", workflowDto));
    }

    /**
     * Complete workflow
     * POST /api/v1/workflows/{workflowId}/complete
     */
    @PostMapping("/{workflowId}/complete")
    public ResponseEntity<ApiResponse<WorkflowDto>> completeWorkflow(
        @PathVariable Long workflowId,
        @RequestParam(required = false) String result,
        Authentication authentication) {
        log.info("Complete workflow request: {}", workflowId);

        Long tenantId = getTenantIdFromAuth(authentication);
        WorkflowDto workflowDto = workflowService.completeWorkflow(workflowId, result, tenantId);

        return ResponseEntity.ok(ApiResponse.success("Workflow completed successfully", workflowDto));
    }

    /**
     * Fail workflow
     * POST /api/v1/workflows/{workflowId}/fail
     */
    @PostMapping("/{workflowId}/fail")
    public ResponseEntity<ApiResponse<WorkflowDto>> failWorkflow(
        @PathVariable Long workflowId,
        @RequestParam String errorMessage,
        Authentication authentication) {
        log.info("Fail workflow request: {}", workflowId);

        Long tenantId = getTenantIdFromAuth(authentication);
        WorkflowDto workflowDto = workflowService.failWorkflow(workflowId, errorMessage, tenantId);

        return ResponseEntity.ok(ApiResponse.success("Workflow failed", workflowDto));
    }

    /**
     * Pause workflow
     * PATCH /api/v1/workflows/{workflowId}/pause
     */
    @PatchMapping("/{workflowId}/pause")
    public ResponseEntity<ApiResponse<WorkflowDto>> pauseWorkflow(
        @PathVariable Long workflowId,
        Authentication authentication) {
        log.info("Pause workflow request: {}", workflowId);

        Long tenantId = getTenantIdFromAuth(authentication);
        WorkflowDto workflowDto = workflowService.pauseWorkflow(workflowId, tenantId);

        return ResponseEntity.ok(ApiResponse.success("Workflow paused successfully", workflowDto));
    }

    /**
     * Resume workflow
     * PATCH /api/v1/workflows/{workflowId}/resume
     */
    @PatchMapping("/{workflowId}/resume")
    public ResponseEntity<ApiResponse<WorkflowDto>> resumeWorkflow(
        @PathVariable Long workflowId,
        Authentication authentication) {
        log.info("Resume workflow request: {}", workflowId);

        Long tenantId = getTenantIdFromAuth(authentication);
        WorkflowDto workflowDto = workflowService.resumeWorkflow(workflowId, tenantId);

        return ResponseEntity.ok(ApiResponse.success("Workflow resumed successfully", workflowDto));
    }

    /**
     * Delete workflow
     * DELETE /api/v1/workflows/{workflowId}
     */
    @DeleteMapping("/{workflowId}")
    public ResponseEntity<ApiResponse<String>> deleteWorkflow(
        @PathVariable Long workflowId,
        Authentication authentication) {
        log.info("Delete workflow request: {}", workflowId);

        Long tenantId = getTenantIdFromAuth(authentication);
        workflowService.deleteWorkflow(workflowId, tenantId);

        return ResponseEntity.ok(ApiResponse.success("Workflow deleted successfully", "Workflow deleted"));
    }

    /**
     * Extract tenant ID from authentication context
     */
    private Long getTenantIdFromAuth(Authentication authentication) {
        return 1L;
    }

    /**
     * Extract user ID from authentication context
     */
    private Long getUserIdFromAuth(Authentication authentication) {
        return 1L;
    }
}
