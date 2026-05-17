# Decisión de Arquitectura: Búsqueda en Space Flight News

## Contexto

La app consume la API pública de Space Flight News (`https://api.spaceflightnewsapi.net/v4/articles/`), que soporta:

- **Paginación offset/limit** (`?limit=20&offset=0`)
- **Búsqueda textual** (`?search=mars`)

Tenemos Room como caché parcial (solo almacena lo que el usuario ya vio) con Paging 3 y `RemoteMediator`.

## El Problema

Al implementar búsqueda contra la API (Opción A), nos enfrentamos a:

1. **RemoteMediator se recrea en cada búsqueda** — al usar `flatMapLatest` sobre el query, se crea un nuevo `Pager` con un nuevo `RemoteMediator` por cada cambio.
2. **CancellationException** — el `flatMapLatest` cancela el flow anterior, lo que lanza `CancellationException` en el `RemoteMediator`. Si no se relanza, se traduce en un snackbar de error para el usuario.
3. **Errores transitorios** — combinación de timeouts, cancelaciones y errores de red que producen experiencias inconsistentes.

## Las Dos Opciones

### Opción A: Búsqueda contra la API

```
Usuario escribe → debounce 300ms → Pager con RemoteMediator + searchQuery → API ?search=term → Room (filtrado)
```

**Pros:**
- Resultados completos (busca en toda la API, no solo lo cacheado)
- Siempre fresco

**Contras:**
- Un HTTP request por cada búsqueda (incluso con debounce)
- RemoteMediator se recrea → bugs de cancelación
- No funciona offline
- Experiencia de "carga" en cada búsqueda

### Opción B: Búsqueda local contra Room

```
Usuario escribe → Pager SIN RemoteMediator → Room PagingSource con WHERE/LIKE → instantáneo
```

**Pros:**
- Sin HTTP requests al buscar
- Sin problemas de cancelación (no hay RemoteMediator)
- Funciona offline
- Live search sin debounce
- Instantáneo (Room en memoria)
- Mismo patrón que Ualá y CityApp

**Contras:**
- Solo busca entre artículos ya cargados en Room
- Si el usuario busca un tema que no ha scrolleado, no aparece

## ¿Qué hacen las apps de referencia?

| App | Enfoque |
|-----|---------|
| **CityApp (Ualá)** | Búsqueda local + PagingSource de Room. RemoteMediator carga sin filtro. |
| **ualaChallenge-main (Ualá)** | Búsqueda local. Sin RemoteMediator (Room + API service separados). |
| **SpaceFlight News API docs** | Sugieren `?search=` para consultas completas. |

Ambas apps de Ualá usan búsqueda local. La lógica es que en una app de desafío técnico el usuario no va a scrollear 500 artículos, pero los que ve son suficientes para que la búsqueda sea útil.

## Decisión Final: Opción B

**Elegimos búsqueda local contra Room por estabilidad y simplicidad.**

### Arquitectura resultante

```
Normal (sin búsqueda):
  Pager(remoteMediator = ArticleRemoteMediator, pagingSource = articleDao.pagingSource())
    → RemoteMediator llama a API sin ?search=, guarda en Room
    → PagingSource lee de Room

Búsqueda (con query):
  Pager(remoteMediator = null, pagingSource = articleDao.searchPagingSource(query))
    → Sin RemoteMediator (no HTTP)
    → Room filtra localmente con LIKE %query%
```

### Cambios en el código

| Archivo | Cambio |
|---------|--------|
| `ArticleRemoteMediator` | Eliminado `searchQuery`. Siempre carga sin filtro. |
| `ArticlesRepository.getArticlesPaged()` | RemoteMediator solo si `searchQuery` es null/blank. Search usa `null` mediator. |
| `ArticlesListViewModel` | `flatMapLatest` directo sobre `_searchQuery`. Eliminados `_searchTrigger`/`searchDisplayQuery`/`onSearchQueryChanged`/`search()`. Simplificado a `onSearchTextChange()`. |
| `ArticlesListScreen` | `onQueryChange = viewModel::onSearchTextChange` (live). `onSearch = {}`. |

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

La búsqueda solo encuentra artículos que ya están en Room. Para un challenge esto es aceptable porque:

1. El RemoteMediator carga páginas continuamente mientras el usuario scrollea
2. Room acumula artículos vistos
3. La experiencia de búsqueda es instantánea y sin errores
4. En la defensa técnica explicamos: "optamos por búsqueda local contra Room, mismo patrón que Ualá, priorizando estabilidad y experiencia offline sobre completitud de resultados"

### Si en el futuro se necesitara búsqueda completa

Se podría implementar un fallback híbrido:

```kotlin
fun search(query: String): Flow<PagingData<Article>> {
    val local = searchLocal(query)    // Room
    if (local.count() > 0) return local
    return searchRemote(query)         // API ?search=
}
```

O usar `?search=` como query parameter del RemoteMediator (Opción A original). La arquitectura actual lo permite: solo cambiaría el `remoteMediator` constructor.
