---
id: TASK-28
title: Write unit tests for domain and application logic
status: To Do
assignee: []
created_date: '2026-02-28 19:18'
labels: []
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Verify domain and application logic in isolation to catch bugs early before integration.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 SessionStateMachine: all valid state transitions succeed, all invalid transitions return errors; both top-level and per-round sub-states covered
- [ ] #2 EvaluationService: pattern-match returns GUARDIAN_WIN for vulnerability-flagged responses, JAILBREAKER_WIN for no-issues responses, AMBIGUOUS for unclear; AMBIGUOUS triggers a second judge LLM call
- [ ] #3 AttackVectorRegistry: correct vector data retrieved for rounds 1–4 and base system prompt returned without error
- [ ] #4 GameService.createSession and joinSession: correct role assignment, second-player capacity enforcement, rejection of join on full sessions and wrong session status
<!-- AC:END -->
