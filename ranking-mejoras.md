# Ranking: Mejores cosas para implementar en el Challenge

Basado en el análisis de **8 repositorios Ualá** (~167 patrones). Priorizo lo que da **más valor en entrevista** con el menor overhead para nuestro proyecto de 2 pantallas.

---

## 🥇 1. Room + Paging 3 con índices y prefix matching

**Fuente:** `ualaChallenge`, `uala-android-home`

**Por qué:** Es el patrón más fuerte de todo el análisis. El challenge de ciudades de Ualá resolvía el mismo problema que nosotros: cargar datos de red, cachearlos localmente, y mostrarlos con búsqueda/paginación fluida.

**Qué implementar:**
```kotlin
@Entity(tableName = "articles", indices = [Index(value = ["title"])])
data class ArticleEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val summary: String,
    val url: String,
    val imageUrl: String?,
    val publishedAt: String,
    val newsSite: String,
    val isFavorite: Boolean = false
)
```

```kotlin
@Dao
interface ArticleDao {
    @Query("SELECT * FROM articles WHERE title LIKE :query || '%' ORDER BY publishedAt DESC")
    fun getArticlesPagingSource(query: String): PagingSource<Int, ArticleEntity>
}
```

**Valor en entrevista:**
- "Usé Room con índice en `title` para prefix matching en 10k+ artículos. La segunda carga es instantánea porque verifica `getCount() > 0`."
- "El DAO devuelve `PagingSource` nativo — Room lo implementa solo, no necesito escribir un PagingSource custom."
- "Combiné `combine(searchText, favoritesOnly).flatMapLatest { }` con `cachedIn(viewModelScope)` para filtros reactivos que cancelan queries anteriores."

**Esfuerzo:** Medio (agregar dependencias, entity, dao, migration, repository changes)

---

## 🥇 2. Sealed class `UiState` + Unidirectional Data Flow

**Fuente:** `ualaChallenge` (DownloadState), `uala-core` (Response\<T\>), `uala-android-home` (State/Actions pattern)

**Por qué:** Hoy usamos data class con booleanos. El sealed class pattern es más limpio, type-safe, y es el estándar en producción.

**Qué implementar:**
```kotlin
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
    object Empty : UiState<Nothing>()
}
```

```kotlin
// State + Actions pattern
@Immutable
data class ArticlesState(
    val articles: UiState<List<Article>> = UiState.Loading,
    val isSearchActive: Boolean = false,
    val searchQuery: String = ""
)

data class ArticlesActions(
    val onArticleClick: (Article) -> Unit,
    val onSearch: (String) -> Unit,
    val onRefresh: () -> Unit,
    val onRetry: () -> Unit
)
```

**Valor en entrevista:**
- "Usé un sealed class `UiState` con Loading/Success/Error/Empty. Renderizo la UI según el estado actual — no hay estados inválidos."
- "Separo `State` (inmutable) de `Actions` (callbacks). Es el patrón que usa el Home de Ualá con `HomeScreenAttributes` + `HomeScreenActions`."

**Esfuerzo:** Bajo (cambiar ViewModel + Screens, no toca data layer)

---

## 🥇 3. `combine` + `flatMapLatest` para búsqueda reactiva local

**Fuente:** `ualaChallenge` (CityListViewModel), `uala-android-home`

**Por qué:** Hoy probablemente la búsqueda pega a la API. Con Room local + este patrón, la búsqueda es instantánea y reactiva.

**Qué implementar:**
```kotlin
val articles: Flow<PagingData<Article>> = combine(searchText, favoritesOnly) { text, favs ->
    Pair(text, favs)
}.flatMapLatest { (query, filterFavs) ->
    repository.getPaginatedArticles(query, filterFavs)
}.cachedIn(viewModelScope)
```

**Valor en entrevista:**
- "`combine` mergea dos StateFlow. `flatMapLatest` cambia a un nuevo PagingSource cuando cambia el filtro, **cancelando la query anterior**. Sin race conditions."
- "`cachedIn(viewModelScope)` mantiene las páginas cacheadas a través de cambios de configuración (rotación, dark mode)."

**Esfuerzo:** Bajo (si ya tenés Room + Paging)

