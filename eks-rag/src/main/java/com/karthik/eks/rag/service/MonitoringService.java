package com.karthik.eks.rag.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MonitoringService - Tracks search performance, quality, and errors.
 * 
 * Purpose:
 * - Provides visibility into RAG system performance
 * - Tracks response times, hit rates, and error rates
 * - Monitors resource usage (embeddings, caches)
 * - Supports alerting on degradation
 * - Enables data-driven optimization
 * 
 * Metrics:
 * 1. Latency: Query time, embedding time, ranking time
 * 2. Quality: Hit rate, relevance, user ratings
 * 3. Volume: Requests/sec, embeddings/day
 * 4. Errors: Failure rates, error types
 * 5. Resources: Cache hit rate, EmbeddingAPI cost
 * 
 * Data Flow:
 * 1. Service methods log timing and results
 * 2. MonitoringService aggregates metrics
 * 3. Metrics exposed via REST API or metrics endpoint
 * 4. Dashboards visualize trends
 * 5. Alerts triggered on anomalies
 * 
 * Integration:
 * - Spring Boot Actuator for /metrics endpoint
 * - Prometheus for scraping metrics
 * - Grafana for visualization
 * - Alerting rules (e.g., Alertmanager)
 * 
 * Example Usage:
 * monitoring.recordSearchLatency(150);  // Record 150ms search
 * monitoring.recordCacheHit();          // Record cache hit
 * monitoring.recordError("HybridSearch", error);
 * metrics = monitoring.getMetrics();    // Get current metrics
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringService {

    /**
     * Metric counters - thread-safe atomic operations.
     */
    private final AtomicLong totalSearches = new AtomicLong(0);
    private final AtomicLong totalEmbeddings = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong errors = new AtomicLong(0);
    private final AtomicLong totalLatencyMs = new AtomicLong(0);

    /**
     * Error tracking by type.
     */
    private final Map<String, AtomicLong> errorCounts = new ConcurrentHashMap<>();

    /**
     * Performance tracking.
     */
    private volatile LocalDateTime lastResetTime = LocalDateTime.now();

    /**
     * Record a search latency measurement.
     * Used when search operation completes.
     * 
     * @param latencyMs the search latency in milliseconds
     */
    public void recordSearchLatency(long latencyMs) {
        totalSearches.incrementAndGet();
        totalLatencyMs.addAndGet(latencyMs);

        if (latencyMs > 1000) {
            log.warn("Slow search detected: {}ms", latencyMs);
        }

        log.debug("Search completed in {}ms", latencyMs);
    }

    /**
     * Record an embedding generation.
     * Used when an embedding is created.
     * 
     * @param latencyMs the embedding generation time
     */
    public void recordEmbeddingGeneration(long latencyMs) {
        totalEmbeddings.incrementAndGet();

        if (latencyMs > 500) {
            log.warn("Slow embedding generation: {}ms", latencyMs);
        }

        log.debug("Embedding generated in {}ms", latencyMs);
    }

    /**
     * Record a cache hit.
     * Called when a result is retrieved from cache.
     */
    public void recordCacheHit() {
        cacheHits.incrementAndGet();
        long totalAccesses = cacheHits.get() + cacheMisses.get();

        if (totalAccesses % 100 == 0) {
            double hitRate = (double) cacheHits.get() / totalAccesses * 100;
            log.debug("Cache hit rate: {:.1f}%", hitRate);
        }
    }

    /**
     * Record a cache miss.
     * Called when cache lookup fails.
     */
    public void recordCacheMiss() {
        cacheMisses.incrementAndGet();
    }

    /**
     * Record an error occurrence.
     * Used for error tracking and debugging.
     * 
     * @param component the component where error occurred
     * @param error the exception or error message
     */
    public void recordError(String component, Exception error) {
        errors.incrementAndGet();

        String errorType = error.getClass().getSimpleName();
        errorCounts.computeIfAbsent(component + ":" + errorType, k -> new AtomicLong(0))
                .incrementAndGet();

        log.error("Error in {}: {} - {}", component, errorType, error.getMessage());
    }

    /**
     * Record a generic error.
     * 
     * @param errorMessage the error message
     */
    public void recordError(String errorMessage) {
        errors.incrementAndGet();
        log.error("Error recorded: {}", errorMessage);
    }

    /**
     * Get current monitoring metrics.
     * Returns aggregated statistics.
     * 
     * @return metrics object with all current values
     */
    public SystemMetrics getMetrics() {
        long totalAccesses = cacheHits.get() + cacheMisses.get();
        double cacheHitRate = totalAccesses > 0 
                ? (double) cacheHits.get() / totalAccesses 
                : 0.0;

        double avgLatencyMs = totalSearches.get() > 0 
                ? (double) totalLatencyMs.get() / totalSearches.get() 
                : 0.0;

        double errorRate = totalSearches.get() > 0 
                ? (double) errors.get() / totalSearches.get() 
                : 0.0;

        long uptime = ChronoUnit.SECONDS.between(lastResetTime, LocalDateTime.now());

        return SystemMetrics.builder()
                .timestamp(LocalDateTime.now())
                .totalSearches(totalSearches.get())
                .totalEmbeddings(totalEmbeddings.get())
                .cacheHitRate(cacheHitRate)
                .cacheHits(cacheHits.get())
                .cacheMisses(cacheMisses.get())
                .averageSearchLatencyMs(avgLatencyMs)
                .totalErrors(errors.get())
                .errorRate(errorRate)
                .uptimeSeconds(uptime)
                .errorBreakdown(new ConcurrentHashMap<>(errorCounts))
                .build();
    }

    /**
     * Reset all metrics counters.
     * Used for testing or to clear historical data.
     * 
     * Effects:
     * - Clears all counters
     * - Resets uptime counter
     * - Preserves error breakdown for reference
     */
    public void resetMetrics() {
        log.info("Resetting monitoring metrics");

        totalSearches.set(0);
        totalEmbeddings.set(0);
        cacheHits.set(0);
        cacheMisses.set(0);
        errors.set(0);
        totalLatencyMs.set(0);
        lastResetTime = LocalDateTime.now();

        log.info("Metrics reset complete");
    }

    /**
     * Get health status based on current metrics.
     * Returns overall system health assessment.
     * 
     * Rules:
     * - ERROR_RATE > 5%: DEGRADED
     * - CACHE_HIT_RATE < 10%: WARNING
     * - AVG_LATENCY > 500ms: WARNING
     * - Otherwise: HEALTHY
     * 
     * @return health status
     */
    public HealthStatus getHealthStatus() {
        SystemMetrics metrics = getMetrics();

        if (metrics.getErrorRate() > 0.05) {
            return HealthStatus.DEGRADED;
        }

        if (metrics.getCacheHitRate() < 0.1 && metrics.getTotalSearches() > 100) {
            log.warn("Low cache hit rate: {:.1f}%", metrics.getCacheHitRate() * 100);
            return HealthStatus.WARNING;
        }

        if (metrics.getAverageSearchLatencyMs() > 500) {
            log.warn("High average latency: {:.0f}ms", metrics.getAverageSearchLatencyMs());
            return HealthStatus.WARNING;
        }

        return HealthStatus.HEALTHY;
    }

    /**
     * HealthStatus enumeration.
     */
    public enum HealthStatus {
        HEALTHY,    // System operating normally
        WARNING,    // Minor issues detected, may need attention
        DEGRADED    // Significant issues, performance impacted
    }

    /**
     * SystemMetrics DTO - Complete system metrics snapshot.
     */
    @lombok.Getter
    @lombok.Setter
    @lombok.Builder
    public static class SystemMetrics {
        private LocalDateTime timestamp;
        private Long totalSearches;
        private Long totalEmbeddings;
        private Double cacheHitRate;
        private Long cacheHits;
        private Long cacheMisses;
        private Double averageSearchLatencyMs;
        private Long totalErrors;
        private Double errorRate;
        private Long uptimeSeconds;
        private Map<String, AtomicLong> errorBreakdown;
    }

    /**
     * Log system status periodically.
     * Called from scheduled task to provide ongoing visibility.
     */
    public void logSystemStatus() {
        SystemMetrics metrics = getMetrics();
        HealthStatus health = getHealthStatus();

        log.info("System Status [{}]: " +
                "Searches={}, Embeddings={}, " +
                "CacheHitRate={:.1f}%, AvgLatency={:.0f}ms, " +
                "Errors={} ({:.1f}%), Uptime={}s",
                health,
                metrics.getTotalSearches(),
                metrics.getTotalEmbeddings(),
                metrics.getCacheHitRate() * 100,
                metrics.getAverageSearchLatencyMs(),
                metrics.getTotalErrors(),
                metrics.getErrorRate() * 100,
                metrics.getUptimeSeconds()
        );
    }
}
