---
tags:
  - wiki/pattern
---

# MVVM + Repository Pattern

> **Last verified:** 2026-05-17 | **Verified by:** [source]

## Estructura por feature

Cada feature tiene 4 archivos en `ui/articles/<feature>/`:

| Archivo | Responsabilidad |
|---|---|
| `*Screen.kt` | Composable con la UI. Recibe state + callbacks. |
| `*ScreenRoute.kt` | Composable route que instancia el ViewModel via `hiltViewModel()` y mapea state a screen |
| `*ScreenState.kt` | Data classes con el estado de la UI |
| `*ViewModel.kt` | ViewModel que expone `StateFlow<UiState<T>>` |

## Flujo de datos

```
View (Composable)
    ↑ StateFlow<UiState<T>>
ViewModel
    ↑ Result<T> / Flow<PagingData<T>>
Repository
    ↑ API Response / Room DAO
ApiService / ArticleDao
```

## UiState

```kotlin
sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
```

Cada ViewModel expone `StateFlow<UiState<T>>`. La Screen observa con `collectAsStateWithLifecycle()`.

## ViewModel

- Usa `ViewModelProvider` de Hilt via `hiltViewModel()` en el Route
- Inyecta Repository via constructor `@Inject constructor`
- Usa `viewModelScope.launch` para corrutinas
- Expone estado via `StateFlow`, acciones via métodos públicos

## Repository

- Inyecta ApiService + DAOs via constructor
- Devuelve `Result<T>` para operaciones puntuales (getArticleById)
- Devuelve `Flow<PagingData<T>>` para listas paginadas (getArticles, searchArticles)
- Wrappea errores de red en `ApiException`

## Use Cases (cuándo y por qué)

Un use case encapsula lógica de negocio que no pertenece ni al ViewModel ni al Repository:

| Escenario | Sin Use Case | Con Use Case |
|---|---|---|
| Solo delegar al repo | `repo.searchArticles(q)` en el VM | Pasamanos, no suma |
| Validar input + delegar | Validación en el VM o repo | Lógica encapsulada |
| Combinar 2+ repositorios | Código duplicado en varios VMs | Un solo use case reutilizado |
| Timeout + fetch | Timeout en ViewModel | Use case con `withTimeoutOrNull` |

El `operator fun invoke()` permite llamarlo como función:

```kotlin
class GetArticleUseCase @Inject constructor(
    private val repository: ArticlesRepository
) {
    suspend operator fun invoke(id: Int, timeoutMs: Long = 30_000L): Result<Article> =
        withTimeoutOrNull(timeoutMs) { repository.getArticle(id) }
            ?: Result.failure(Exception("Timeout al cargar el artículo"))
}

// Uso
val article = getArticleUseCase(id)  // vs getArticleUseCase.invoke(id)
```

### Cuándo NO usar use cases

Para 2 pantallas con 1 fuente de datos, un use case que solo delega al repositorio sin lógica real es over-engineering. Aplicar YAGNI.

## Stale-while-revalidate via Paging 3

El patrón de caché implementado en el proyecto usa **Paging 3 + RemoteMediator + Room**, que sigue la estrategia **stale-while-revalidate**:

```
PagingSource (Room) — sirve datos cacheados inmediatamente (STALE)
    ↑
    | Room almacena artículos cargados
    |
RemoteMediator — trae páginas nuevas de la API (REVALIDATE)
    ↑
ApiService — HTTP request
```

**Cómo funciona:**

1. **Stale**: `PagingSource` lee de Room. Si hay datos cacheados, el usuario los ve al instante, sin esperar la red. `cachedIn(viewModelScope)` mantiene las páginas a través de cambios de configuración.

2. **Revalidate**: `RemoteMediator.load()` se ejecuta cuando el usuario scrollea al final de la página actual. Fetcha la siguiente página de la API, la guarda en Room, y Room notifica al `PagingSource`.

3. **REFRESH**: En `LoadType.REFRESH` (primer load o pull-to-refresh), el RemoteMediator limpia la DB y recarga desde la página 0.

```kotlin
// Suspend fun: stale-while-revalidate manual
class GetArticleUseCase @Inject constructor(
    private val repository: ArticlesRepository
) {
    suspend operator fun invoke(id: Int, timeoutMs: Long = 30_000L): Result<Article> =
        withTimeoutOrNull(timeoutMs) { repository.getArticle(id) }
            ?: Result.failure(Exception("Timeout al cargar el artículo"))
}
```

Para el fetch individual (`getArticle(id)`), hay un `withTimeoutOrNull` de 30s: si la API no responde, se devuelve error en lugar de un spinner infinito.

**Diferencias con "cache optimista" clásico:**

| Aspecto | Cache optimista (mem→DB→red) | Stale-while-revalidate (Paging 3) |
|---|---|---|
| API calls | Siempre al suscribirse | Solo cuando se scrollea al final |
| Memoria | MemoryCache explícito | `cachedIn()` de Paging 3 |
| Inmediatez | Primera emisión de memoria | Primera emisión de Room |
| Sincronización | Manual (shouldFetch) | Automática (RemoteMediator + Room) |

## Ver también

- [[architecture/clean-architecture-guide]] — Clean Architecture + use cases
- [[architecture/di-hierarchy]] — DI hierarchy
- [[architecture/data-layer]] — Data layer