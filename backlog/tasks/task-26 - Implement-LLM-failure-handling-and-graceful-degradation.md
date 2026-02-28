---
id: TASK-26
title: Implement LLM failure handling and graceful degradation
status: To Do
assignee: []
created_date: '2026-02-28 19:18'
labels: []
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Prevent LLM errors from corrupting game state and provide actionable feedback to players when inference fails.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 LLM Gateway errors (502, 504, parse failure) keep the round in EVALUATING state; round is never advanced and game state is not corrupted
- [ ] #2 Typed LLM_ERROR event broadcast to both players so the Jailbreaker sees a human-readable retry prompt
- [ ] #3 Every LLM Gateway call logged with gameId, roundNumber, attemptNumber, durationMs, evaluation method, and outcome using structured logging
- [ ] #4 AMBIGUOUS classifications logged separately for accuracy auditing
<!-- AC:END -->
