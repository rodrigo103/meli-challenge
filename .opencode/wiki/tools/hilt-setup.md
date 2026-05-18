---
tags:
  - wiki/tool
---

# Hilt Setup

> **Last verified:** 2026-05-18 | **Verified by:** [source]

## Entries

- `MyApplication` → `@HiltAndroidApp`
- `MainActivity` → `@AndroidEntryPoint`

## Módulos de DI

| Módulo | Bindings clave |
|---|---|
| `AppModule` | `Context`, `DataStore<Preferences>` |
| `NetworkModule` | `OkHttpClient`, `Retrofit`, `ApiService` |
| `DatabaseModule` | `AppDatabase`, `ArticleDao` |
| `RepositoryModule` | `ArticlesRepository` |
| `DataStoreModule` | `AppPreferences` |
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