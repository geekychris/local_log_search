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

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class MultiIndexIteratorTest {

    private SearchResult createResult(String index, float score, Instant timestamp) {
        SearchResult result = new SearchResult();
        result.setIndexName(index);
        result.setScore(score);
        result.setTimestamp(timestamp);
        result.setRawText("test");
        return result;
    }

    @Test
    void testMergesSortedIteratorsByScoreDescending() {
        // Create two sorted iterators (score descending)
        List<SearchResult> iter1Results = Arrays.asList(
            createResult("index1", 10.0f, Instant.now()),
            createResult("index1", 5.0f, Instant.now())
        );
        List<SearchResult> iter2Results = Arrays.asList(
            createResult("index2", 8.0f, Instant.now()),
            createResult("index2", 3.0f, Instant.now())
        );

        List<Iterator<SearchResult>> iterators = Arrays.asList(
            iter1Results.iterator(),
            iter2Results.iterator()
        );

        // Default comparator is score descending
        MultiIndexIterator multiIter = new MultiIndexIterator(iterators);

        // Collect results
        List<SearchResult> results = new ArrayList<>();
        multiIter.forEachRemaining(results::add);

        // Verify merged order: 10, 8, 5, 3
        assertEquals(4, results.size());
        assertEquals(10.0f, results.get(0).getScore(), 0.001f);
        assertEquals(8.0f, results.get(1).getScore(), 0.001f);
        assertEquals(5.0f, results.get(2).getScore(), 0.001f);
        assertEquals(3.0f, results.get(3).getScore(), 0.001f);
    }

    @Test
    void testMergesSortedIteratorsByTimestampAscending() {
        Instant t1 = Instant.ofEpochSecond(1000);
        Instant t2 = Instant.ofEpochSecond(2000);
        Instant t3 = Instant.ofEpochSecond(3000);
        Instant t4 = Instant.ofEpochSecond(4000);

        // Create two sorted iterators (timestamp ascending)
        List<SearchResult> iter1Results = Arrays.asList(
            createResult("index1", 1.0f, t1),
            createResult("index1", 1.0f, t3)
        );
        List<SearchResult> iter2Results = Arrays.asList(
            createResult("index2", 1.0f, t2),
            createResult("index2", 1.0f, t4)
        );

        List<Iterator<SearchResult>> iterators = Arrays.asList(
            iter1Results.iterator(),
            iter2Results.iterator()
        );

        // Custom comparator for timestamp ascending
        Comparator<SearchResult> comparator = Comparator.comparing(SearchResult::getTimestamp);
        MultiIndexIterator multiIter = new MultiIndexIterator(iterators, comparator);

        // Collect results
        List<SearchResult> results = new ArrayList<>();
        multiIter.forEachRemaining(results::add);

        // Verify merged order: t1, t2, t3, t4
        assertEquals(4, results.size());
        assertEquals(t1, results.get(0).getTimestamp());
        assertEquals(t2, results.get(1).getTimestamp());
        assertEquals(t3, results.get(2).getTimestamp());
        assertEquals(t4, results.get(3).getTimestamp());
    }

    @Test
    void testMergesSortedIteratorsByTimestampDescending() {
        Instant t1 = Instant.ofEpochSecond(1000);
        Instant t2 = Instant.ofEpochSecond(2000);
        Instant t3 = Instant.ofEpochSecond(3000);
        Instant t4 = Instant.ofEpochSecond(4000);

        // Create two sorted iterators (timestamp descending)
        List<SearchResult> iter1Results = Arrays.asList(
            createResult("index1", 1.0f, t4),
            createResult("index1", 1.0f, t2)
        );
        List<SearchResult> iter2Results = Arrays.asList(
            createResult("index2", 1.0f, t3),
            createResult("index2", 1.0f, t1)
        );

        List<Iterator<SearchResult>> iterators = Arrays.asList(
            iter1Results.iterator(),
            iter2Results.iterator()
        );

        // Custom comparator for timestamp descending
        Comparator<SearchResult> comparator = (a, b) -> b.getTimestamp().compareTo(a.getTimestamp());
        MultiIndexIterator multiIter = new MultiIndexIterator(iterators, comparator);

        // Collect results
        List<SearchResult> results = new ArrayList<>();
        multiIter.forEachRemaining(results::add);

        // Verify merged order: t4, t3, t2, t1
        assertEquals(4, results.size());
        assertEquals(t4, results.get(0).getTimestamp());
        assertEquals(t3, results.get(1).getTimestamp());
        assertEquals(t2, results.get(2).getTimestamp());
        assertEquals(t1, results.get(3).getTimestamp());
    }

    @Test
    void testHandlesEmptyIterators() {
        List<Iterator<SearchResult>> iterators = Arrays.asList(
            Collections.emptyIterator(),
            Arrays.asList(createResult("index1", 5.0f, Instant.now())).iterator(),
            Collections.emptyIterator()
        );

        MultiIndexIterator multiIter = new MultiIndexIterator(iterators);

        List<SearchResult> results = new ArrayList<>();
        multiIter.forEachRemaining(results::add);

        assertEquals(1, results.size());
        assertEquals(5.0f, results.get(0).getScore(), 0.001f);
    }

    @Test
    void testHandlesAllEmptyIterators() {
        List<Iterator<SearchResult>> iterators = Arrays.asList(
            Collections.emptyIterator(),
            Collections.emptyIterator()
        );

        MultiIndexIterator multiIter = new MultiIndexIterator(iterators);

        assertFalse(multiIter.hasNext());
    }

    @Test
    void testMergesThreeIterators() {
        // Create three sorted iterators
        List<SearchResult> iter1Results = Arrays.asList(
            createResult("index1", 10.0f, Instant.now()),
            createResult("index1", 4.0f, Instant.now())
        );
        List<SearchResult> iter2Results = Arrays.asList(
            createResult("index2", 8.0f, Instant.now()),
            createResult("index2", 3.0f, Instant.now())
        );
        List<SearchResult> iter3Results = Arrays.asList(
            createResult("index3", 6.0f, Instant.now()),
            createResult("index3", 2.0f, Instant.now())
        );

        List<Iterator<SearchResult>> iterators = Arrays.asList(
            iter1Results.iterator(),
            iter2Results.iterator(),
            iter3Results.iterator()
        );

        MultiIndexIterator multiIter = new MultiIndexIterator(iterators);

        List<SearchResult> results = new ArrayList<>();
        multiIter.forEachRemaining(results::add);

        // Verify merged order: 10, 8, 6, 4, 3, 2
        assertEquals(6, results.size());
        assertEquals(10.0f, results.get(0).getScore(), 0.001f);
        assertEquals(8.0f, results.get(1).getScore(), 0.001f);
        assertEquals(6.0f, results.get(2).getScore(), 0.001f);
        assertEquals(4.0f, results.get(3).getScore(), 0.001f);
        assertEquals(3.0f, results.get(4).getScore(), 0.001f);
        assertEquals(2.0f, results.get(5).getScore(), 0.001f);
    }
}
