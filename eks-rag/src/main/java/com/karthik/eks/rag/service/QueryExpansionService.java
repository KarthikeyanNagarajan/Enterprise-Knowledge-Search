package com.karthik.eks.rag.service;

import com.karthik.eks.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * QueryExpansionService - Expands queries to improve retrieval recall.
 * 
 * Purpose:
 * - Handles synonym expansion (e.g., "car" → "car", "automobile", "vehicle")
 * - Removes stop words for cleaner search
 * - Generates query variations to improve semantic matching
 * - Helps bridge vocabulary gaps between queries and documents
 * 
 * Techniques:
 * 1. Synonym Expansion: Maps common terms to similar terms
 * 2. Stop Word Removal: Filters out non-informative words
 * 3. Query Variation: Generates alternative formulations
 * 
 * Benefits:
 * - Improves recall (finding more relevant documents)
 * - Reduces vocabulary mismatch issues
 * - Handles user colloquialisms and terminology variations
 * 
 * Example:
 * Input: "How to fix bugs in software?"
 * Output:
 *   - Original: "How to fix bugs in software"
 *   - Variations:
 *     1. "fix bugs software"
 *     2. "bug fixing software development"
 *     3. "software debugging troubleshooting"
 * 
 * Future Enhancements:
 * - Integration with WordNet or other NLP libraries for automatic synonyms
 * - Context-aware expansion using LLM
 * - User feedback on expansion quality
 * - Custom synonym dictionaries per tenant
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryExpansionService {

    /**
     * Common stop words that are filtered from queries.
     * These words have little semantic value and can noise up searches.
     */
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "but", "by",
            "for", "from", "had", "has", "have", "he", "her", "his",
            "how", "i", "if", "in", "is", "it", "its", "may", "me",
            "my", "no", "not", "of", "on", "or", "our", "out", "she",
            "so", "such", "that", "the", "their", "them", "then",
            "there", "these", "they", "this", "to", "too", "under",
            "up", "was", "we", "what", "when", "where", "which", "who",
            "will", "with", "you", "your", "about", "above", "after",
            "again", "into", "over", "between", "further", "should"
    );

    /**
     * Synonym mappings for common technical and domain-specific terms.
     * Maps singular terms to related synonyms.
     * 
     * Can be extended with:
     * - Domain-specific synonyms
     * - Abbreviations (e.g., "API" → "application programming interface")
     * - Plural forms
     */
    private static final Map<String, Set<String>> SYNONYM_MAP = Map.ofEntries(
            // API and Development
            Map.entry("api", Set.of("application programming interface", "endpoint", "interface")),
            Map.entry("bug", Set.of("defect", "issue", "error", "problem")),
            Map.entry("fix", Set.of("resolve", "patch", "correction", "repair")),
            Map.entry("deploy", Set.of("launch", "release", "push", "publish")),
            Map.entry("database", Set.of("db", "storage", "datastore", "repository")),

            // Documentation
            Map.entry("documentation", Set.of("doc", "docs", "guide", "manual")),
            Map.entry("tutorial", Set.of("guide", "walkthrough", "example")),
            Map.entry("example", Set.of("sample", "demo", "illustration")),

            // Performance
            Map.entry("fast", Set.of("quick", "rapid", "speedy", "efficient")),
            Map.entry("slow", Set.of("sluggish", "lagging", "inefficient")),
            Map.entry("performance", Set.of("speed", "efficiency", "optimization")),

            // Security
            Map.entry("security", Set.of("protection", "safeguard", "authentication")),
            Map.entry("authentication", Set.of("auth", "login", "credentials")),
            Map.entry("encryption", Set.of("cipher", "encoding", "protection")),

            // Error Handling
            Map.entry("error", Set.of("exception", "failure", "problem", "issue")),
            Map.entry("warning", Set.of("caution", "alert", "notification")),

            // Networking
            Map.entry("network", Set.of("connectivity", "communication", "link")),
            Map.entry("server", Set.of("host", "backend", "service")),
            Map.entry("client", Set.of("frontend", "requester", "consumer")),

            // Data
            Map.entry("query", Set.of("search", "request", "question")),
            Map.entry("filter", Set.of("search", "selection", "criteria")),
            Map.entry("data", Set.of("information", "records", "content"))
    );

    /**
     * Expand a user query to improve retrieval recall.
     * 
     * Process:
     * 1. Remove stop words to focus on meaningful terms
     * 2. Expand each term with synonyms
     * 3. Generate multiple query variations
     * 4. Return all variations for parallel searching
     * 
     * @param originalQuery the user's original query
     * @return set of expanded query variations
     * @throws BusinessException if query is invalid
     */
    public Set<String> expandQuery(String originalQuery) {
        log.info("Expanding query: '{}'", originalQuery);

        if (originalQuery == null || originalQuery.trim().isEmpty()) {
            throw new BusinessException("Query cannot be empty", HttpStatus.BAD_REQUEST);
        }

        try {
            Set<String> expansions = new HashSet<>();

            // Add original query
            expansions.add(originalQuery.trim());

            // Step 1: Clean query - remove stop words
            String cleanedQuery = removeStopWords(originalQuery);
            expansions.add(cleanedQuery);
            log.debug("Cleaned query: '{}'", cleanedQuery);

            // Step 2: Synonym-based expansions
            String[] terms = originalQuery.toLowerCase().split("\\s+");
            Set<String> synonynmExpansions = generateSynonymExpansions(terms);
            expansions.addAll(synonynmExpansions);

            // Step 3: Generate variations
            Set<String> variations = generateQueryVariations(originalQuery);
            expansions.addAll(variations);

            // Remove duplicates and empty strings
            expansions = expansions.stream()
                    .filter(q -> q != null && !q.trim().isEmpty())
                    .collect(Collectors.toSet());

            log.info("Query expansion complete. Generated {} variations from '{}'", 
                    expansions.size(), originalQuery);
            log.debug("Expansions: {}", expansions);

            return expansions;

        } catch (Exception e) {
            log.error("Query expansion failed for: '{}'", originalQuery, e);
            throw new BusinessException("Query expansion failed: " + e.getMessage(), e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Remove stop words from a query to highlight meaningful terms.
     * 
     * Example:
     * Input: "How to fix bugs in software development?"
     * Output: "fix bugs software development"
     * 
     * @param query the original query
     * @return query with stop words removed
     */
    private String removeStopWords(String query) {
        return Arrays.stream(query.toLowerCase().split("\\s+"))
                .filter(word -> !STOP_WORDS.contains(word.replaceAll("[^a-z]", "")))
                .collect(Collectors.joining(" "))
                .trim();
    }

    /**
     * Generate synonym expansions for query terms.
     * 
     * Example:
     * Terms: ["fix", "bug", "software"]
     * Expansions:
     *   - "resolve bug software"
     *   - "fix defect software"
     *   - "fix issue software"
     * 
     * @param terms the query terms to expand
     * @return set of expanded queries with synonyms
     */
    private Set<String> generateSynonymExpansions(String[] terms) {
        Set<String> expansions = new HashSet<>();

        // For each term, check if it has synonyms
        for (String term : terms) {
            String cleanTerm = term.toLowerCase().replaceAll("[^a-z]", "");
            
            if (SYNONYM_MAP.containsKey(cleanTerm)) {
                Set<String> synonyms = SYNONYM_MAP.get(cleanTerm);
                
                // Create variations with each synonym
                for (String synonym : synonyms) {
                    String[] currentTerms = Arrays.copyOf(terms, terms.length);
                    for (int i = 0; i < currentTerms.length; i++) {
                        if (currentTerms[i].equals(term)) {
                            currentTerms[i] = synonym;
                            break;
                        }
                    }
                    expansions.add(String.join(" ", currentTerms));
                }
            }
        }

        return expansions;
    }

    /**
     * Generate query variations to improve semantic matching.
     * 
     * Techniques:
     * 1. N-gram variations: "A B C" → "A B", "B C", "A C"
     * 2. Word order variations: "A B C" → "B A C", "C B A" (for smaller queries)
     * 3. Keyword extraction: Highlight important terms
     * 
     * @param originalQuery the original query
     * @return set of query variations
     */
    private Set<String> generateQueryVariations(String originalQuery) {
        Set<String> variations = new HashSet<>();
        String[] terms = originalQuery.toLowerCase().split("\\s+");

        // Skip if query is too short
        if (terms.length < 2) {
            return variations;
        }

        // Variation 1: Bigrams (pairs of consecutive words)
        for (int i = 0; i < terms.length - 1; i++) {
            variations.add((terms[i] + " " + terms[i + 1]).trim());
        }

        // Variation 2: Important bigrams (skip common words)
        Set<String> importantTerms = Arrays.stream(terms)
                .filter(t -> !STOP_WORDS.contains(t.replaceAll("[^a-z]", "")))
                .collect(Collectors.toSet());
        
        if (importantTerms.size() >= 2) {
            String importantQuery = String.join(" ", importantTerms);
            variations.add(importantQuery);
        }

        // Variation 3: Question reformulation (remove question marks and reshape)
        String normalized = originalQuery
                .replaceAll("[?!.]", "")
                .replaceAll("how\\s+to\\s+", "")
                .replaceAll("what\\s+is\\s+", "")
                .trim();
        if (!normalized.isEmpty()) {
            variations.add(normalized);
        }

        return variations;
    }

    /**
     * Get the synonym map (useful for debugging and admin interfaces).
     * 
     * @return map of term → synonyms
     */
    public Map<String, Set<String>> getSynonymMap() {
        return Map.copyOf(SYNONYM_MAP);
    }

    /**
     * Get stop words set (useful for debugging and admin interfaces).
     * 
     * @return unmodifiable set of stop words
     */
    public Set<String> getStopWords() {
        return Set.copyOf(STOP_WORDS);
    }
}
