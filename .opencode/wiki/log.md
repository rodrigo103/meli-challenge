# Changelog del Wiki

## 2026-05-19

- Removed `data/preferences/AppPreferences` and `di/DataStoreModule` — no se consumía (`lastOpenedArticleId` sin lectores, `isDarkMode` sin uso)
  - Updated [[architecture/app-structure]] — removed `DataStoreModule` and `preferences/` directory
  - Updated [[tools/hilt-setup]] — removed `DataStoreModule` row, updated `AppModule` bindings
  - Updated [[architecture/di-hierarchy]] — removed `DataStoreModule` row and dependency, updated `AppModule` bindings
  - Updated [[architecture/data-layer]] — removed DataStore Preferences section
  - Removed `datastore.preferences` dependency from `build.gradle.kts`

## 2026-05-18 (2)

- Refactor DualPaneScreen + introducción de `GetArticleUseCase`
  - Created `data/usecase/GetArticleUseCase` — pure domain use case: timeout + fetch article by ID
  - Created `ui/articles/detail/ArticleDetailPaneViewModel` — ViewModel para tablet detail pane (dynamic articleId)
  - Updated `DualPaneScreen` — eliminado `repository` param, usa `ArticlesListViewModel` + `ArticleDetailPaneViewModel` como default params, estado de selección local
  - Updated `ArticleDetailViewModel` — ahora usa `GetArticleUseCase` en vez de `repository.getArticle` directo
  - Updated `ArticlesListViewModel` — eliminado `selectedArticleId` / `onArticleSelected` (solo se usaba en tablet)
  - Deleted `ui/RepositoryEntryPoint` — ya no necesario
  - Updated `ui/ResponsiveApp` — eliminado `EntryPointAccessors`
  - Updated [[architecture/app-structure]] — removed `RepositoryEntryPoint`, added `usecase/` and `ArticleDetailPaneViewModel`
  - Updated [[architecture/di-hierarchy]] — removed `RepositoryEntryPoint`
  - Updated [[tools/hilt-setup]] — removed EntryPoint section
  - Eliminados todos los `@Suppress("ViewModelInjection")` del proyecto (2)

- Refactor DualPaneScreen + introducción de `GetArticleUseCase`
  - Created `data/usecase/GetArticleUseCase` — pure domain use case: timeout + fetch article by ID
  - Created `ui/articles/detail/ArticleDetailPaneViewModel` — ViewModel para tablet detail pane (dynamic articleId)
  - Updated `DualPaneScreen` — eliminado `repository` param, usa `ArticlesListViewModel` + `ArticleDetailPaneViewModel` como default params, estado de selección local
  - Updated `ArticleDetailViewModel` — ahora usa `GetArticleUseCase` en vez de `repository.getArticle` directo
  - Updated `ArticlesListViewModel` — eliminado `selectedArticleId` / `onArticleSelected` (solo se usaba en tablet)
  - Deleted `ui/RepositoryEntryPoint` — ya no necesario
  - Updated `ui/ResponsiveApp` — eliminado `EntryPointAccessors`
  - Updated [[architecture/app-structure]] — removed `RepositoryEntryPoint`, added `usecase/` and `ArticleDetailPaneViewModel`
  - Updated [[architecture/di-hierarchy]] — removed `RepositoryEntryPoint`
  - Updated [[tools/hilt-setup]] — removed EntryPoint section
  - Eliminados todos los `@Suppress("ViewModelInjection")` del proyecto

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