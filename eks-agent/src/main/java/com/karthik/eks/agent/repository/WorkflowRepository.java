package com.karthik.eks.agent.repository;

import com.karthik.eks.agent.domain.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, Long> {

    /**
     * Find workflows by agent ID
     */
    List<Workflow> findByAgentId(Long agentId);

    /**
     * Find workflows by tenant ID
     */
    List<Workflow> findByTenantId(Long tenantId);

    /**
     * Find workflows by tenant and user ID
     */
    List<Workflow> findByTenantIdAndUserId(Long tenantId, Long userId);

    /**
     * Find workflows by status
     */
    List<Workflow> findByStatusAndTenantId(String status, Long tenantId);

    /**
     * Find workflows by execution status
     */
    List<Workflow> findByExecutionStatus(String executionStatus);

    /**
     * Find active workflows by agent ID
     */
    List<Workflow> findByAgentIdAndStatus(Long agentId, String status);

    /**
     * Find workflows by priority
     */
    @Query("SELECT w FROM Workflow w WHERE w.priority = :priority AND w.tenantId = :tenantId ORDER BY w.createdAt DESC")
    List<Workflow> findByPriorityAndTenant(@Param("priority") String priority, @Param("tenantId") Long tenantId);

    /**
     * Find running workflows
     */
    @Query("SELECT w FROM Workflow w WHERE w.executionStatus = 'RUNNING' AND w.tenantId = :tenantId")
    List<Workflow> findRunningWorkflows(@Param("tenantId") Long tenantId);

    /**
     * Count workflows by agent and status
     */
    Long countByAgentIdAndExecutionStatus(Long agentId, String executionStatus);
}
