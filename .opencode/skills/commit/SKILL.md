---
name: commit
description: "Before generating the commit command, ask me:"
user-invocable: true
allowed-tools: Bash, Read, Glob
---

# Commit Generator

Before generating the commit command, ask me:
"Which branch are you committing to?"

Wait for my answer, then:

1. Extract the Jira ticket from the branch name
   (e.g. feature/AUTH-123-login → AUTH-123)

2. Ask me: "Is this the first commit of this branch? (y/n)"

3. Based on the answer use the commit template:

## Primer commit del branch
```
feat[TICKET-JIRA]: Short title

Short description of the changes

- Change 1
- Change 2
- Change 3
```

## Commits siguientes
```
feat: Short title

Short description of the changes

- Change 1
- Change 2
- Change 3
```

## Reglas
- Tipos: feat, fix, refactor, chore, docs, test, perf
- TICKET-JIRA: se extrae del nombre del branch
  - feature/AUTH-123-login → AUTH-123
  - fix/PLAT-456-navbar    → PLAT-456
- Solo el PRIMER commit del branch lleva el ticket
- Los demás commits del mismo branch no llevan ticket
- Descripción corta en inglés, imperativo
- Lista de cambios en inglés

4. Analyze the current project changes and generate the command:
   `but commit -m "..." <branch>`

Only show the final command ready to copy and paste, nothing else.
