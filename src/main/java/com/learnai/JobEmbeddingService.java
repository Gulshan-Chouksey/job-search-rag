package com.learnai;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class JobEmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final JdbcTemplate jdbcTemplate;

    public JobEmbeddingService(EmbeddingModel embeddingModel, JdbcTemplate jdbcTemplate) {
        this.embeddingModel = embeddingModel;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void embedAllJobPostings() {
        List<Map<String, Object>> jobs = jdbcTemplate.queryForList(
                "SELECT id, description FROM job_postings WHERE embedding IS NULL"
        );

        for (Map<String, Object> job : jobs) {
            Integer id = (Integer) job.get("id");
            String description = (String) job.get("description");

            float[] embedding = embeddingModel.embed(description);

            jdbcTemplate.update(
                    "UPDATE job_postings SET embedding = ?::vector WHERE id = ?",
                    Arrays.toString(embedding), id
            );
        }
    }
}