---
id: TASK-24
title: Build the Summary Page
status: To Do
assignee: []
created_date: '2026-02-28 19:17'
labels: []
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Display a permanent, shareable summary of the completed game session with educational content and image export.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Page accepts gameId from URL path (/summary/{gameId}) and fetches summary data from the REST API on mount
- [ ] #2 Session recap displayed: team roles, total rounds played, and number of injection attempts per round
- [ ] #3 Chronological timeline rendered per round: each injection attempt (text, LLM response, outcome) paired with the prompt update that followed
- [ ] #4 Educational descriptions of all four attack vectors shown along with defensive techniques the Guardians applied
- [ ] #5 Export as Image button uses html2canvas to capture the summary DOM and triggers a file download with no backend involvement
- [ ] #6 Page publicly accessible by Game ID with no login or token required
<!-- AC:END -->
