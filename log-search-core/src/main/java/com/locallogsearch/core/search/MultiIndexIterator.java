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

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Iterator that chains multiple LuceneResultIterators together,
 * streaming results from multiple indices sequentially.
 */
public class MultiIndexIterator implements Iterator<SearchResult> {
    
    private final List<Iterator<SearchResult>> indexIterators;
    private int currentIteratorIndex = 0;
    
    public MultiIndexIterator(List<Iterator<SearchResult>> indexIterators) {
        this.indexIterators = indexIterators;
    }
    
    @Override
    public boolean hasNext() {
        // Find next iterator with results
        while (currentIteratorIndex < indexIterators.size()) {
            if (indexIterators.get(currentIteratorIndex).hasNext()) {
                return true;
            }
            currentIteratorIndex++;
        }
        return false;
    }
    
    @Override
    public SearchResult next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return indexIterators.get(currentIteratorIndex).next();
    }
}
