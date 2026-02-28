---
id: TASK-23
title: Build the Guardian game panel
status: To Do
assignee: []
created_date: '2026-02-28 19:17'
labels: []
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Give the Guardian a panel to view injection breaches and iteratively harden their system prompt.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Current system prompt shown in an editable textarea, initially populated with the base system prompt from GAME_STARTED
- [ ] #2 On ROUND_ATTEMPT_FAILED, the Jailbreaker's injection text and the LLM's response are displayed so the Guardian can learn from the breach
- [ ] #3 System prompt editor and Update Prompt submit button enabled after a failed defense; disabled while evaluation is in progress
- [ ] #4 Two hint buttons provided (Tier 1: Attack Type, Tier 2: Example Defense); hint content displayed inline below the editor after retrieval
- [ ] #5 GUARDIAN_WIN success indicator shown and editing disabled when the round is won
<!-- AC:END -->
