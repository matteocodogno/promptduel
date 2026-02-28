---
id: TASK-16
title: Configure CORS policy and input validation limits
status: To Do
assignee: []
created_date: '2026-02-28 19:16'
labels: []
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Harden the API against cross-origin abuse and prevent runaway token usage from oversized inputs.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 CORS restricted to the configured frontend origin only; wildcard origins explicitly disallowed
- [ ] #2 injectionText capped at 2,000 characters at the controller boundary; requests exceeding the limit rejected with 400
- [ ] #3 systemPrompt capped at 4,000 characters at the controller boundary; requests exceeding the limit rejected with 400
<!-- AC:END -->
