---
tags:
  - wiki/architecture
---

# DI Hierarchy

> **Last verified:** 2026-05-18 | **Verified by:** [source]

Jerarquía de módulos de Hilt DI. Todos los módulos están en `com.example.myandroidapp.di`.

RepositoryEntryPoint eliminado — ya no se pasa repository a composables.

## Módulos

| Módulo | Scope | Bindings |
|---|---|---|
| `AppModule` | Singleton | `Context`, `DataStore<Preferences>` |
| `NetworkModule` | Singleton | `OkHttpClient`, `Retrofit`, `ApiService` |
| `DatabaseModule` | Singleton | `AppDatabase`, `ArticleDao` |
| `RepositoryModule` | Singleton | `ArticlesRepository` |
| `DataStoreModule` | Singleton | `AppPreferences` |
| `DispatcherModule` | Singleton | `CoroutineDispatchers` provider |
| `AnalyticsModule` | Singleton | `AnalyticsHelper` → `TimberAnalyticsHelper` |

## Dependencies entre módulos

```
AppModule (Context, DataStore)
├── DataStoreModule (AppPreferences)
├── NetworkModule (OkHttp, Retrofit, ApiService)
├── DatabaseModule (Room, DAOs)
│   └── DispatcherModule (Dispatchers)
└── RepositoryModule (ArticlesRepository)
    ├── NetworkModule
    ├── DatabaseModule
    └── DispatcherModule
```

## Entry points

- `MainActivity` — `@AndroidEntryPoint`
- `MyApplication` — `@HiltAndroidApp`

## Ver también

- [[tools/hilt-setup]] — Detalles de los módulos Hilt