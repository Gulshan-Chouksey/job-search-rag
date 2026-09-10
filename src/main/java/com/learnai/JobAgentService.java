package com.learnai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class JobAgentService {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = """
        You are a job search and career assistant. When discussing job fit,
        use ONLY postings returned by the searchJobPostings tool — never invent
        or suggest job titles that were not in the tool's results. If asked about
        salary, use ONLY the estimateSalary tool's output. Clearly distinguish
        real search results from your own general advice, if you give any.
        """;

    public JobAgentService(ChatClient.Builder builder, JobSearchTools jobSearchTools) {
        this.chatClient = builder
                .defaultTools(jobSearchTools)
                .build();
    }

    public String ask(String userQuestion) {
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(userQuestion)
                .call()
                .content();
    }
}