---
id: TASK-5
title: Build the Attack Vector Registry
status: To Do
assignee: []
created_date: '2026-02-28 18:37'
labels: []
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Provide the static game content and educational material that drives all four rounds of gameplay.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Four attack vectors defined in strict order: Direct Override, Role Confusion, Context Manipulation, and Indirect Injection
- [ ] #2 Each vector stores: round number, name, educational description, tier-1 hint text, and tier-2 example defensive prompt
- [ ] #3 Intentionally weak base system prompt included, designed to be defeated by round-1 immediately
- [ ] #4 Static vulnerable SQL injection code sample included for use as the code-review input in all evaluation calls
- [ ] #5 Registry is read-only with zero I/O or external dependencies
<!-- AC:END -->
