package com.karthik.eks.document.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DocumentChunkTest {

    @Test
    void incrementRetrievalCountShouldIncreaseFromExistingValue() {
        DocumentChunk chunk = DocumentChunk.builder()
                .documentId(1L)
                .tenantId(2L)
                .chunkIndex(0)
                .content("sample content")
                .tokens(4)
                .retrievalCount(2)
                .build();

        chunk.incrementRetrievalCount();

        assertEquals(3, chunk.getRetrievalCount());
    }

    @Test
    void incrementRetrievalCountShouldStartAtOneWhenNull() {
        DocumentChunk chunk = DocumentChunk.builder()
                .documentId(1L)
                .tenantId(2L)
                .chunkIndex(0)
                .content("sample content")
                .tokens(4)
                .retrievalCount(null)
                .build();

        chunk.incrementRetrievalCount();

        assertEquals(1, chunk.getRetrievalCount());
    }

    @Test
    void updateAverageRelevanceScoreShouldIgnoreInvalidValues() {
        DocumentChunk chunk = DocumentChunk.builder()
                .documentId(1L)
                .tenantId(2L)
                .chunkIndex(0)
                .content("sample content")
                .tokens(4)
                .build();

        chunk.updateAverageRelevanceScore(-0.5);
        assertNull(chunk.getAverageRelevanceScore());

        chunk.updateAverageRelevanceScore(1.5);
        assertNull(chunk.getAverageRelevanceScore());
    }

    @Test
    void updateAverageRelevanceScoreShouldApplyWeightedMovingAverage() {
        DocumentChunk chunk = DocumentChunk.builder()
                .documentId(1L)
                .tenantId(2L)
                .chunkIndex(0)
                .content("sample content")
                .tokens(4)
                .build();

        chunk.updateAverageRelevanceScore(0.5);
        chunk.updateAverageRelevanceScore(0.7);

        assertEquals(0.56, chunk.getAverageRelevanceScore(), 0.0001);
    }
}
