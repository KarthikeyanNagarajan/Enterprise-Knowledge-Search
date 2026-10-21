package com.karthik.eks.agent.service;

import com.karthik.eks.agent.domain.Agent;
import com.karthik.eks.agent.dto.AgentDto;
import com.karthik.eks.agent.dto.CreateAgentRequest;
import com.karthik.eks.agent.repository.AgentRepository;
import com.karthik.eks.common.exception.BusinessException;
import com.karthik.eks.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing agents
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AgentService {

    private final AgentRepository agentRepository;

    /**
     * Create a new agent
     */
    public AgentDto createAgent(CreateAgentRequest request, Long tenantId) {
        log.info("Creating new agent: {} for tenant: {}", request.getName(), tenantId);

        if (agentRepository.existsByNameAndTenantId(request.getName(), tenantId)) {
            throw new BusinessException("Agent with name '" + request.getName() + "' already exists", HttpStatus.BAD_REQUEST);
        }

        Agent agent = Agent.builder()
            .name(request.getName())
            .description(request.getDescription())
            .agentType(request.getAgentType())
            .role(request.getRole())
            .configuration(request.getConfiguration())
            .maxRetries(request.getMaxRetries() != null ? request.getMaxRetries() : 3)
            .executionTimeoutSeconds(request.getExecutionTimeoutSeconds() != null ? request.getExecutionTimeoutSeconds() : 300L)
            .status("ACTIVE")
            .tenantId(tenantId)
            .build();

        Agent saved = agentRepository.save(agent);
        log.info("Agent created with ID: {}", saved.getId());
        return mapToDto(saved);
    }

    /**
     * Get agent by ID
     */
    @Transactional(readOnly = true)
    public AgentDto getAgentById(Long agentId, Long tenantId) {
        Agent agent = agentRepository.findById(agentId)
            .orElseThrow(() -> new ResourceNotFoundException("Agent not found with ID: " + agentId));

        if (!agent.getTenantId().equals(tenantId)) {
            throw new BusinessException("Agent does not belong to tenant: " + tenantId, HttpStatus.FORBIDDEN);
        }

        return mapToDto(agent);
    }

    /**
     * Get all agents for a tenant
     */
    @Transactional(readOnly = true)
    public List<AgentDto> getAgentsByTenant(Long tenantId) {
        log.debug("Fetching agents for tenant: {}", tenantId);
        List<Agent> agents = agentRepository.findByTenantId(tenantId);
        return agents.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    /**
     * Get active agents for a tenant
     */
    @Transactional(readOnly = true)
    public List<AgentDto> getActiveAgents(Long tenantId) {
        List<Agent> agents = agentRepository.findByTenantIdAndStatus(tenantId, "ACTIVE");
        return agents.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    /**
     * Get agents by role
     */
    @Transactional(readOnly = true)
    public List<AgentDto> getAgentsByRole(String role, Long tenantId) {
        List<Agent> agents = agentRepository.findByRoleAndStatus(role, "ACTIVE");
        return agents.stream()
            .filter(a -> a.getTenantId().equals(tenantId))
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    /**
     * Update agent
     */
    public AgentDto updateAgent(Long agentId, CreateAgentRequest request, Long tenantId) {
        log.info("Updating agent with ID: {}", agentId);

        Agent agent = agentRepository.findById(agentId)
            .orElseThrow(() -> new ResourceNotFoundException("Agent not found with ID: " + agentId));

        if (!agent.getTenantId().equals(tenantId)) {
            throw new BusinessException("Agent does not belong to tenant: " + tenantId, HttpStatus.FORBIDDEN);
        }

        agent.setName(request.getName());
        agent.setDescription(request.getDescription());
        agent.setAgentType(request.getAgentType());
        agent.setRole(request.getRole());
        agent.setConfiguration(request.getConfiguration());
        if (request.getMaxRetries() != null) {
            agent.setMaxRetries(request.getMaxRetries());
        }
        if (request.getExecutionTimeoutSeconds() != null) {
            agent.setExecutionTimeoutSeconds(request.getExecutionTimeoutSeconds());
        }

        Agent updated = agentRepository.save(agent);
        log.info("Agent updated successfully");
        return mapToDto(updated);
    }

    /**
     * Deactivate agent
     */
    public void deactivateAgent(Long agentId, Long tenantId) {
        log.info("Deactivating agent with ID: {}", agentId);

        Agent agent = agentRepository.findById(agentId)
            .orElseThrow(() -> new ResourceNotFoundException("Agent not found with ID: " + agentId));

        if (!agent.getTenantId().equals(tenantId)) {
            throw new BusinessException("Agent does not belong to tenant: " + tenantId, HttpStatus.FORBIDDEN);
        }

        agent.setStatus("INACTIVE");
        agentRepository.save(agent);
        log.info("Agent deactivated successfully");
    }

    /**
     * Delete agent
     */
    public void deleteAgent(Long agentId, Long tenantId) {
        log.info("Deleting agent with ID: {}", agentId);

        Agent agent = agentRepository.findById(agentId)
            .orElseThrow(() -> new ResourceNotFoundException("Agent not found with ID: " + agentId));

        if (!agent.getTenantId().equals(tenantId)) {
            throw new BusinessException("Agent does not belong to tenant: " + tenantId, HttpStatus.FORBIDDEN);
        }

        agentRepository.deleteById(agentId);
        log.info("Agent deleted successfully");
    }

    /**
     * Map Agent entity to AgentDto
     */
    private AgentDto mapToDto(Agent agent) {
        return AgentDto.builder()
            .id(agent.getId())
            .name(agent.getName())
            .description(agent.getDescription())
            .agentType(agent.getAgentType())
            .status(agent.getStatus())
            .role(agent.getRole())
            .configuration(agent.getConfiguration())
            .maxRetries(agent.getMaxRetries())
            .executionTimeoutSeconds(agent.getExecutionTimeoutSeconds())
            .tenantId(agent.getTenantId())
            .createdAt(agent.getCreatedAt())
            .updatedAt(agent.getUpdatedAt())
            .build();
    }
}
