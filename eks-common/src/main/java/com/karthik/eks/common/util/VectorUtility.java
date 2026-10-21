package com.karthik.eks.common.util;

import java.util.ArrayList;
import java.util.List;

/**
 * VectorUtility - Shared utilities for vector and embedding operations.
 * 
 * Purpose:
 * - Format embeddings for PostgreSQL pgvector extension
 * - Parse vector strings from database results
 * - Normalize and validate vector operations
 * 
 * pgvector uses "[a,b,c]" string format for vectors.
 * Java embeddings are typically float[] arrays.
 */
public class VectorUtility {

    private VectorUtility() {
        // Utility class - no instantiation
    }

    /**
     * Format a float array embedding for PostgreSQL pgvector.
     * 
     * pgvector expects format: "[0.1,0.2,0.3,...]"
     * 
     * Example:
     * float[] embedding = {0.1f, 0.2f, 0.3f};
     * String pgvectorFormat = formatVectorForPgvector(embedding);
     * // Result: "[0.1,0.2,0.3]"
     * 
     * @param embedding the float array embedding (768 dims for nomic-embed-text)
     * @return pgvector-formatted string
     * @throws IllegalArgumentException if embedding is empty or null
     */
    public static String formatVectorForPgvector(float[] embedding) {
        if (embedding == null || embedding.length == 0) {
            throw new IllegalArgumentException("Embedding cannot be null or empty");
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            sb.append(embedding[i]);
            if (i < embedding.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");

        return sb.toString();
    }

    /**
     * Parse a pgvector string into a float array.
     * 
     * Converts "[0.1,0.2,0.3,...]" to {0.1f, 0.2f, 0.3f, ...}
     * 
     * @param pgvectorString the pgvector-formatted string
     * @return float array embedding
     * @throws IllegalArgumentException if format is invalid
     */
    public static float[] parseVectorFromPgvector(String pgvectorString) {
        if (pgvectorString == null || !pgvectorString.startsWith("[") || !pgvectorString.endsWith("]")) {
            throw new IllegalArgumentException("Invalid pgvector format: " + pgvectorString);
        }

        String content = pgvectorString.substring(1, pgvectorString.length() - 1);
        if (content.isEmpty()) {
            throw new IllegalArgumentException("Empty vector");
        }

        String[] values = content.split(",");
        float[] result = new float[values.length];

        try {
            for (int i = 0; i < values.length; i++) {
                result[i] = Float.parseFloat(values[i].trim());
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid vector component: " + e.getMessage());
        }

        return result;
    }

    /**
     * Calculate Euclidean distance (L2) between two embeddings.
     * 
     * Used for similarity search: lower distance = more similar
     * 
     * Formula: sqrt(sum((a[i] - b[i])^2))
     * 
     * @param embedding1 first embedding
     * @param embedding2 second embedding
     * @return Euclidean distance
     * @throws IllegalArgumentException if dimensions don't match
     */
    public static double calculateEuclideanDistance(float[] embedding1, float[] embedding2) {
        if (embedding1.length != embedding2.length) {
            throw new IllegalArgumentException(
                    "Embedding dimensions must match: " + embedding1.length + " vs " + embedding2.length
            );
        }

        double sumSquares = 0.0;
        for (int i = 0; i < embedding1.length; i++) {
            double diff = embedding1[i] - embedding2[i];
            sumSquares += diff * diff;
        }

        return Math.sqrt(sumSquares);
    }

    /**
     * Calculate cosine similarity between two embeddings.
     * 
     * Used for semantic similarity: 1.0 = identical, 0.0 = orthogonal, -1.0 = opposite
     * 
     * Formula: (a · b) / (||a|| * ||b||)
     * 
     * @param embedding1 first embedding
     * @param embedding2 second embedding
     * @return cosine similarity from -1.0 to 1.0
     * @throws IllegalArgumentException if dimensions don't match
     */
    public static double calculateCosineSimilarity(float[] embedding1, float[] embedding2) {
        if (embedding1.length != embedding2.length) {
            throw new IllegalArgumentException(
                    "Embedding dimensions must match: " + embedding1.length + " vs " + embedding2.length
            );
        }

        double dotProduct = 0.0;
        double magnitude1 = 0.0;
        double magnitude2 = 0.0;

        for (int i = 0; i < embedding1.length; i++) {
            dotProduct += embedding1[i] * embedding2[i];
            magnitude1 += embedding1[i] * embedding1[i];
            magnitude2 += embedding2[i] * embedding2[i];
        }

        magnitude1 = Math.sqrt(magnitude1);
        magnitude2 = Math.sqrt(magnitude2);

        if (magnitude1 == 0.0 || magnitude2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (magnitude1 * magnitude2);
    }

    /**
     * Normalize an embedding to unit length (magnitude 1).
     * 
     * Common preprocessing for similarity calculations.
     * 
     * @param embedding the embedding to normalize
     * @return normalized embedding
     */
    public static float[] normalize(float[] embedding) {
        if (embedding == null || embedding.length == 0) {
            throw new IllegalArgumentException("Embedding cannot be null or empty");
        }

        double magnitude = 0.0;
        for (float value : embedding) {
            magnitude += value * value;
        }
        magnitude = Math.sqrt(magnitude);

        if (magnitude == 0.0) {
            return embedding.clone();
        }

        float[] normalized = new float[embedding.length];
        for (int i = 0; i < embedding.length; i++) {
            normalized[i] = (float) (embedding[i] / magnitude);
        }

        return normalized;
    }

    /**
     * Validate embedding dimensions match expected size.
     * 
     * @param embedding the embedding to validate
     * @param expectedDimension the expected dimension
     * @throws IllegalArgumentException if dimensions don't match
     */
    public static void validateDimension(float[] embedding, int expectedDimension) {
        if (embedding == null || embedding.length != expectedDimension) {
            throw new IllegalArgumentException(
                    "Invalid embedding dimension: expected " + expectedDimension +
                            ", got " + (embedding == null ? 0 : embedding.length)
            );
        }
    }

    /**
     * Batch format multiple embeddings for pgvector.
     * 
     * @param embeddings list of float array embeddings
     * @return list of pgvector-formatted strings
     */
    public static List<String> formatBatchForPgvector(List<float[]> embeddings) {
        List<String> formatted = new ArrayList<>();
        for (float[] embedding : embeddings) {
            formatted.add(formatVectorForPgvector(embedding));
        }
        return formatted;
    }
}
