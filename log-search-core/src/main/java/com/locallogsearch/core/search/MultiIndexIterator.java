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

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;

/**
 * Iterator that merges multiple sorted iterators using a priority queue (heap-based selection tree).
 * Only peeks at one element ahead per iterator (O(n) space where n is number of iterators).
 * Each call to next() selects the best element in O(log n) time.
 */
public class MultiIndexIterator implements Iterator<SearchResult> {
    
    private final PriorityQueue<PeekableIterator> heap;
    private final Comparator<SearchResult> comparator;
    
    /**
     * Creates a merging iterator with a custom comparator.
     * The comparator should match the sort order of the input iterators.
     */
    public MultiIndexIterator(List<Iterator<SearchResult>> indexIterators, Comparator<SearchResult> comparator) {
        this.comparator = comparator;
        
        // Priority queue orders iterators by their next element
        this.heap = new PriorityQueue<>(
            Comparator.comparing(PeekableIterator::peek, comparator)
        );
        
        // Initialize heap with all non-empty iterators
        for (Iterator<SearchResult> iterator : indexIterators) {
            if (iterator.hasNext()) {
                heap.offer(new PeekableIterator(iterator));
            }
        }
    }
    
    /**
     * Creates a merging iterator with default score-descending order.
     */
    public MultiIndexIterator(List<Iterator<SearchResult>> indexIterators) {
        this(indexIterators, (a, b) -> Float.compare(b.getScore(), a.getScore()));
    }
    
    @Override
    public boolean hasNext() {
        return !heap.isEmpty();
    }
    
    @Override
    public SearchResult next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        
        // Get iterator with best next element (O(log n))
        PeekableIterator best = heap.poll();
        SearchResult result = best.next();
        
        // If iterator still has elements, re-insert into heap (O(log n))
        if (best.hasNext()) {
            heap.offer(best);
        }
        
        return result;
    }
    
    /**
     * Wrapper that eagerly peeks at the next element for heap ordering.
     * Only buffers one element at a time.
     */
    private static class PeekableIterator {
        private final Iterator<SearchResult> iterator;
        private SearchResult peeked;
        
        PeekableIterator(Iterator<SearchResult> iterator) {
            this.iterator = iterator;
            // Eagerly peek first element for heap ordering
            this.peeked = iterator.next();
        }
        
        boolean hasNext() {
            return peeked != null;
        }
        
        SearchResult peek() {
            return peeked;
        }
        
        SearchResult next() {
            SearchResult result = peeked;
            // Advance to next element if available
            peeked = iterator.hasNext() ? iterator.next() : null;
            return result;
        }
    }
}