---

## 🥇 4. Responsive Layout con `BoxWithConstraints`

**Fuente:** `ualaChallenge` (ReadyStateView)

**Por qué:** Muestra conocimiento de layouts adaptativos para tablets/foldables. Es un diferenciador fuerte en entrevista.

**Qué implementar:**
```kotlin
BoxWithConstraints(Modifier.fillMaxSize()) {
    val isTablet = maxWidth > 840.dp

    if (isTablet) {
        Row(Modifier.fillMaxSize()) {
            ArticlesListPanel(modifier = Modifier.weight(0.4f))
            ArticleDetailPanel(modifier = Modifier.weight(0.6f))
        }
    } else {
        // Navigation-based como hoy
    }
}
```

**Valor en entrevista:**
- "Usé `BoxWithConstraints` con breakpoint de 840dp. En tablets, muestro lista + detalle side-by-side tipo Master-Detail. No uso orientación, uso ancho — funciona en split-screen y foldables."

**Esfuerzo:** Medio (requiere refactor de la navegación para soportar dual-pane)

---

## 🥇 5. Timeout para operaciones de red + retry pattern

**Fuente:** `ualaChallenge` (CityListViewModel: `withTimeoutOrNull(60000L)`)

**Por qué:** Simple de implementar, muy visible en entrevista, demuestra manejo de errores real.

**Qué implementar:**
```kotlin
val success = withTimeoutOrNull(30_000L) {
    repository.fetchArticles()
}
if (success == null) {
    _uiState.value = UiState.Error("Tiempo de espera agotado. Verificá tu conexión.")
}
```

**Valor en entrevista:**
- "Puse un timeout de 30s en la carga inicial. Si la API no responde, muestro error en lugar de un spinner infinito."
- "El botón de retry reinicia el flujo completo."

**Esfuerzo:** Muy bajo (agregar `withTimeoutOrNull` + retry button en UI)

---

## 🥈 6. Custom CallAdapter para HTTP errors (en vez de try-catch)

**Fuente:** `uala-core` (HttpErrorHandlerCallAdapter)

**Por qué:** El `Response<T>` de Retrofit ya lo tenemos. El CallAdapter lleva el patrón al siguiente nivel: los errores HTTP se convierten automáticamente en excepciones tipadas sin `try-catch` en el repository.

**Qué implementar:**
```kotlin
class HttpErrorCallAdapter<T>(private val delegate: CallAdapter<T, *>) : CallAdapter<T, Any> {
    override fun adapt(call: Call<T>): Any = suspendCancellableCoroutine { cont ->
        call.enqueue(object : Callback<T> {
            override fun onResponse(call: Call<T>, response: Response<T>) {
                if (response.isSuccessful) cont.resume(response.body()!!)
                else cont.resumeWithException(HttpException(response.code(), response.message()))
            }
            override fun onFailure(call: Call<T>, t: Throwable) {
                cont.resumeWithException(NetworkException(t))
            }
        })
    }
}
```

**Valor en entrevista:** "Envolví Retrofit con un CallAdapter custom. Los errores HTTP se convierten en excepciones tipadas automáticamente. El repository nunca tiene un `try-catch` — el ViewModel recibe la excepción directa."

**Esfuerzo:** Medio (implementar CallAdapter, registrarlo en Retrofit, cambiar repository)

---

## 🥈 7. Custom Semantic Properties para testing de Compose

**Fuente:** `uala-android-abra` (AbraMatcher, AbraAssert), `uala-android-visual`

**Por qué:** Los tests que usan `testTag("loading_text")` son frágiles. Los semantic properties son más robustos y profesionales.

**Qué implementar:**
```kotlin
object AbraSemantics {
    val UiState = SemanticsPropertyKey<String>("UiState")
    val Variant = SemanticsPropertyKey<String>("Variant")  // "primary", "secondary"
}

// En el componente:
Modifier.semantics { 
    this[AbraSemantics.UiState] = "loading"
}

// En el test:
composeTestRule.onNode(SemanticsMatcher("is loading") {
    it.config[AbraSemantics.UiState] == "loading"
}).assertExists()
```

