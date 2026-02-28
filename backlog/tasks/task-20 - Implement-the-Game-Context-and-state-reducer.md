---
id: TASK-20
title: Implement the Game Context and state reducer
status: To Do
assignee: []
created_date: '2026-02-28 19:17'
labels: []
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Centralise all game state and provide typed actions to every React component via Context.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 GameState shape defined: gameId, playerId, myRole, status, currentRound, currentSystemPrompt, opponentConnected, rounds with attempts, and pendingEvaluation flag
- [ ] #2 useReducer dispatcher handles all incoming GameEvent types immutably: PLAYER_JOINED, GAME_STARTED, ROUND_ATTEMPT_FAILED, PROMPT_UPDATED, ROUND_COMPLETED, GAME_COMPLETED, PLAYER_DISCONNECTED, PLAYER_RECONNECTED, SESSION_RESUMED, HINT_RECEIVED
- [ ] #3 Context actions exposed: createSession, joinSession, setReady, submitInjection, updateSystemPrompt, requestHint
- [ ] #4 pendingEvaluation set to true on injection submit and false when any round outcome event arrives
<!-- AC:END -->
