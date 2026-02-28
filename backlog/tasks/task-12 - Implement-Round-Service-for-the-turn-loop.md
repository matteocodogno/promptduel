---
id: TASK-12
title: Implement Round Service for the turn loop
status: To Do
assignee: []
created_date: '2026-02-28 19:16'
labels: []
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Execute the round turn loop: accept injections, evaluate outcomes, and manage system prompt updates.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Injection submissions validated: session must be IN_PROGRESS and caller must be JAILBREAKER before delegating to Evaluation Service
- [ ] #2 Each InjectionAttempt persisted with injection text, full LLM response, evaluation method, outcome, and system prompt version ID
- [ ] #3 On JAILBREAKER_WIN: ROUND_ATTEMPT_FAILED broadcast with injection text, LLM response, and attempt number to both players
- [ ] #4 System prompt updates accepted from GUARDIAN only when current round outcome is JAILBREAKER_WIN; new SystemPromptVersion persisted with incremented version number; PROMPT_UPDATED broadcast
- [ ] #5 On GUARDIAN_WIN: round counter advanced; session marked COMPLETED and GAME_COMPLETED broadcast after round 4; otherwise ROUND_COMPLETED broadcast
<!-- AC:END -->
