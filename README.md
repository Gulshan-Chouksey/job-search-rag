# Job Search RAG

A Retrieval-Augmented Generation (RAG) pipeline that answers natural-language questions about job postings using semantic search — not keyword matching.

Built on top of the data model from my [HireHub](https://github.com/Gulshan-Chouksey/hirehub) job portal project, as a focused exploration of how to add AI-powered search to a Spring Boot backend.

## What it does

Ask a question like:

> "I know a high level OOP language and building backend web services, what job fits me?"

...and it correctly matches the **Java Backend Developer** posting — even though the question shares zero words with the posting's description ("Spring Boot," "REST APIs," "MySQL"). The match happens on *meaning*, not spelling.

A plain SQL `LIKE '%keyword%'` query would return nothing for that question. This project proves the retrieval is doing genuine semantic search.

## How it works

```
User question
    → embedded into a 768-dimension vector (Gemini embedding model)
    → compared against pre-embedded job postings using cosine similarity (pgvector)
    → top 3 closest matches retrieved
    → matches injected into a prompt as context
    → Gemini generates an answer grounded in that context
```

## Proof of semantic (not keyword) matching

**Query:** *"I know a high level oop language and building backend web services, what job fits me?"*

**Result:** Correctly surfaced the Java Backend Developer posting, connecting:
- "high level OOP language" → Java
- "backend web services" → Spring Boot / REST APIs

No literal word overlap between the question and the matched posting's text existed.

## Tech stack

- **Spring Boot** + **Spring AI** — application framework and LLM/embedding integration
- **Google Gemini** — `gemini-2.5-flash` for chat generation, `gemini-embedding-001` for embeddings (768 dimensions)
- **PostgreSQL + pgvector** — vector storage and cosine-similarity search, running locally via Docker
- **Docker** — local Postgres/pgvector environment

## Architecture decisions

- **pgvector over a separate vector database** — keeps relational data (job postings) and their embeddings in one database, queryable together with normal SQL. Avoids the dual-write sync problem of maintaining two separate data stores.
- **System prompt instructs the model to say when it doesn't know** — a deliberate anti-hallucination guard: if retrieved context doesn't answer the question, the model is instructed to say so rather than guess.
- **Every LLM response is treated as untrusted input** — parsed into a validated DTO with explicit error handling, never trusted as a guaranteed contract.

## Bugs hit along the way (and why)

- **Missing `EmbeddingModel` bean at startup** — the chat model and embedding model are separate Spring AI starters (`spring-ai-starter-model-google-genai` vs. `...-embedding`); adding one doesn't auto-include the other.
- **`ClassCastException`: Integer cannot be cast to Long** — Postgres's `SERIAL` type returns as Java `Integer` via JDBC, unlike MySQL's typical `Long` mapping for auto-increment IDs. A reminder that JDBC type behavior isn't identical across databases.

## Setup

1. Run Postgres with pgvector via Docker:
   ```
   docker run --name hirehub-pgvector -e POSTGRES_PASSWORD=yourpassword -e POSTGRES_DB=hirehub -p 5432:5432 -d pgvector/pgvector:pg16
   ```
2. Enable the extension: `CREATE EXTENSION IF NOT EXISTS vector;`
3. Set `GEMINI_API_KEY` as an environment variable (get one free at [aistudio.google.com](https://aistudio.google.com))
4. Run the Spring Boot app — endpoints:
   - `POST /api/admin/embed-all` — generate embeddings for job postings missing one
   - `POST /api/jobs/ask` — ask a natural-language question about job postings

## Agent with tool-calling

Extended the RAG pipeline into an agent that can decide, per-question, which of two tools to call — rather than always running a hardcoded search.

**Tools available to the model:**
- `searchJobPostings(query)` — semantic search over job postings (the RAG logic above, exposed as a callable tool)
- `estimateSalary(yearsExperience)` — a simple salary-range calculator

The model chooses which tool(s) to call based on the question — including calling **both** in a single response when a question needs it (e.g., "what job fits me and what salary should I expect?").

### A hallucination I caught and fixed

First version had a real bug worth documenting rather than hiding: when asked a combined job-fit + salary question, the model correctly called both tools, but then **invented two job titles that don't exist in the database** ("Cloud Support Engineer," "Backend Developer with Cloud/DevOps focus") alongside the one real match, blending retrieved data with hallucinated suggestions.

**Fix:** added an explicit system prompt instructing the model to use *only* tool-returned results and to clearly label real data vs. any general advice:

```java
private static final String SYSTEM_PROMPT = """
        You are a job search and career assistant. When discussing job fit,
        use ONLY postings returned by the searchJobPostings tool — never invent
        or suggest job titles that were not in the tool's results. If asked about
        salary, use ONLY the estimateSalary tool's output. Clearly distinguish
        real search results from your own general advice, if you give any.
        """;
```

**After the fix**, the same question returned only real postings from the database, explicitly labeled: *"The job titles above are strictly from our active job search database, and the salary range is generated by our salary estimation tool."*

This is the same lesson as the RAG hallucination guard, applied to a more complex multi-tool flow — retrieval grounding isn't automatic just because a tool call happened; the model still needs explicit instruction not to pad results with invented content.

## What's next

- Expand beyond 5 sample postings to a realistic dataset
- Add evals — a small set of test questions with expected matches, to catch retrieval regressions
- Tighten `searchJobPostings` with a similarity-distance threshold, so weakly-relevant matches are filtered before the model sees them, rather than relying on the model to editorialize about fit
- Explore multi-step agentic reasoning beyond single-turn tool selection