/*
 * MIT License
 *
 * Copyright (c) 2025 Chris Collins
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.locallogsearch.core.search;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Streaming iterator that lazily fetches search results from Lucene.
 * Fetches documents in batches to reduce I/O overhead while avoiding
 * loading all results into memory at once.
 */
public class LuceneResultIterator implements Iterator<SearchResult> {
    private static final Logger log = LoggerFactory.getLogger(LuceneResultIterator.class);
    private static final int BATCH_SIZE = 1000; // Fetch documents in batches
    
    private final IndexSearcher searcher;
    private final ScoreDoc[] scoreDocs;
    private final String indexName;
    private int currentIndex = 0;
    
    // Batch cache to reduce Lucene I/O calls
    private SearchResult[] currentBatch = null;
    private int batchStartIndex = 0;
    private int batchSize = 0;
    
    public LuceneResultIterator(IndexSearcher searcher, TopDocs topDocs, String indexName) {
        this.searcher = searcher;
        this.scoreDocs = topDocs.scoreDocs;
        this.indexName = indexName;
    }
    
    @Override
    public boolean hasNext() {
        return currentIndex < scoreDocs.length;
    }
    
    @Override
    public SearchResult next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        
        try {
            // Check if we need to fetch a new batch
            if (currentBatch == null || currentIndex >= batchStartIndex + batchSize) {
                fetchNextBatch();
            }
            
            // Return from current batch
            int batchOffset = currentIndex - batchStartIndex;
            SearchResult result = currentBatch[batchOffset];
            currentIndex++;
            return result;
            
        } catch (IOException e) {
            log.error("Error fetching search result at index {}", currentIndex, e);
            throw new RuntimeException("Failed to fetch search result", e);
        }
    }
    
    /**
     * Fetch the next batch of documents from Lucene.
     * This reduces I/O overhead by fetching multiple documents at once.
     */
    private void fetchNextBatch() throws IOException {
        batchStartIndex = currentIndex;
        int remainingDocs = scoreDocs.length - currentIndex;
        batchSize = Math.min(BATCH_SIZE, remainingDocs);
        
        currentBatch = new SearchResult[batchSize];
        
        // Fetch batch of documents
        for (int i = 0; i < batchSize; i++) {
            ScoreDoc scoreDoc = scoreDocs[batchStartIndex + i];
            Document doc = searcher.doc(scoreDoc.doc);
            currentBatch[i] = documentToSearchResult(doc, indexName, scoreDoc.score);
        }
    }
    
    /**
     * Convert a Lucene Document to a SearchResult.
     */
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
        
        // Extract all other fields (skip _num variants)
        Map<String, String> fields = new HashMap<>();
        doc.getFields().forEach(field -> {
            String name = field.name();
            if (!name.equals("raw_text") && !name.equals("source") && !name.equals("timestamp") 
                && !name.endsWith("_exact") && !name.endsWith("_num")) {
                fields.put(name, field.stringValue());
            }
        });
        result.setFields(fields);
        
        return result;
    }
}
