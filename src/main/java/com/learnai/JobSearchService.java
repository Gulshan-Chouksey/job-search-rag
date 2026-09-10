package com.learnai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class JobSearchService {

    private final EmbeddingModel embeddingModel;
    private final JdbcTemplate jdbcTemplate;
    private final ChatClient chatClient;

    public JobSearchService(EmbeddingModel embeddingModel, JdbcTemplate jdbcTemplate, ChatClient.Builder builder) {
        this.embeddingModel = embeddingModel;
        this.jdbcTemplate = jdbcTemplate;
        this.chatClient = builder.build();
    }

    private static final String SYSTEM_PROMPT = """
            You are a job search assistant. Answer the user's question using ONLY
            the job postings provided below as context. If the context doesn't
            contain enough information to answer, say so clearly instead of guessing.
            """;

    public String askAboutJobs(String userQuestion) {
        // Step 1: embed the question the same way we embedded job descriptions
        float[] questionEmbedding = embeddingModel.embed(userQuestion);

        // Step 2: find the 3 closest job postings using pgvector's cosine distance operator
        List<Map<String, Object>> matches = jdbcTemplate.queryForList(
                "SELECT title, description FROM job_postings " +
                        "ORDER BY embedding <=> ?::vector LIMIT 3",
                Arrays.toString(questionEmbedding)
        );

        // Step 3: build a context block from the retrieved postings
        StringBuilder context = new StringBuilder();
        for (Map<String, Object> match : matches) {
            context.append("Title: ").append(match.get("title")).append("\n");
            context.append("Description: ").append(match.get("description")).append("\n\n");
        }

        // Step 4: ask Gemini to answer using that context
        String userPrompt = "Context (relevant job postings):\n" + context +
                "\nQuestion: " + userQuestion;

        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(userPrompt)
                .call()
                .content();
    }
}
