---
id: TASK-13
title: Implement Hint Service
status: To Do
assignee: []
created_date: '2026-02-28 19:16'
labels: []
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Serve educational hints to the Guardian when they need help defending against an attack vector.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Tier-1 hint returns the attack-type description for the current round's vector from the Attack Vector Registry
- [ ] #2 Tier-2 hint returns the example defensive prompt for the current round's vector
- [ ] #3 Requesting player is validated to be the Guardian and the session must be IN_PROGRESS before any hint is served
<!-- AC:END -->
