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

/**
 * Iterator that merges multiple sorted iterators using a selection tree approach.
 * Only peeks at one element ahead per iterator (O(n) space where n is number of iterators).
 * Each call to next() selects the best element among the peeked values.
 */
public class MultiIndexIterator implements Iterator<SearchResult> {
    
    private final PeekableIterator[] iterators;
    private final Comparator<SearchResult> comparator;
    
    /**
     * Creates a merging iterator with a custom comparator.
     * The comparator should match the sort order of the input iterators.
     */
    public MultiIndexIterator(List<Iterator<SearchResult>> indexIterators, Comparator<SearchResult> comparator) {
        this.comparator = comparator;
        this.iterators = new PeekableIterator[indexIterators.size()];
        
        // Wrap each iterator in a peekable wrapper
        for (int i = 0; i < indexIterators.size(); i++) {
            this.iterators[i] = new PeekableIterator(indexIterators.get(i));
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
        // Check if any iterator has a next element
        for (PeekableIterator iter : iterators) {
            if (iter.hasNext()) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public SearchResult next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        
        // Selection tree: find the iterator with the best next element
        int bestIndex = -1;
        SearchResult bestResult = null;
        
        for (int i = 0; i < iterators.length; i++) {
            if (iterators[i].hasNext()) {
                SearchResult candidate = iterators[i].peek();
                if (bestResult == null || comparator.compare(candidate, bestResult) < 0) {
                    bestResult = candidate;
                    bestIndex = i;
                }
            }
        }
        
        // Consume the best element from its iterator
        return iterators[bestIndex].next();
    }
    
    /**
     * Wrapper that allows peeking at the next element without consuming it.
     * Only buffers one element at a time.
     */
    private static class PeekableIterator {
        private final Iterator<SearchResult> iterator;
        private SearchResult peeked;
        private boolean hasPeeked;
        
        PeekableIterator(Iterator<SearchResult> iterator) {
            this.iterator = iterator;
            this.hasPeeked = false;
        }
        
        boolean hasNext() {
            return hasPeeked || iterator.hasNext();
        }
        
        SearchResult peek() {
            if (!hasPeeked) {
                peeked = iterator.next();
                hasPeeked = true;
            }
            return peeked;
        }
        
        SearchResult next() {
            if (!hasPeeked) {
                return iterator.next();
            }
            SearchResult result = peeked;
            peeked = null;
            hasPeeked = false;
            return result;
        }
    }
}
