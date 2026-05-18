# Error Handling

> **Last verified:** 2026-05-18 | **Verified by:** [source] — migrado de `extractBody()` a `CallAdapter.Factory`

## Capas de error

### Network layer

- `HttpErrorCallAdapterFactory` — `CallAdapter.Factory` registrado en el `Retrofit.Builder` de `NetworkModule`. Intercepta `Call.enqueue()` y convierte respuestas no-2xx en excepciones tipadas **antes** de que lleguen al caller. [source]
- `ApiException` — sealed class con subclases por HTTP status [source]:
  - `BadRequest(400)`, `Unauthorized(401)`, `NotFound(404)`, `Conflict(409)`
  - `ServerError(code, 500..599)`, `NetworkError`
- **Ya no se usa `Response<T>` en las APIs** ni `extractBody()`. Las APIs devuelven dominio directo (`Article`, `ArticleResponse`). El `CallAdapter` se encarga de mapear errores HTTP a excepciones automáticamente.

### Repository layer

- Repository wrappea operaciones con `Result.runCatching { ... }`
- Errores de red → `Result.failure(ApiException)` (lanzada por el CallAdapter)
- Errores de base de datos → `Result.failure(Exception)` (Room lanza sus propias excepciones)
- Logging via `Timber.e()` en `.onFailure {}`

### ViewModel layer

- ViewModel transforma `Result<T>` en `UiState<T>`:
- `Result.success` → `UiState.Success`
- `Result.failure` → `UiState.Error(message)`

### UI layer

- `Success` → renderiza datos
- `Loading` → muestra indicador de carga (Lottie)
- `Error` → muestra Snackbar con mensaje + botón de retry

## Patrón general

```kotlin
// En Repository
override suspend fun getArticle(id: Int): Result<Article> =
    runCatching {
        apiService.getArticle(id)  // CallAdapter tira ApiException en error
    }.onFailure {
        Timber.e(it, "Error fetching article with id: %d", id)
    }

// En ViewModel
fun loadArticle(id: Int) {
    viewModelScope.launch {
        _uiState.value = UiState.Loading
        repository.getArticle(id)
            .onSuccess { _uiState.value = UiState.Success(it) }
            .onFailure { _uiState.value = UiState.Error(it.message ?: "Error desconocido") }
    }
}
```

## Errores no controlados

- `MyApplication` tiene `Thread.setDefaultUncaughtExceptionHandler`
- LeakCanary en debug captura memory leaks
