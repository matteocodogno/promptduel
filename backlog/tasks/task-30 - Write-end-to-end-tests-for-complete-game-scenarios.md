---
id: TASK-30
title: Write end-to-end tests for complete game scenarios
status: To Do
assignee: []
created_date: '2026-02-28 19:18'
labels: []
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Verify the full user journey works correctly from session creation through summary export.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Full two-player session simulated: create → join → ready → inject (JAILBREAKER_WIN) → update prompt → inject again (GUARDIAN_WIN) → advance all four rounds → GAME_COMPLETED received → summary URL accessible with valid data
- [ ] #2 Disconnection and reconnection mid-round simulated: disconnected player's opponent receives PLAYER_DISCONNECTED; on reconnect SESSION_RESUMED restores correct state; opponent receives PLAYER_RECONNECTED
- [ ] #3 Summary Page verified to render all timeline data; html2canvas image export produces a non-empty output
<!-- AC:END -->
