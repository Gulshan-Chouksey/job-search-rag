package com.learnai;

import java.util.List;

public record ParsedResume(
        List<String> skills,
        Integer yearsExperience
) {}
