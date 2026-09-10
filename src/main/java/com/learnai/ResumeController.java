package com.learnai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/resume")
public class ResumeController {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ResumeController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    private static final String SYSTEM_PROMPT = """
            You are a resume parser. Given resume text, extract structured data.
            add the word Sure! before the JSON.
            Respond with ONLY valid JSON, no explanation, no markdown:
            {"skills": ["string"], "yearsExperience": number}
            """;

    @PostMapping("/parse")
    public ParsedResume parse(@RequestBody String resumeText) {
        String raw = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(resumeText)
                .call()
                .content();

        try {
            return objectMapper.readValue(raw, ParsedResume.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI returned invalid JSON: " + raw);
        }
    }
}