---
id: TASK-29
title: Write integration tests for application and infrastructure layers
status: To Do
assignee: []
created_date: '2026-02-28 19:18'
labels: []
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Verify that application services, repositories, and HTTP controllers work correctly together with real infrastructure.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Full injection turn tested via RoundService: submit injection → EvaluationService with mocked LlmGateway → InjectionAttempt persisted → correct events published
- [ ] #2 GameService reconnection tested: disconnect notification, DB state unchanged, snapshot assembled from DB matches current session state
- [ ] #3 SessionController endpoints tested via MockMvc: create (200/400), join (200/404/409), summary (200/404), hints (200/400/404)
- [ ] #4 LlmGateway tested with WireMock simulating LiteLLM: correct request format, successful response mapping, correct error types for 500 and timeout
<!-- AC:END -->
