---
id: TASK-6
title: Build the Session State Machine
status: To Do
assignee: []
created_date: '2026-02-28 18:39'
labels: []
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Enforce valid game state transitions to prevent illegal session state changes and maintain data consistency.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Valid top-level states modelled: WAITING_FOR_PLAYERS → IN_PROGRESS → COMPLETED with no other transitions allowed
- [ ] #2 Per-round sub-states modelled: ROUND_ACTIVE → EVALUATING → ROUND_RESULT (JAILBREAKER_WIN or GUARDIAN_WIN)
- [ ] #3 Illegal state transitions return a typed error instead of silently mutating state
- [ ] #4 All logic is pure with no I/O or side effects
- [ ] #5 GameSession JPA entity marked with an optimistic-lock @Version field to prevent concurrent update conflicts
<!-- AC:END -->
