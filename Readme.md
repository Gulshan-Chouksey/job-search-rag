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

## What's next

- Expand beyond 5 sample postings to a realistic dataset
- Add evals — a small set of test questions with expected matches, to catch retrieval regressions
- Explore agentic tool-calling on top of this (multi-step reasoning, not just single-shot retrieval)