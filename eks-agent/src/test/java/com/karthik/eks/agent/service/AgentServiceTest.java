package com.karthik.eks.agent.service;

import com.karthik.eks.agent.domain.Agent;
import com.karthik.eks.agent.dto.AgentDto;
import com.karthik.eks.agent.dto.CreateAgentRequest;
import com.karthik.eks.agent.repository.AgentRepository;
import com.karthik.eks.common.exception.BusinessException;
import com.karthik.eks.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentServiceTest {

    @Mock
    private AgentRepository agentRepository;

    @InjectMocks
    private AgentService agentService;

    @Test
    void createAgentShouldPersistNewAgent() {
        CreateAgentRequest request = new CreateAgentRequest();
        request.setName("Planner");
        request.setDescription("Planning agent");
        request.setAgentType("AUTONOMOUS");
        request.setRole("QUERY_ANALYZER");
        request.setConfiguration("{}") ;
        request.setMaxRetries(3);
        request.setExecutionTimeoutSeconds(120L);

        Agent saved = Agent.builder()
                .name("Planner")
                .description("Planning agent")
                .agentType("AUTONOMOUS")
                .role("QUERY_ANALYZER")
                .configuration("{}")
                .maxRetries(3)
                .executionTimeoutSeconds(120L)
                .status("ACTIVE")
                .tenantId(7L)
                .build();
        saved.setId(1L);

        when(agentRepository.existsByNameAndTenantId("Planner", 7L)).thenReturn(false);
        when(agentRepository.save(any(Agent.class))).thenReturn(saved);

        AgentDto result = agentService.createAgent(request, 7L);

        assertEquals("Planner", result.getName());
        assertEquals("ACTIVE", result.getStatus());
        verify(agentRepository).save(any(Agent.class));
    }

    @Test
    void createAgentShouldRejectDuplicateName() {
        CreateAgentRequest request = new CreateAgentRequest();
        request.setName("Planner");
        request.setDescription("Planning agent");
        request.setAgentType("AUTONOMOUS");
        request.setRole("QUERY_ANALYZER");
        request.setConfiguration("{}") ;

        when(agentRepository.existsByNameAndTenantId("Planner", 7L)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> agentService.createAgent(request, 7L));

        assertEquals("Agent with name 'Planner' already exists", ex.getMessage());
    }

    @Test
    void getAgentByIdShouldRejectWrongTenant() {
        Agent agent = Agent.builder()
                .name("Planner")
                .agentType("AUTONOMOUS")
                .role("QUERY_ANALYZER")
                .tenantId(10L)
                .status("ACTIVE")
                .build();
        agent.setId(1L);

        when(agentRepository.findById(1L)).thenReturn(Optional.of(agent));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> agentService.getAgentById(1L, 7L));

        assertEquals("Agent does not belong to tenant: 7", ex.getMessage());
    }

    @Test
    void updateAgentShouldApplyChanges() {
        Agent agent = Agent.builder()
                .name("Old")
                .description("Old desc")
                .agentType("AUTONOMOUS")
                .role("QUERY_ANALYZER")
                .configuration("{}")
                .maxRetries(2)
                .executionTimeoutSeconds(60L)
                .tenantId(7L)
                .status("ACTIVE")
                .build();
        agent.setId(1L);

        CreateAgentRequest request = new CreateAgentRequest();
        request.setName("New");
        request.setDescription("New desc");
        request.setAgentType("REACTIVE");
        request.setRole("DATA_TRANSFORMER");
        request.setConfiguration("{\"flag\":true}");
        request.setMaxRetries(4);
        request.setExecutionTimeoutSeconds(120L);

        when(agentRepository.findById(1L)).thenReturn(Optional.of(agent));
        when(agentRepository.save(any(Agent.class))).thenReturn(agent);

        AgentDto updated = agentService.updateAgent(1L, request, 7L);

        assertEquals("New", updated.getName());
        assertEquals("REACTIVE", updated.getAgentType());
    }

    @Test
    void deactivateAgentShouldThrowWhenMissing() {
        when(agentRepository.findById(5L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> agentService.deactivateAgent(5L, 7L));

        assertEquals("Agent not found with ID: 5", ex.getMessage());
    }

    @Test
    void getAgentsByTenantShouldReturnList() {
        Agent agent = Agent.builder()
                .name("Planner")
                .agentType("AUTONOMOUS")
                .role("QUERY_ANALYZER")
                .status("ACTIVE")
                .tenantId(7L)
                .build();
        agent.setId(1L);

        when(agentRepository.findByTenantId(7L)).thenReturn(List.of(agent));

        List<AgentDto> agents = agentService.getAgentsByTenant(7L);

        assertEquals(1, agents.size());
        assertEquals("Planner", agents.get(0).getName());
    }
}
