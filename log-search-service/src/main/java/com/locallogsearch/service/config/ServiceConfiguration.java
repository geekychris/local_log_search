package com.locallogsearch.service.config;

import com.locallogsearch.core.config.IndexConfig;
import com.locallogsearch.core.index.IndexManager;
import com.locallogsearch.core.search.SearchService;
import com.locallogsearch.core.tailer.TailerManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceConfiguration {
    
    @Value("${index.base-directory}")
    private String baseDirectory;
    
    @Value("${index.commit-interval-seconds}")
    private int commitIntervalSeconds;
    
    @Value("${index.max-buffered-docs}")
    private int maxBufferedDocs;
    
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
    public TailerManager tailerManager(IndexManager indexManager) {
        return new TailerManager(indexManager);
    }
    
    @Bean
    public SearchService searchService(IndexConfig indexConfig) {
        return new SearchService(indexConfig);
    }
}
