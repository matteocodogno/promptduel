---
id: TASK-25
title: Wire frontend and backend together for the full game flow
status: To Do
assignee: []
created_date: '2026-02-28 19:17'
labels: []
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Validate the complete end-to-end game flow by connecting frontend and backend and exercising every path.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Complete path verified: create session → join → both ready → Jailbreaker injects → LLM evaluates → ROUND_ATTEMPT_FAILED delivered → Guardian updates prompt → inject again → GUARDIAN_WIN → ROUND_COMPLETED → repeat rounds 2–4 → GAME_COMPLETED with summary URL
- [ ] #2 Page refresh on either client correctly reconnects via localStorage credentials and receives SESSION_RESUMED with full game state
- [ ] #3 GAME_COMPLETED event delivers the /summary/{gameId} URL and the Summary Page renders the full session data
<!-- AC:END -->
