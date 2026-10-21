package com.karthik.eks.agent.repository;

import com.karthik.eks.agent.domain.WorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowStepRepository extends JpaRepository<WorkflowStep, Long> {

    /**
     * Find steps by workflow ID ordered by step order
     */
    List<WorkflowStep> findByWorkflowIdOrderByStepOrder(Long workflowId);

    /**
     * Find steps by workflow ID and status
     */
    List<WorkflowStep> findByWorkflowIdAndStatus(Long workflowId, String status);

    /**
     * Find pending steps for a workflow (for execution)
     */
    @Query("SELECT ws FROM WorkflowStep ws WHERE ws.workflow.id = :workflowId AND ws.status = 'PENDING' ORDER BY ws.stepOrder ASC")
    List<WorkflowStep> findPendingSteps(@Param("workflowId") Long workflowId);

    /**
     * Get step by workflow and step name
     */
    Optional<WorkflowStep> findByWorkflowIdAndName(Long workflowId, String name);

    /**
     * Count completed steps
     */
    Long countByWorkflowIdAndStatus(Long workflowId, String status);

    /**
     * Find next step based on step order
     */
    @Query("SELECT ws FROM WorkflowStep ws WHERE ws.workflow.id = :workflowId AND ws.stepOrder = :stepOrder + 1")
    Optional<WorkflowStep> findNextStep(@Param("workflowId") Long workflowId, @Param("stepOrder") Integer stepOrder);
}
