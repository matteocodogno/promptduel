---
id: TASK-27
title: Expose health monitoring and validate security settings
status: To Do
assignee: []
created_date: '2026-02-28 19:18'
labels: []
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Verify system health and confirm security configuration is correct so the application is production-safe.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Spring Actuator /actuator/health endpoint exposes a custom LiteLLM reachability indicator
- [ ] #2 CORS restricted to the configured frontend origin in both REST and WebSocket configuration; wildcard origins not present
- [ ] #3 LiteLLM API key read from an environment variable and never appears in logs, responses, or frontend-accessible endpoints
<!-- AC:END -->
