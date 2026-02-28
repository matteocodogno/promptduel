---
id: TASK-7
title: Implement the LLM Gateway adapter
status: To Do
assignee: []
created_date: '2026-02-28 18:39'
labels: []
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Integrate with the LiteLLM proxy so the backend can call the LLM for code review evaluation.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 HTTP client sends POST /chat/completions to LiteLLM in OpenAI-compatible format with system and user message roles
- [ ] #2 Successful 200 response mapped to a structured result with response text, model name, and token count
- [ ] #3 HTTP 4xx/5xx responses and timeouts mapped to distinct typed error values (HttpError, Timeout, ParseError)
- [ ] #4 LiteLLM host, port, model name, and timeout read from application properties and never hard-coded
<!-- AC:END -->
