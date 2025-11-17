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

package com.locallogsearch.core.pipe;

import com.locallogsearch.core.search.SearchResult;
import java.util.Iterator;

/**
 * Interface for pipe commands that process search results.
 * Commands receive results as an iterator for memory-efficient streaming processing.
 */
public interface PipeCommand {
    /**
     * Process the input results stream and return transformed output.
     * The iterator is consumed during execution - implementations should
     * process results one at a time when possible to minimize memory usage.
     * 
     * @param input Iterator over search results to process
     * @param totalHits Total number of hits (for statistics/progress tracking)
     * @return Processed pipe result
     */
    PipeResult execute(Iterator<SearchResult> input, int totalHits);
    
    /**
     * Get the command name (e.g., "stats", "chart", "timechart")
     */
    String getName();
}
