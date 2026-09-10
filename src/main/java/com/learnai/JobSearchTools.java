package com.learnai;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class JobSearchTools {

    private final EmbeddingModel embeddingModel;
    private final JdbcTemplate jdbcTemplate;

    public JobSearchTools(EmbeddingModel embeddingModel, JdbcTemplate jdbcTemplate) {
        this.embeddingModel = embeddingModel;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Tool(description = "Search job postings by semantic meaning. Use this when the user describes skills, experience, or asks what job fits them.")
    public String searchJobPostings(String query) {
        float[] queryEmbedding = embeddingModel.embed(query);

        List<Map<String, Object>> matches = jdbcTemplate.queryForList(
                "SELECT title, description FROM job_postings " +
                        "ORDER BY embedding <=> ?::vector LIMIT 3",
                Arrays.toString(queryEmbedding)
        );

        StringBuilder result = new StringBuilder();
        for (Map<String, Object> match : matches) {
            result.append("Title: ").append(match.get("title")).append("\n");
            result.append("Description: ").append(match.get("description")).append("\n\n");
        }
        return result.toString();
    }

    @Tool(description = "Estimate a rough salary range in INR for a given years of experience in Java backend development.")
    public String estimateSalary(int yearsExperience) {
        int baseLPA = 4 + (yearsExperience * 2);
        return String.format("Estimated range: ₹%d LPA to ₹%d LPA", baseLPA, baseLPA + 2);
    }
}
