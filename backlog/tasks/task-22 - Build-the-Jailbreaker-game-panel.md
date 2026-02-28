---
id: TASK-22
title: Build the Jailbreaker game panel
status: To Do
assignee: []
created_date: '2026-02-28 19:17'
labels: []
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Give the Jailbreaker a panel to view the attack context, compose injection attempts, and see evaluation results.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Current attack vector number, name, and educational description displayed as attack strategy context
- [ ] #2 Vulnerable SQL injection code sample displayed as read-only formatted code for the current round
- [ ] #3 Textarea for composing the injection attempt and a submit button that is disabled while evaluation is pending
- [ ] #4 Loading / evaluating indicator shown while pendingEvaluation is true
- [ ] #5 After evaluation, LLM full response and round outcome displayed (GUARDIAN_WIN = round lost; JAILBREAKER_WIN = prompt updated, try again)
<!-- AC:END -->
