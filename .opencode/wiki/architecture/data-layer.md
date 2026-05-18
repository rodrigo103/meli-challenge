# Data Layer

> **Last verified:** 2026-05-18 | **Verified by:** [source] — `ResponseExt.kt` eliminado, `extractBody()` reemplazado por `CallAdapter.Factory`

Data layer del proyecto. Sigue patrón Repository con dos fuentes: API remota (Retrofit) y base local (Room).

## Network layer

```
ApiService (Retrofit)
    ├── GET /articles/?limit=&offset=&search=  → ArticleResponse
    └── GET /articles/{id}/                     → Article
```

- Base URL: `https://api.spaceflightnewsapi.net/v4`
- `OkHttpClient` con logging interceptor en debug
- `kotlinx.serialization` converter (no Gson)
- `HttpErrorCallAdapterFactory` — `CallAdapter.Factory` registrado en `NetworkModule`. Intercepta `enqueue()` y convierte respuestas no-2xx en `ApiException` (BadRequest, Unauthorized, NotFound, Conflict, ServerError) o `IOException`. [source]
- APIs devuelven dominio directo (`ArticleResponse`, `Article`), no `Response<T>`. El error handling es automático via `CallAdapter`.
- `ResponseExt.kt` fue eliminado — ya no se necesita `extractBody()`.

## Room layer

- `AppDatabase` con migrations (Room 2.7+)
- `ArticleEntity` — entidad Room para artículos (id, title, authors, url, image_url, news_site, summary, published_at)
- `ArticleDao` — DAO con consultas Paging:
  - `getAllPaging()` → `PagingSource<Int, ArticleEntity>`
  - `search(searchQuery)` → `PagingSource<Int, ArticleEntity>` con `LIKE` query

## Paging 3 + RemoteMediator

- `ArticleRemoteMediator` — sincroniza API → Room con paginación:
  1. Carga página desde API
  2. Inserta en Room
  3. Room via PagingSource alimenta la UI
  4. En scroll infinito, RemoteMediator pide siguiente página

## Repository pattern

`ArticlesRepository` orquesta las fuentes [source]:
- `getArticles(limit, offset)` → `Result<List<Article>>` (via `apiService.getArticles(...).results`)
- `searchArticles(query, limit)` → `Result<List<Article>>` (via `apiService.getArticles(...).results`)
- `getArticle(id)` → `Result<Article>` (via `apiService.getArticle(id)`)
- `getArticlesPaged(searchQuery?)` → `Flow<PagingData<Article>>` (via RemoteMediator + Room)
- Errores capturados con `runCatching { ... }.onFailure { Timber.e(...) }`

## DataStore Preferences

- `AppPreferences` — wrapper sobre `DataStore<Preferences>` [source]
- Guarda preferencias de usuario (tema, etc.)
