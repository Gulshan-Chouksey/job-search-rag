package com.learnai;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final JobEmbeddingService jobEmbeddingService;

    public AdminController(JobEmbeddingService jobEmbeddingService) {
        this.jobEmbeddingService = jobEmbeddingService;
    }

    @PostMapping("/embed-all")
    public String embedAll() {
        jobEmbeddingService.embedAllJobPostings();
        return "Embedding complete";
    }
}
