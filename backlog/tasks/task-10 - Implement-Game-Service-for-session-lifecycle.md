---
id: TASK-10
title: Implement Game Service for session lifecycle
status: To Do
assignee: []
created_date: '2026-02-28 18:39'
labels: []
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Orchestrate the full session lifecycle from creation through game start and reconnection handling.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 createSession generates a UUID Game ID, assigns the creator's role, persists the session in WAITING_FOR_PLAYERS, and persists the base system prompt as version 1
- [ ] #2 joinSession assigns the second player to the remaining role and rejects with an error if the session is not WAITING_FOR_PLAYERS or already has two players
- [ ] #3 When both players signal ready, session transitions to IN_PROGRESS and GAME_STARTED is broadcast with round 1, current system prompt, and attack vector name
- [ ] #4 Reconnection loads the full session snapshot from the database and sends SESSION_RESUMED to the reconnecting player; PLAYER_RECONNECTED broadcast to the other player
- [ ] #5 WebSocket disconnection events detected and PLAYER_DISCONNECTED broadcast to the remaining player
<!-- AC:END -->
