# Changelog del Wiki

## 2026-05-18

- Migración de `Response<T>` + `extractBody()` a `CallAdapter.Factory` automático
  - Updated [[tools/retrofit-setup]] — Apis devuelven dominio directo, `HttpErrorCallAdapterFactory` activado
  - Updated [[architecture/data-layer]] — `ResponseExt.kt` eliminado, APIs sin `Response<T>`
  - Updated [[patterns/error-handling]] — Nuevo flujo CallAdapter → ApiException → Result
  - Updated [[architecture/app-structure]] — `ResponseExt.kt` removido, `NetworkModule` usa `CallAdapterFactory`
  - Deleted `ResponseExt.kt` — reemplazado por `HttpErrorCallAdapter.kt` activo
- Updated `check-commit` skill — new step 4: wiki review antes del commit
- Updated `AGENTS.md` — reminder para revisar wiki después de cambios arquitectónicos / API / DI / error handling

## 2026-05-17 (2)

- Added [[tools/detekt-setup]] — Detekt static analysis, config, Compose rules, baseline, CI
- Updated [[processes/build-and-test]] — Added detekt commands, CI pipeline table
- Updated [[processes/pr-workflow]] — Added PR template, CODEOWNERS, CI checks section
- Updated [[index]] — Added detekt-setup link

## 2026-05-17

- Setup inicial del wiki
- Seed pages creadas para architecture, processes, patterns, tools
- Added [[patterns/attributes-actions]] — Attributes/Actions pattern + previews + analytics en Actions