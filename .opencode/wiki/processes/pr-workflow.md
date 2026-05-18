---
tags:
  - wiki/process
---

---
tags:
  - wiki/process
---

# PR Workflow

> **Last verified:** 2026-05-17 | **Verified by:** [analysis]

## Branch naming

`<tipo>/<descripcion-corta>` — ej: `feat/search-articles`, `fix/crash-on-empty-list`

## Commit format

```
<tipo>: <descripción>

Opcional: cuerpo explicativo
```

Tipos: `feat`, `fix`, `refactor`, `chore`, `docs`, `test`, `perf`.

## PR template

Archivo `.github/PULL_REQUEST_TEMPLATE.md` con checklist estandarizado [source]:

```markdown
## 📋 Descripción
## 🎯 Cambios realizados (feature/bug/refactor/deps/CI)
## ✅ Checklist
[ ] Tests pasan (`./gradlew test`)
[ ] Detekt no reporta issues (`./gradlew detekt`)
[ ] Build debug exitoso (`./gradlew assembleDebug`)
[ ] Convenciones del proyecto
```

## CODEOWNERS

`.github/CODEOWNERS` — `* @rodrigo103` revisa todos los archivos [source].

## CI Checks

El workflow `.github/workflows/ci.yml` corre automáticamente en cada PR a `main` [source]:

1. **Detekt** — Static analysis code quality
2. **Unit Tests** — JUnit + MockK + Turbine
3. **Assemble Debug** — Build de debug

Todos los checks deben pasar para mergear.

## Review

- PR mínimo de 1 approve
- Review checklist:
  - Compila en CI (detekt + tests + build)
  - Tests pasan
  - Detekt sin nuevos issues
  - Sin magic numbers
  - Null safety chequeada
  - Composición sin recomposiciones innecesarias
  - Manejo de errores consistente

## Merge

- Squash merge
- Mensaje de commit consolidado = título del PR
- Eliminar branch post-merge