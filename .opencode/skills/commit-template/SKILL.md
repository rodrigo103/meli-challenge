---
name: commit-template
description: "Primer commit del branch"
user-invocable: true
allowed-tools: Bash, Read
---

# Commit Template Reference

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
