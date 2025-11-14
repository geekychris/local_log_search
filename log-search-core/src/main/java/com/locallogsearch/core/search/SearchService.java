package com.locallogsearch.core.search;

import com.locallogsearch.core.config.IndexConfig;
import com.locallogsearch.core.pipe.*;
import com.locallogsearch.core.pipe.PipeQueryParser.ParsedQuery;
import com.locallogsearch.core.pipe.PipeQueryParser.PipeCommandSpec;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class SearchService {
    private static final Logger log = LoggerFactory.getLogger(SearchService.class);
    
    private final IndexConfig indexConfig;
    private final StandardAnalyzer analyzer;
    private final Map<String, IndexReader> indexReaders;
    
    public SearchService(IndexConfig indexConfig) {
        this.indexConfig = indexConfig;
        this.analyzer = new StandardAnalyzer();
        this.indexReaders = new HashMap<>();
    }
    
    public SearchResponse search(SearchRequest request) throws IOException, ParseException {
        // Check if query contains pipes
        ParsedQuery parsedQuery = PipeQueryParser.parse(request.getQuery());
        
        if (parsedQuery.hasPipes()) {
            // Handle piped query
            return searchWithPipes(request, parsedQuery);
        }
        
        // Regular search
        List<SearchResult> allResults = new ArrayList<>();
        int totalHits = 0;
        Map<String, Map<String, Integer>> allFacets = new HashMap<>();
        
        for (String indexName : request.getIndices()) {
            SearchResponse indexResponse = searchIndex(indexName, request);
            allResults.addAll(indexResponse.getResults());
            totalHits += indexResponse.getTotalHits();
            
            // Merge facets
            if (request.isIncludeFacets()) {
                mergeFacets(allFacets, indexResponse.getFacets());
            }
        }
        
        // Sort results
        sortResults(allResults, request);
        
        // Apply pagination
        int start = request.getPage() * request.getPageSize();
        int end = Math.min(start + request.getPageSize(), allResults.size());
        List<SearchResult> pageResults = start < allResults.size() ? 
            allResults.subList(start, end) : new ArrayList<>();
        
        return new SearchResponse(pageResults, totalHits, request.getPage(), request.getPageSize(), allFacets);
    }
    
    // Legacy method for backward compatibility
    public SearchResponse search(List<String> indexNames, String queryString, int maxResults) throws IOException, ParseException {
        SearchRequest request = new SearchRequest();
        request.setIndices(indexNames);
        request.setQuery(queryString);
        request.setPageSize(maxResults);
        return search(request);
    }
    
    private SearchResponse searchIndex(String indexName, SearchRequest request) throws IOException, ParseException {
        Path indexPath = Paths.get(indexConfig.getBaseDirectory(), indexName);
        
        if (!Files.exists(indexPath)) {
            log.warn("Index does not exist: {}", indexName);
            return new SearchResponse(new ArrayList<>(), 0);
        }
        
        IndexReader reader = getOrOpenReader(indexName, indexPath);
        IndexSearcher searcher = new IndexSearcher(reader);
        
        // Parse query - search in raw_text and all other fields
        String[] fields = {"raw_text"};
        MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, analyzer);
        parser.setDefaultOperator(QueryParser.Operator.AND);
        
        Query query = parser.parse(request.getQuery());
        
        // Add timestamp range filter if specified
        if (request.getTimestampFrom() != null || request.getTimestampTo() != null) {
            long from = request.getTimestampFrom() != null ? request.getTimestampFrom() : 0L;
            long to = request.getTimestampTo() != null ? request.getTimestampTo() : Long.MAX_VALUE;
            log.info("Applying timestamp filter: from={} to={}", from, to);
            Query timestampQuery = LongPoint.newRangeQuery("timestamp", from, to);
            
            // Combine with main query using BooleanQuery
            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            builder.add(query, BooleanClause.Occur.MUST);
            builder.add(timestampQuery, BooleanClause.Occur.FILTER);
            query = builder.build();
            log.info("Combined query: {}", query);
        }
        
        // Get enough results for pagination and faceting
        // For pipe queries, we might need more results
        int requestedPageSize = request.getPageSize();
        int maxDocsToFetch = requestedPageSize > 0 ? Math.min(requestedPageSize, reader.numDocs()) : Math.min(10000, reader.numDocs());
        TopDocs topDocs = searcher.search(query, maxDocsToFetch);
        
        List<SearchResult> results = new ArrayList<>();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document doc = searcher.doc(scoreDoc.doc);
            SearchResult result = documentToSearchResult(doc, indexName, scoreDoc.score);
            results.add(result);
        }
        
        // Calculate facets if requested - use ALL matching documents, not just the page
        Map<String, Map<String, Integer>> facets = new HashMap<>();
        if (request.isIncludeFacets()) {
            facets = calculateFacetsFromAllHits(searcher, query, topDocs.totalHits.value, request.getFacetBuckets());
        }
        
        return new SearchResponse(results, (int) topDocs.totalHits.value, 0, results.size(), facets);
    }
    
    private IndexReader getOrOpenReader(String indexName, Path indexPath) throws IOException {
        IndexReader reader = indexReaders.get(indexName);
        
        if (reader == null) {
            Directory directory = FSDirectory.open(indexPath);
            reader = DirectoryReader.open(directory);
            indexReaders.put(indexName, reader);
        } else {
            // Try to reopen if there are changes
            DirectoryReader newReader = DirectoryReader.openIfChanged((DirectoryReader) reader);
            if (newReader != null) {
                reader.close();
                reader = newReader;
                indexReaders.put(indexName, reader);
            }
        }
        
        return reader;
    }
    
    private SearchResult documentToSearchResult(Document doc, String indexName, float score) {
        SearchResult result = new SearchResult();
        result.setIndexName(indexName);
        result.setScore(score);
        result.setRawText(doc.get("raw_text"));
        result.setSource(doc.get("source"));
        
        Long timestampMillis = doc.getField("timestamp") != null ? 
            doc.getField("timestamp").numericValue().longValue() : null;
        if (timestampMillis != null) {
            result.setTimestamp(Instant.ofEpochMilli(timestampMillis));
        }
        
        // Extract all other fields
        Map<String, String> fields = new HashMap<>();
        doc.getFields().forEach(field -> {
            String name = field.name();
            if (!name.equals("raw_text") && !name.equals("source") && !name.equals("timestamp") && !name.endsWith("_exact")) {
                fields.put(name, field.stringValue());
            }
        });
        result.setFields(fields);
        
        return result;
    }
    
    /**
     * Calculate facets from ALL matching documents, not just the returned page.
     * This dynamically discovers fields and counts values across all hits.
     * Supports bucketing numeric fields into ranges.
     */
    private Map<String, Map<String, Integer>> calculateFacetsFromAllHits(IndexSearcher searcher, Query query, long totalHits, 
                                                                           Map<String, SearchRequest.FacetBucketConfig> bucketConfigs) throws IOException {
        Map<String, Map<String, Integer>> facets = new HashMap<>();
        
        if (totalHits == 0) {
            return facets;
        }
        
        // Limit faceting to reasonable number of documents for performance
        int maxDocsForFaceting = Math.min((int) totalHits, 10000);
        TopDocs allDocs = searcher.search(query, maxDocsForFaceting);
        
        // Dynamically discover ALL fields and count values in a single pass
        for (ScoreDoc scoreDoc : allDocs.scoreDocs) {
            Document doc = searcher.doc(scoreDoc.doc);
            
            // Iterate through all fields in this document
            for (IndexableField field : doc.getFields()) {
                String fieldName = field.name();
                
                // Skip internal/system fields
                if (fieldName.equals("raw_text") || fieldName.equals("source") || 
                    fieldName.equals("timestamp") || fieldName.endsWith("_exact")) {
                    continue;
                }
                
                String value = field.stringValue();
                if (value != null && !value.isEmpty()) {
                    // Check if this field should be bucketed
                    if (bucketConfigs != null && bucketConfigs.containsKey(fieldName)) {
                        String bucketLabel = bucketValue(value, bucketConfigs.get(fieldName));
                        if (bucketLabel != null) {
                            facets.computeIfAbsent(fieldName, k -> new HashMap<>())
                                  .merge(bucketLabel, 1, Integer::sum);
                        }
                    } else {
                        // Normal faceting - count exact values
                        facets.computeIfAbsent(fieldName, k -> new HashMap<>())
                              .merge(value, 1, Integer::sum);
                    }
                }
            }
        }
        
        return facets;
    }
    
    /**
     * Bucket a numeric value into a range based on configuration
     */
    private String bucketValue(String value, SearchRequest.FacetBucketConfig config) {
        try {
            // Try to parse as numeric (remove non-numeric suffixes like "ms")
            String numStr = value.replaceAll("[^0-9.\\-]", "");
            double numValue = Double.parseDouble(numStr);
            
            List<Double> ranges = config.getRanges();
            if (ranges == null || ranges.isEmpty()) {
                return value;  // No ranges defined, return original
            }
            
            // Sort ranges to ensure proper ordering
            List<Double> sortedRanges = new ArrayList<>(ranges);
            Collections.sort(sortedRanges);
            
            // Find which bucket the value falls into
            for (int i = 0; i < sortedRanges.size(); i++) {
                double lower = sortedRanges.get(i);
                
                if (i == sortedRanges.size() - 1) {
                    // Last range - everything >= this value
                    if (numValue >= lower) {
                        return lower + "+";
                    }
                } else {
                    double upper = sortedRanges.get(i + 1);
                    if (numValue >= lower && numValue < upper) {
                        return lower + "-" + upper;
                    }
                }
            }
            
            // Value is less than smallest range
            return "<" + sortedRanges.get(0);
            
        } catch (NumberFormatException e) {
            // Not a number, return original value
            return value;
        }
    }
    
    private void mergeFacets(Map<String, Map<String, Integer>> target, Map<String, Map<String, Integer>> source) {
        for (Map.Entry<String, Map<String, Integer>> fieldEntry : source.entrySet()) {
            String fieldName = fieldEntry.getKey();
            Map<String, Integer> targetCounts = target.computeIfAbsent(fieldName, k -> new HashMap<>());
            
            for (Map.Entry<String, Integer> valueEntry : fieldEntry.getValue().entrySet()) {
                targetCounts.merge(valueEntry.getKey(), valueEntry.getValue(), Integer::sum);
            }
        }
    }
    
    private void sortResults(List<SearchResult> results, SearchRequest request) {
        if (request.getSortField() == null || request.getSortField().isEmpty()) {
            // Default: sort by score descending
            results.sort((a, b) -> Float.compare(b.getScore(), a.getScore()));
            return;
        }
        
        String sortField = request.getSortField();
        boolean desc = request.isSortDescending();
        
        if ("timestamp".equals(sortField)) {
            results.sort((a, b) -> {
                Instant ta = a.getTimestamp();
                Instant tb = b.getTimestamp();
                if (ta == null && tb == null) return 0;
                if (ta == null) return desc ? 1 : -1;
                if (tb == null) return desc ? -1 : 1;
                int cmp = ta.compareTo(tb);
                return desc ? -cmp : cmp;
            });
        } else if ("score".equals(sortField)) {
            results.sort((a, b) -> {
                int cmp = Float.compare(a.getScore(), b.getScore());
                return desc ? -cmp : cmp;
            });
        } else {
            // Sort by field value
            results.sort((a, b) -> {
                String va = a.getFields() != null ? a.getFields().get(sortField) : null;
                String vb = b.getFields() != null ? b.getFields().get(sortField) : null;
                if (va == null && vb == null) return 0;
                if (va == null) return desc ? 1 : -1;
                if (vb == null) return desc ? -1 : 1;
                int cmp = va.compareTo(vb);
                return desc ? -cmp : cmp;
            });
        }
    }
    
    /**
     * Search with pipe commands
     */
    private SearchResponse searchWithPipes(SearchRequest request, ParsedQuery parsedQuery) throws IOException, ParseException {
        log.debug("searchWithPipes - timestampFrom: {}, timestampTo: {}", request.getTimestampFrom(), request.getTimestampTo());
        
        // Execute base search with the parsed base query
        SearchRequest baseRequest = new SearchRequest();
        baseRequest.setIndices(request.getIndices());
        baseRequest.setQuery(parsedQuery.getBaseQuery());
        baseRequest.setPageSize(100000); // Get enough results for pipe processing (increased limit)
        baseRequest.setIncludeFacets(false); // Don't need facets for pipes
        // Copy timestamp filters from original request
        baseRequest.setTimestampFrom(request.getTimestampFrom());
        baseRequest.setTimestampTo(request.getTimestampTo());
        log.debug("baseRequest - timestampFrom: {}, timestampTo: {}", baseRequest.getTimestampFrom(), baseRequest.getTimestampTo());
        
        List<SearchResult> allResults = new ArrayList<>();
        int totalHitsAcrossIndices = 0;
        for (String indexName : request.getIndices()) {
            SearchResponse indexResponse = searchIndex(indexName, baseRequest);
            allResults.addAll(indexResponse.getResults());
            totalHitsAcrossIndices += indexResponse.getTotalHits();
        }
        
        // Apply pipe commands in sequence
        PipeResult pipeResult = new PipeResult.LogsResult(allResults);
        
        for (PipeCommandSpec spec : parsedQuery.getPipeCommands()) {
            try {
                PipeCommand command = PipeCommandFactory.createCommand(spec);
                
                // Get input for command
                List<SearchResult> input;
                if (pipeResult instanceof PipeResult.LogsResult) {
                    input = ((PipeResult.LogsResult) pipeResult).getResults();
                } else {
                    // Can't pipe from non-logs result
                    log.warn("Cannot pipe from {} result type", pipeResult.getType());
                    break;
                }
                
                pipeResult = command.execute(input);
            } catch (Exception e) {
                log.error("Error executing pipe command: {}", spec.getCommand(), e);
                throw new RuntimeException("Pipe command error: " + e.getMessage(), e);
            }
        }
        
        // Return appropriate response based on result type
        return new SearchResponse(pipeResult);
    }
    
    public void close() throws IOException {
        for (IndexReader reader : indexReaders.values()) {
            reader.close();
        }
        indexReaders.clear();
    }
}
