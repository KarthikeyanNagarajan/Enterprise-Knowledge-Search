package com.karthik.eks.agent.controller;

import com.karthik.eks.agent.dto.AgentDto;
import com.karthik.eks.agent.dto.CreateAgentRequest;
import com.karthik.eks.agent.service.AgentService;
import com.karthik.eks.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing agents
 */
@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
@Slf4j
public class AgentController {

    private final AgentService agentService;

    /**
     * Create a new agent
     * POST /api/v1/agents
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AgentDto>> createAgent(
        @RequestBody CreateAgentRequest request,
        Authentication authentication) {
        log.info("Create agent request: {}", request.getName());

        Long tenantId = getTenantIdFromAuth(authentication);

        AgentDto agentDto = agentService.createAgent(request, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Agent created successfully", agentDto));
    }

    /**
     * Get agent by ID
     * GET /api/v1/agents/{agentId}
     */
    @GetMapping("/{agentId}")
    public ResponseEntity<ApiResponse<AgentDto>> getAgent(
        @PathVariable Long agentId,
        Authentication authentication) {
        log.info("Get agent request: {}", agentId);

        Long tenantId = getTenantIdFromAuth(authentication);
        AgentDto agentDto = agentService.getAgentById(agentId, tenantId);

        return ResponseEntity.ok(ApiResponse.success("Agent retrieved successfully", agentDto));
    }

    /**
     * Get all agents for tenant
     * GET /api/v1/agents
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AgentDto>>> getAllAgents(
        Authentication authentication) {
        log.info("Get all agents request");

        Long tenantId = getTenantIdFromAuth(authentication);
        List<AgentDto> agents = agentService.getAgentsByTenant(tenantId);

        return ResponseEntity.ok(ApiResponse.success("Agents retrieved successfully", agents));
    }

    /**
     * Get active agents
     * GET /api/v1/agents/active
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<AgentDto>>> getActiveAgents(
        Authentication authentication) {
        log.info("Get active agents request");

        Long tenantId = getTenantIdFromAuth(authentication);
        List<AgentDto> agents = agentService.getActiveAgents(tenantId);

        return ResponseEntity.ok(ApiResponse.success("Active agents retrieved successfully", agents));
    }

    /**
     * Get agents by role
     * GET /api/v1/agents/role/{role}
     */
    @GetMapping("/role/{role}")
    public ResponseEntity<ApiResponse<List<AgentDto>>> getAgentsByRole(
        @PathVariable String role,
        Authentication authentication) {
        log.info("Get agents by role: {}", role);

        Long tenantId = getTenantIdFromAuth(authentication);
        List<AgentDto> agents = agentService.getAgentsByRole(role, tenantId);

        return ResponseEntity.ok(ApiResponse.success("Agents retrieved successfully", agents));
    }

    /**
     * Update agent
     * PUT /api/v1/agents/{agentId}
     */
    @PutMapping("/{agentId}")
    public ResponseEntity<ApiResponse<AgentDto>> updateAgent(
        @PathVariable Long agentId,
        @RequestBody CreateAgentRequest request,
        Authentication authentication) {
        log.info("Update agent request: {}", agentId);

        Long tenantId = getTenantIdFromAuth(authentication);
        AgentDto agentDto = agentService.updateAgent(agentId, request, tenantId);

        return ResponseEntity.ok(ApiResponse.success("Agent updated successfully", agentDto));
    }

    /**
     * Deactivate agent
     * PATCH /api/v1/agents/{agentId}/deactivate
     */
    @PatchMapping("/{agentId}/deactivate")
    public ResponseEntity<ApiResponse<String>> deactivateAgent(
        @PathVariable Long agentId,
        Authentication authentication) {
        log.info("Deactivate agent request: {}", agentId);

        Long tenantId = getTenantIdFromAuth(authentication);
        agentService.deactivateAgent(agentId, tenantId);

        return ResponseEntity.ok(ApiResponse.success("Agent deactivated successfully", "Agent deactivated"));
    }

    /**
     * Delete agent
     * DELETE /api/v1/agents/{agentId}
     */
    @DeleteMapping("/{agentId}")
    public ResponseEntity<ApiResponse<String>> deleteAgent(
        @PathVariable Long agentId,
        Authentication authentication) {
        log.info("Delete agent request: {}", agentId);

        Long tenantId = getTenantIdFromAuth(authentication);
        agentService.deleteAgent(agentId, tenantId);

        return ResponseEntity.ok(ApiResponse.success("Agent deleted successfully", "Agent deleted"));
    }

    /**
     * Extract tenant ID from authentication context
     * This should be implemented based on your authentication provider
     */
    private Long getTenantIdFromAuth(Authentication authentication) {
        return 1L;
    }
}
