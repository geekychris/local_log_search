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

package com.locallogsearch.service.config;

import com.locallogsearch.core.config.IndexConfig;
import com.locallogsearch.core.config.LogSourceConfig;
import com.locallogsearch.core.index.IndexManager;
import com.locallogsearch.core.search.SearchService;
import com.locallogsearch.core.tailer.FileTailerState;
import com.locallogsearch.core.tailer.TailerManager;
import com.locallogsearch.service.model.TailerState;
import com.locallogsearch.service.repository.LogSourceRepository;
import com.locallogsearch.service.repository.TailerStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.nio.file.Paths;
import java.util.Map;

@Configuration
public class ServiceConfiguration {
    private static final Logger log = LoggerFactory.getLogger(ServiceConfiguration.class);
    
    @Value("${index.base-directory}")
    private String baseDirectory;
    
    @Value("${index.commit-interval-seconds}")
    private int commitIntervalSeconds;
    
    @Value("${index.max-buffered-docs}")
    private int maxBufferedDocs;
    
    @Value("${state.directory:./state}")
    private String stateDirectory;
    
    @Bean
    public IndexConfig indexConfig() {
        IndexConfig config = new IndexConfig();
        config.setBaseDirectory(baseDirectory);
        config.setCommitIntervalSeconds(commitIntervalSeconds);
        config.setMaxBufferedDocs(maxBufferedDocs);
        return config;
    }
    
    @Bean
    public IndexManager indexManager(IndexConfig indexConfig) {
        return new IndexManager(indexConfig);
    }
    
    @Bean
    public LogSourceRepository logSourceRepository() {
        return new LogSourceRepository(Paths.get(stateDirectory));
    }
    
    @Bean
    public TailerStateRepository tailerStateRepository() {
        return new TailerStateRepository(Paths.get(stateDirectory));
    }
    
    @Bean
    public TailerManager tailerManager(IndexManager indexManager, TailerStateRepository stateRepository) {
        TailerManager manager = new TailerManager(indexManager);
        
        // Set up checkpoint callback to persist tailer state
        manager.setCheckpointCallback((sourceId, state) -> {
            TailerState persistentState = new TailerState(
                state.getFilePath(),
                state.getFilePointer(),
                state.getLastModifiedTime(),
                state.getFileSize(),
                state.getFileKey()
            );
            stateRepository.save(sourceId, persistentState);
        });
        
        return manager;
    }
    
    @Bean
    public SearchService searchService(IndexConfig indexConfig) {
        return new SearchService(indexConfig);
    }
}
