---
tags:
  - wiki/pattern
---

# Room + Paging 3 + RemoteMediator

> **Last verified:** 2026-05-19 | **Verified by:** [source]

## Room setup

- `AppDatabase` — Database singleton via Hilt
- `ArticleEntity` — Entidad con campos: `id` (PK), `title`, `authors`, `url`, `image_url`, `news_site`, `summary`, `published_at`
- `ArticleDao` — DAO con:
  - `getAllPaging(source)` → `PagingSource<Int, ArticleEntity>` — paginación simple
  - `search(searchQuery)` → `PagingSource<Int, ArticleEntity>` — búsqueda con `LIKE`
  - `insertAll(articles: List<ArticleEntity>)` — upsert batch para RemoteMediator
  - `clearAll()` — limpieza antes de recarga

## Paging 3

Usa `PagingSource` de Room + `Flow<PagingData<T>>` expuesto por Repository.

## RemoteMediator

`ArticleRemoteMediator` sincroniza API → Room:

1. Detecta si necesita cargar más datos (scroll al final)
2. Llama a `ApiService.getArticles(limit, offset)`
3. Mapea `Article` → `ArticleEntity`
4. Inserta en Room via `ArticleDao.insertAll()`
5. Room notifica al `PagingSource` que hay nuevos datos
6. `Flow<PagingData<T>>` emite nuevo estado a la UI

### Load type

- `LoadType.REFRESH` — Limpia DB y recarga desde página 0
- `LoadType.PREPEND` — No implementado (API no soporta backward pagination)
- `LoadType.APPEND` — Carga siguiente página usando `offset + limit`

## Flujo completo

```
Screen ← collectAsStateWithLifecycle() ← Flow<PagingData<Article>>
    ↑
ArticlesRepository.getArticles()
    ↑
ArticleRemoteMediator ← → ApiService.getArticles()
    ↑                        ↓
ArticleDao.getAllPaging() ← ArticleDao.insertAll()
    ↑
AppDatabase (Room)
```

## Performance: índices y chunked inserts

Para manejar grandes volúmenes de datos eficientemente:

```kotlin
@Entity(
    tableName = "articles",
    indices = [Index(value = ["title"]), Index(value = ["published_at"])]
)
data class ArticleEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val summary: String,
    // ...
)
```

- **Índice en `title`** acelera las consultas `LIKE` de búsqueda
- **Índice en `published_at`** acelera el ordenamiento por fecha
- `COLLATE NOCASE` para orden case-insensitive

### Chunked batch insertion

Evita memory pressure al insertar muchos registros:

```kotlin
val chunkSize = 200
entities.chunked(chunkSize).forEach { chunk ->
    dao.insertAll(chunk)
}
```

### Cache check para carga instantánea

```kotlin
val count = dao.getArticleCount()
if (count > 0) return true  // Ya cacheados, salida inmediata
```

## Reactive filtering con combine + flatMapLatest

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
val articles: Flow<PagingData<Article>> = combine(searchText, searchQuery) { text, query ->
    Pair(text, query)
}.flatMapLatest { (text, query) ->
    if (text.isBlank()) repository.getArticlesPaged(null)
    else repository.getArticlesPaged(text)
}.cachedIn(viewModelScope)
```

- `combine` mergea dos StateFlow
- `flatMapLatest` cambia a un nuevo PagingSource cuando cambia el filtro, **cancelando la query anterior** sin race conditions
- `cachedIn(viewModelScope)` mantiene páginas cacheadas a través de cambios de configuración (rotación, dark mode)

## Ver también

- [[patterns/search-strategy]] — Búsqueda local vs API
- [[patterns/mvvm-repository]] — MVVM + Repository