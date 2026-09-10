package com.learnai;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
public class JobAgentController {

    private final JobAgentService jobAgentService;

    public JobAgentController(JobAgentService jobAgentService) {
        this.jobAgentService = jobAgentService;
    }

    @PostMapping("/ask")
    public String ask(@RequestBody String question) {
        return jobAgentService.ask(question);
    }
}
