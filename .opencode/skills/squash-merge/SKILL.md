---
name: squash-merge
description: Squash and merge a GitHub PR with a consolidated commit message rewritten from the PR's commits (or description for version-bump PRs), then trigger build-monitor for the affected module. Usage: /squash-merge [PR_NUMBER or URL]
---

# Squash & Merge

Consolidates a PR into a clean, well-organized squash commit and (for library modules) kicks off build monitoring afterward.

## Input

- `/squash-merge` → resolve PR from current branch (`gh pr view --json number,url,headRepository`)
- `/squash-merge 412` → PR #412 in the current repo
- `/squash-merge https://github.com/Bancar/uala-android-help/pull/412` → explicit URL

Parse `owner`, `repo`, `pull_number` from whichever form is given.

## Workflow

### Step 1 — Fetch PR data

- `mcp__github__get_pull_request` → title, body, base, mergeable state, checks
- `mcp__github__list_commits` → full commit list of the PR

If the PR is not mergeable, has failing required checks, or has unresolved conflicts, **stop and warn Tony**. Do not merge without explicit confirmation.

### Step 2 — Pick the rewriting strategy

Detect repo type:

| Repo | Strategy |
|------|----------|
| `uala-android-app` (main app) | **Description-first**: body built mainly from PR description (typically version bumps). If diff touches files outside `versions/` or `gradle/libs.versions.toml`, also mine commits for context. |
| Any other repo (library modules: `uala-android-help`, `uala-android-home`, `uala-android-profile`, etc.) | **Commits-first**: consolidate and reorder the PR's commits; drop intermediate churn (reverts that no longer apply, "fix lint", "address review", "rebase", etc.). |

### Step 3 — Build the commit message (English)

The rewritten message **must be in English**, regardless of the conversation language. Keep Tony's conventional-commit style.

**Title**: `<type>: [JIRA-ID] <short description> (#<PR number>)`
- Reuse the PR title's prefix and Jira ID.
- Keep under ~100 chars when possible.

**Body**: group bullets by category when applicable. Typical sections:
- `Security:` (Dependabot, CVE, Code Scanning)
- `SonarQube cleanups in <module>:` (one bullet per rule: `S1192`, `S6526`, etc.)
- `Resources:` (strings, layouts, manifest)
- `Version bumps:` (module versions, catalogue versions)
- `Tests:` (new/updated tests)
- `Refactor:` / `Docs:` when they apply

Rules:
- One bullet per meaningful change. No fluff.
- Drop commits that cancel each other (e.g., "add X" + "revert X" → omit both).
- Drop review-churn commits ("fix review", "lint", "rebase main").
- Preserve sub-Jira references (e.g., `[AAC-412]` inline on catalogue bumps).
- End with: `Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>`

### Step 4 — Confirm with Tony

Show the proposed title + body. Wait for confirmation before merging, unless Tony explicitly says "hazlo directo" or "dale sin preguntar".

### Step 5 — Merge

Always use squash:
```
mcp__github__merge_pull_request(
  owner, repo, pull_number,
  merge_method: "squash",
  commit_title: <title>,
  commit_message: <body>
)
```

**Never** use `merge` or `rebase` methods.
**Never** use `--no-verify` or skip hooks.
**Never** force-merge a PR with failing required checks.

### Step 6 — Post-merge actions

**If repo is a library module** (anything other than `uala-android-app`):
1. Extract module name from repo: `uala-android-help` → `help`, `uala-android-home` → `home`, etc.
2. Invoke the `build-monitor` skill with that name.
3. Report back the expected new version (read from the bumped `version.properties` or infer from the commit body).

**If repo is `uala-android-app`**:
- Do not run build-monitor (app doesn't publish artifacts to JFrog).
- Just confirm the merge and mention next steps if relevant (release branch, PlayStore, etc.).

### Step 7 — Notify

Short message in Spanish to Tony:
- Merge SHA
- New version being published (if library)
- Status of build-monitor (running / scheduled wake-up)

## Known module mappings

| Repo | Short name | JFrog module |
|------|------------|--------------|
| uala-android-help | help | help (group: uala-android-cx) — also publishes help-experience |
| uala-android-home | home | home |
| uala-android-profile | profile | profile |
| uala-android-cash-in | cash-in | cash-in + pse |
| uala-android-loans | loans | loans |

If the repo is unknown, ask Tony what the short name for build-monitor should be.

## Style

- Talk to Tony in Spanish.
- Write the commit message in English.
- Table format when summarizing multi-step status.
- Be concise; skip narration of obvious steps.