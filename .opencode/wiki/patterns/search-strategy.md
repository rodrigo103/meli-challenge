---
tags:
  - wiki/pattern
---

# Search Strategy

> **Last verified:** 2026-05-19 | **Verified by:** [analysis]

## Contexto

La API de Space Flight News soporta búsqueda textual via `?search=mars`. El proyecto tiene Room como caché con Paging 3 y `RemoteMediator`.

## El problema

Al implementar búsqueda contra la API nos enfrentamos a:

1. **RemoteMediator se recrea en cada búsqueda** — al usar `flatMapLatest` sobre el query, se crea un nuevo `Pager` con un nuevo `RemoteMediator` por cada cambio.
2. **CancellationException** — `flatMapLatest` cancela el flow anterior, lanzando `CancellationException`. Si no se relanza, se traduce en un snackbar de error.
3. **Errores transitorios** — combinación de timeouts, cancelaciones y errores de red que producen experiencias inconsistentes.

## Las dos opciones

### Opción A: Búsqueda contra la API

```
Usuario escribe → debounce 300ms → Pager con RemoteMediator + searchQuery → API ?search= → Room
```

**Pros:** Resultados completos, siempre fresco.
**Contras:** HTTP request por cada búsqueda, RemoteMediator se recrea, no funciona offline, experiencia de "carga" en cada búsqueda.

### Opción B: Búsqueda local contra Room

```
Usuario escribe → Pager SIN RemoteMediator → Room PagingSource con WHERE/LIKE → instantáneo
```

**Pros:** Sin HTTP requests, sin problemas de cancelación, funciona offline, instantáneo.
**Contras:** Solo busca entre artículos ya cacheados en Room.

## Decisión: Opción B

**Búsqueda local contra Room por estabilidad y simplicidad.**

### Arquitectura resultante

```
Normal (sin búsqueda):
  Pager(remoteMediator = ArticleRemoteMediator, pagingSource = articleDao.pagingSource())
    → RemoteMediator llama API sin ?search=, guarda en Room
    → PagingSource lee de Room

Búsqueda (con query):
  Pager(remoteMediator = null, pagingSource = articleDao.searchPagingSource(query))
    → Sin RemoteMediator (no HTTP)
    → Room filtra localmente con LIKE %query%
```

### Flujo de datos

```
Usuario typea "mars"
  → _searchQuery.value = "mars"
  → flatMapLatest → repository.getArticlesPaged("mars")
  → Pager(remoteMediator = null, pagingSource = articleDao.searchPagingSource("mars"))
  → SELECT * FROM articles WHERE title LIKE '%mars%' OR summary LIKE '%mars%'
  → Flow<PagingData<Article>> → collectAsLazyPagingItems()
```

### Tradeoff aceptado

La búsqueda solo encuentra artículos que ya están en Room. Esto es aceptable porque:

1. RemoteMediator carga páginas continuamente mientras el usuario scrollea
2. Room acumula artículos vistos
3. La experiencia de búsqueda es instantánea y sin errores
4. Se prioriza estabilidad y experiencia offline sobre completitud de resultados

### Fallback híbrido futuro

```kotlin
fun search(query: String): Flow<PagingData<Article>> {
    val local = searchLocal(query)    // Room
    if (local.count() > 0) return local
    return searchRemote(query)         // API ?search=
}
```

## Ver también

- [[patterns/room-paging]] — Room + Paging 3 + RemoteMediator
- [[patterns/mvvm-repository]] — MVVM + Repository