---
tags:
  - wiki/tool
---

# Hilt Setup

> **Last verified:** 2026-05-19 | **Verified by:** [source] — removed `DataStoreModule`, updated `AppModule` bindings

## Entries

- `MyApplication` → `@HiltAndroidApp`
- `MainActivity` → `@AndroidEntryPoint`

## Módulos de DI

| Módulo | Bindings clave |
|---|---|
| `AppModule` | `isDebug` named binding |
| `NetworkModule` | `OkHttpClient`, `Retrofit`, `ApiService` |
| `DatabaseModule` | `AppDatabase`, `ArticleDao` |
| `RepositoryModule` | `ArticlesRepository` |
| `DispatcherModule` | `CoroutineDispatchers` |
| `AnalyticsModule` | `AnalyticsHelper` → `TimberAnalyticsHelper` |

## Plugins Gradle

```
id("com.google.dagger.hilt.android")
id("com.google.devtools.ksp")
```

## Dependencias

```
implementation(libs.hilt.android)
ksp(libs.hilt.compiler)
implementation(libs.hilt.navigation.compose)
```