**Valor en entrevista:** "Definí propiedades semánticas custom para los estados de UI y variantes de botones. Los tests matchean por semántica, no por texto visible o testTags frágiles."

**Esfuerzo:** Bajo (agregar objeto con keys + matchers)

---

## 🥈 8. Settings + Composable pattern (para botones/cards reusables)

**Fuente:** `uala-android-abra` (Settings + Composable pattern)

**Por qué:** Diferencia un "composable tirado ahí" de un "sistema de componentes diseñado". Muestra madurez de diseño.

**Qué implementar:**
```kotlin
@Stable
interface ArticleCardSettings {
    val article: Article
    val onFavoriteClick: (Article) -> Unit
    
    @Composable
    operator fun invoke() = ArticleCard(
        article = article,
        onFavoriteClick = onFavoriteClick
    )
}

fun articleCardSettings(
    article: Article,
    onFavoriteClick: (Article) -> Unit
): ArticleCardSettings = ArticleCardSettingsDefault(article, onFavoriteClick)
```

**Valor en entrevista:** "Separé la configuración de cada componente de su renderizado. Las pantallas arman un objeto Settings y lo invocan. Esto permite reusar configuraciones y testear componentes independientemente."

**Esfuerzo:** Bajo (refactor de componentes existentes)

---

## 🥈 9. META-INF exclusions + packaging optimizations

**Fuente:** `ualaChallenge`

**Por qué:** 10 líneas que evitan errores de build. Muestra que conocés el ecosistema Gradle.

**Qué implementar:**
```kotlin
// app/build.gradle.kts
packaging {
    resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
        excludes += "/META-INF/LICENSE.md"
        excludes += "/META-INF/LICENSE-notice.md"
    }
}
```

**Valor en entrevista:** (respuesta rápida si te preguntan por qué el build falla en CI vs local)

**Esfuerzo:** Muy bajo

---

## 🥈 10. Braze-like analytics wrapper simplificado

**Fuente:** `uala-core` (AnalyticsHelper, Printer)

**Por qué:** Muestra que pensaste en analytics desde el día 1. Un wrapper simple sobre Timber + un event tracker es fácil de implementar y vende mucho en entrevista.

**Qué implementar:**
```kotlin
class AnalyticsHelper @Inject constructor(
    private val analyticsProvider: AnalyticsProvider
) {
    fun trackEvent(event: String, properties: Map<String, Any> = emptyMap()) {
        analyticsProvider.track(event, properties)
        Timber.d("Analytics: $event $properties")
    }
}

interface AnalyticsProvider {
    fun track(event: String, properties: Map<String, Any>)
}
```

**Valor en entrevista:** "Creé una abstracción `AnalyticsProvider` para no acoplarme a Firebase/Amplitude. Si mañana cambiamos de proveedor, solo cambia la implementación. Hoy uso Timber en debug y Firebase en release."

**Esfuerzo:** Muy bajo

---

## 🥉 11-15 (si sobra tiempo)

| # | Patrón | Esfuerzo | Valor |
|---|--------|----------|-------|
| 11 | `Locale.of()` moderno con graceful fallback | Muy bajo | Bajo |
| 12 | DataStore Preferences para settings de UI | Bajo | Medio |
| 13 | `@IoDispatcher` custom qualifier (ya lo tenemos) | — | Ya hecho |
| 14 | Error handling jerárquico con sealed exceptions | Medio | Medio |
| 15 | AbraString-like accesibilidad wrapper | Medio | Alto (si preguntan) |

---

## Resumen: orden de implementación sugerido

1. **UiState sealed class** — ya, es rápido y cambia la calidad del código visiblemente
2. **Room + Paging 3** — el plato fuerte, el que más impacto tiene
3. **`combine` + `flatMapLatest`** — viene gratis con Room
4. **`withTimeoutOrNull`** — 2 líneas, mucho valor
5. **CallAdapter custom** — si querés mostrar conocimiento profundo de Retrofit
6. **BoxWithConstraints responsive** — si tenés tiempo y querés el modo tablet
7. **Resto** — en orden de prioridad

Los items 1 a 5 son **realizables en una tarde** y cubren: arquitectura, performance, testing, networking, y experiencia de usuario.
