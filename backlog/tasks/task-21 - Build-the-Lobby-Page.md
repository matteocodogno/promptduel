---
id: TASK-21
title: Build the Lobby Page
status: To Do
assignee: []
created_date: '2026-02-28 19:17'
labels: []
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Allow players to create or join a game session before the game begins, without requiring any account.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Page shows two options: create a new session with role selector (Jailbreaker or Guardian) and join an existing session with a Game ID input
- [ ] #2 After session creation, Game ID displayed prominently with a copy-to-clipboard action and sharing instructions
- [ ] #3 After joining, a waiting room indicator shown until both players signal ready
- [ ] #4 404 (session not found) and 409 (session full) join errors shown as inline user-visible messages
<!-- AC:END -->
