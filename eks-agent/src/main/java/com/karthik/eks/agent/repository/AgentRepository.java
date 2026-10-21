package com.karthik.eks.agent.repository;

import com.karthik.eks.agent.domain.Agent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {

    /**
     * Find agents by tenant ID
     */
    List<Agent> findByTenantId(Long tenantId);

    /**
     * Find agents by tenant ID and status
     */
    List<Agent> findByTenantIdAndStatus(Long tenantId, String status);

    /**
     * Find agents by role
     */
    List<Agent> findByRoleAndStatus(String role, String status);

    /**
     * Find agent by name and tenant ID
     */
    Optional<Agent> findByNameAndTenantId(String name, Long tenantId);

    /**
     * Check if agent exists
     */
    boolean existsByNameAndTenantId(String name, Long tenantId);

    /**
     * Custom query to find agents by agent type and tenant
     */
    @Query("SELECT a FROM Agent a WHERE a.agentType = :agentType AND a.tenantId = :tenantId AND a.status = 'ACTIVE'")
    List<Agent> findActiveAgentsByTypeAndTenant(@Param("agentType") String agentType, @Param("tenantId") Long tenantId);
}
