---
name: update-pr
description: "Updates the current branch's PR with a structured description, Jira link, and detailed changelog based on actual code changes."
argument-hint: '[Jira ticket URL]'
user-invocable: true
allowed-tools: Bash, Read, Glob, Grep
---

# Update PR

Update the pull request associated with the current branch with a well-structured description in English.

## Steps

1. Get the current branch name and find its associated PR using `gh pr list --head <branch>`.
2. Extract the Jira ID from the branch name (e.g. `feat/AAC-432-description` → `AAC-432`) or from the provided argument.
3. Analyze ALL commits on the branch vs main using `git log main..HEAD` and `git diff main...HEAD --stat`.
4. Update the PR title using `gh pr edit <number> --title` with the format: `type: [JIRA-ID] Short description`. Infer the type from the branch prefix (feat, fix, chore, etc.).
5. Draft the PR body following the template below, filling in each section based on the actual changes.
6. Update the PR using `gh pr edit <number> --body`.
7. Update the CHANGELOG entry for the current version with the PR number link (e.g. `[#PR404](https://github.com/Bancar/uala-android-help/pull/404)`).

## Template

```markdown
### **User description**
## Description

{{A concise paragraph (2-4 sentences) explaining the purpose and scope of this PR. Focus on the "why" and high-level "what".}}

[JIRA-ID]({{jira_url}})

### Changes
{{Bulleted list of all meaningful changes. Be specific — mention component names, screens, models, use cases, etc. Group related items.}}

### Checklist
- [ ] CHANGELOG updated.
- [ ] Module version updated (if needed).
- [ ] Tests added/updated.

### Evidence
[Screenshots/GIFs]
```

## Rules

- Always write in English.
- Extract the Jira ID from the provided URL and format it as a clickable link.
- Analyze the actual code diff to write accurate change descriptions — do not guess or use generic text.
- Keep the description paragraph concise but informative.
- List changes from most important to least important.
- Mention removed/deleted code explicitly (e.g. "Removed legacy X", "Deleted unused Y").
- If no Jira URL is provided as argument, leave the Jira section as `[JIRA-ID](url)` for the user to fill in.
- Return the PR URL when done.
