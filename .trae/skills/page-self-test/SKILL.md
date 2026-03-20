---
name: "page-self-test"
description: "Runs a reusable web page self-test flow. Invoke when changing any page UI/interaction/API wiring and before handing off frontend work."
---

# Page Self Test

## Goal
Provide a consistent self-test checklist for page-level verification after frontend changes.

## Invoke When
- A page UI layout/style is modified
- A button/form interaction is changed
- Frontend API wiring or polling logic is updated
- Before finishing any frontend task that affects user behavior

## Preconditions
- Dev server is running and page is reachable
- Backend API (if required) is running
- Test account and minimal test data are available

## Mandatory Steps
1. Open target page and capture initial state screenshot.
2. Verify primary entry flow:
   - navigation route is correct
   - required inputs/buttons are visible
   - disabled/enabled state is expected
3. Verify core interaction flow:
   - fill required fields with valid values
   - submit main action
   - assert success feedback and target route/state change
4. Verify failure/guard flow:
   - submit with invalid or missing required fields
   - assert validation messages and no unexpected navigation
5. Verify async behavior:
   - loading indicators appear/disappear correctly
   - polling/retry logic stops at terminal states
6. Verify data rendering:
   - response fields render in expected structure
   - key fields (status, type, evidence, timestamps) are visible and formatted
7. Verify regression points:
   - sidebar collapse/expand and active state consistency
   - responsive behavior for narrow width
8. Capture final screenshot and summarize:
   - pass/fail items
   - reproducible issues with exact steps

## Locator and Assertion Rules
- Prefer user-facing locators: role, label, visible text.
- Avoid brittle selectors tied to deep DOM/CSS structure.
- Prefer state assertions that auto-wait over fixed sleep.
- Do not rely on arbitrary hard-coded delays unless no alternative exists.

## Output Template
- Page: `<route>`
- Scope: `<what changed>`
- Result:
  - ✅ `<passed item>`
  - ❌ `<failed item + repro steps>`
- Evidence:
  - screenshot before: `<path>`
  - screenshot after: `<path>`
  - key network/assertion notes: `<short notes>`

## Default Coverage Matrix
- Navigation & route
- Form validation
- Primary action
- Error handling
- Async loading/polling
- Structured data rendering
- Visual consistency
- Regression checks
