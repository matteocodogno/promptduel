---
id: TASK-14
title: Implement Summary Service
status: To Do
assignee: []
created_date: '2026-02-28 19:16'
labels: []
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Assemble and expose the complete session summary data for the permanent public summary URL.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Full session summary assembled from GameSession, SystemPromptVersion, and InjectionAttempt data for the given Game ID
- [ ] #2 Summary includes: session recap (teams, total rounds, attempts per round), chronological timeline (each injection attempt with outcome and prompt update), attack vector descriptions, and defensive techniques applied per round
- [ ] #3 Summary is available by Game ID with no authentication and no time limit
<!-- AC:END -->
