package com.karthik.eks.identity.service;

import com.karthik.eks.identity.entity.Tenant;
import com.karthik.eks.identity.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tenant Admin Service - Metrics and Statistics.
 * 
 * Responsibilities:
 * 1. Provide tenant usage metrics
 * 2. Generate tenant statistics
 * 3. Track document processing per tenant
 * 4. Report conversation activity per tenant
 * 
 * Architecture:
 * - Read-only access to other module data
 * - No AI logic (that would make it RAG system's job)
 * - Reporting and analytics only
 * 
 * This was previously in eks-admin module, now consolidated into eks-identity
 * since it manages tenant-level metrics.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantAdminService {

    private final TenantRepository tenantRepository;

    /**
     * Get statistics for a tenant.
     * 
     * @param tenantId the tenant ID
     * @return TenantStats with usage metrics
     */
    @Transactional(readOnly = true)
    public TenantStats getTenantStats(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
        
        // Note: Document and conversation counts would be queried from their respective
        // repositories once they have tenant awareness. For now, this is placeholder.
        long documentCount = 0;  // documentRepository.countByTenantId(tenantId);
        long conversationCount = 0;  // conversationRepository.countByTenantId(tenantId);

        return TenantStats.builder()
                .tenantId(tenantId)
                .tenantName(tenant.getName())
                .tenantCode(tenant.getCode())
                .totalDocuments(documentCount)
                .totalConversations(conversationCount)
                .build();
    }

    /**
     * Result DTO for tenant statistics.
     */
    @lombok.Data
    @lombok.Builder
    public static class TenantStats {
        private Long tenantId;
        private String tenantName;
        private String tenantCode;
        private long totalDocuments;
        private long totalConversations;
    }
}
