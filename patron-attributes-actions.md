# Patrón Attributes/Actions

## Inspiración: Ualá (Abra design system)

En los proyectos de Ualá (CityApp, Home, ualaChallenge), los composables de pantalla siguen este patrón:

```kotlin
@Composable
fun HomeScreen(
    attributes: HomeScreenAttributes,  // todos los datos de UI
    actions: HomeScreenActions,        // todos los callbacks
)
```

Donde:
- **`HomeScreenAttributes`** — data class inmutable con todo el estado que la pantalla necesita para renderizarse
- **`HomeScreenActions`** — data class con todas las funciones callback que la pantalla puede disparar

Ejemplo real de Ualá (`HomeScreenAttributes.kt`):
```kotlin
data class HomeScreenAttributes(
    val loyaltyPoints: String? = null,
    val balanceData: List<BalanceTabData>? = null,
    val homeFeaturesList: List<HomeFeature> = emptyList(),
    val listOfCards: List<Card>? = null,
    val showNotificationAction: Boolean,
    val environment: Environment,
    // ... más campos
)
```

```kotlin
data class HomeScreenActions(
    val refreshHome: () -> Unit,
    val onCardClick: (String) -> Unit,
    val onActionButtonClick: (HomeFeature) -> Unit,
    val onBalanceDetailsClick: (BalanceDetail?, HomeRemunerationRate?) -> Unit,
    // ... más callbacks
)
```

---

## ¿Qué problema resuelve?

### Antes (nuestro código original)

```kotlin
@Composable
fun ArticlesListScreen(
    onArticleClick: (Int) -> Unit,   // parámetros sueltos
    modifier: Modifier = Modifier,
) {
    val vm = hiltViewModel()          // dependencia directa de Hilt
    val articles = vm.articles.collectAsLazyPagingItems()
    val searchQuery by vm.searchQuery.collectAsStateWithLifecycle()
    // viewModel.searchQuery, viewModel.onSearchTextChange, viewModel.clearSearch
}
```

Problemas:
- ❌ No se puede previsualizar (Hilt no funciona en previews)
- ❌ La screen sabe de ViewModel, Hilt, StateFlow, Lifecycle
- ❌ Testear el UI requiere un ViewModel real
- ❌ El signature escala mal (si agregás un callback, cambiás el API de la screen)

### Después (patrón Attributes/Actions)

```kotlin
// State: datos inmutables que la screen necesita
data class ArticlesListAttributes(
    val searchQuery: String,
    val articles: Flow<PagingData<Article>>,   // reactivo
)

// Actions: callbacks que la screen dispara
data class ArticlesListActions(
    val onSearchTextChange: (String) -> Unit,
    val onClearSearch: () -> Unit,
    val onArticleClick: (Int) -> Unit,
)

// Screen: función pura, sin Hilt
@Composable
fun ArticlesListScreen(
    attributes: ArticlesListAttributes,
    actions: ArticlesListActions,
    modifier: Modifier = Modifier,
) {
    val articles = attributes.articles.collectAsLazyPagingItems()
    // usa actions.onSearchTextChange, actions.onClearSearch, etc.
}
```

---

## Cómo funciona el flujo

```
   ViewModel (Hilt)
    │
    ├── searchQuery: StateFlow<String>
    ├── articles: Flow<PagingData<Article>>
    ├── onSearchTextChange(text)
    ├── clearSearch()
    └── sendAnalytics(event, properties)   ← wrapper de AnalyticsHelper
         │
         ▼
    Route composable (conecta ViewModel → Screen)
         │
         ├── ArticlesListAttributes(searchQuery, articles)
         └── ArticlesListActions(
                  onSearchTextChange,
                  onClearSearch,
                  onArticleClick,             ← directo, sin chain
                  sendAnalytics,              ← viewModel::sendAnalytics
              )
              │
              ▼
         ArticlesListScreen(attributes, actions)   ← función pura
              │
              ├── produce attributes.articles.collectAsLazyPagingItems()
              ├── usa actions.onSearchTextChange en SearchBar
              └── actions.sendAnalytics + actions.onArticleClick en cada card
```

---

## Analytics: `sendAnalytics` como parte de Actions

En Ualá, el ViewModel expone un método público `sendAnalytics` que wrappea el `AnalyticsHelper`:

```kotlin
// ViewModel
fun sendAnalytics(event: String, properties: Map<String, String>) {
    analyticsHelper.logEvent(event, properties)
}

// Actions
data class ArticlesListActions(
    val onSearchTextChange: (String) -> Unit,
    val onClearSearch: () -> Unit,
    val onArticleClick: (Int) -> Unit,
    val sendAnalytics: (String, Map<String, String>) -> Unit,   ← provisto por el VM
)

// Screen: callbacks de usuario disparan analytics
articleCardSettings(
    article = article,
    onClick = {
        actions.sendAnalytics("article_selected", mapOf("id" to article.id.toString()))
        actions.onArticleClick(article.id)
    },
)()

// Preview: sendAnalytics se reemplaza por no-op
actions = ArticlesListActions(
    ...
    sendAnalytics = { _, _ -> },
)
```

### Separación de responsabilidades

| Evento | Dónde se trackea | Razón |
|--------|------------------|-------|
| `screen_view` | `init` del ViewModel | Evento de ciclo de vida, no de UI |
| `article_selected` | Screen vía `actions.sendAnalytics` | Acción de usuario, pertenece a la UI |
| `article_loaded` | `loadArticle()` en DetailViewModel | Evento de infraestructura, no de UI |

En el Route (phone), `onArticleClick` pasa directo como callback de navegación. En DualPaneScreen (tablet), `onArticleClick` llama `viewModel.onArticleSelected(id)` para actualizar el panel derecho. En ambos casos, `sendAnalytics` viene del ViewModel y la screen lo llama antes del click.

---

## Previews: el beneficio principal

```kotlin
@Preview
@Composable
fun ArticlesListScreenWithDataPreview() {
    MaterialTheme {
        ArticlesListScreen(
            attributes = ArticlesListAttributes(
                searchQuery = "",
                articles = flow { emit(PagingData.from(sampleArticles)) },
            ),
            actions = ArticlesListActions(
                onSearchTextChange = {},
                onClearSearch = {},
                onArticleClick = {},
                sendAnalytics = { _, _ -> },   ← no-op en preview
            ),
        )
    }
}
```

| Preview | Atributos | Estado visual |
|---------|-----------|---------------|
| `ArticlesListScreenWithDataPreview` | `flow { PagingData.from(list) }` | Cards con artículos |
| `ArticlesListScreenEmptyPreview` | `flow { PagingData.empty() }` | "No articles available" |
| `ArticlesListScreenSearchPreview` | `searchQuery = "mars"`, 1 resultado | "No results found" |
| `ArticlesListScreenLoadingPreview` | `flow { }` (nunca emite) | Lottie animación |
| `ArticleDetailScreenSuccessPreview` | `UiState.Success(fakeArticle)` | Artículo completo |
| `ArticleDetailScreenLoadingPreview` | `UiState.Loading` | Lottie animación |
| `ArticleDetailScreenErrorPreview` | `UiState.Error("msg")` | Mensaje de error |

---

## Relación con otros patrones

### Settings pattern (previo)

Las Settings (`ArticleCardSettings`, `ArticleDetailContentSettings`) son ORTOGONALES al patrón Attributes/Actions:
- **Settings**: envuelven componentes pequeños con una interfaz `@Stable` + factory function
- **Attributes/Actions**: estructuran pantallas completas y su relación con el ViewModel

Ambos conviven: la screen usa `articleCardSettings(...)()` dentro del renderizado, mientras recibe datos via `attributes/actions`.

### UiState sealed class

`UiState<T>` (Loading / Success / Error) se usa dentro de `ArticleDetailAttributes`:

```kotlin
data class ArticleDetailAttributes(
    val state: UiState<ArticleDetailState>,   // ← UiState dentro de attributes
)
```

---

## Archivos creados/modificados

### Nuevos (6)

| Archivo | Contenido |
|---------|-----------|
| `ui/articles/list/ArticlesListScreenState.kt` | `ArticlesListAttributes` + `ArticlesListActions` |
| `ui/articles/list/ArticlesListScreenRoute.kt` | Connector ViewModel → Screen |
| `ui/articles/detail/ArticleDetailScreenState.kt` | `ArticleDetailAttributes` + `ArticleDetailActions` |
| `ui/articles/detail/ArticleDetailScreenRoute.kt` | Connector ViewModel → Screen |
| `ui/preview/ArticlesListScreenPreviews.kt` | 4 previews de list + 2 de card |
| `ui/preview/ArticleDetailScreenPreviews.kt` | 3 previews de detail |

### Modificados (5)

| Archivo | Cambio |
|---------|--------|
| `ArticlesListScreen.kt` | Signature: `(attributes, actions)` en vez de `(onArticleClick)`. Sin Hilt ni previews. |
| `ArticleDetailScreen.kt` | Signature: `(attributes, actions)` en vez de `(onBack)`. Sin Hilt. |
| `ArticlesListViewModel.kt` | Agrega `sendAnalytics()`, elimina `analytics.logEvent` de `onArticleSelected`. |
| `Navigation.kt` | Usa `*Route` composables en vez de llamar screens directo |
| `DualPaneScreen.kt` | Crea `ArticlesListAttributes` + `ArticlesListActions` desde ViewModel |

---

## ¿Por qué no es overkill para 2 pantallas?

1. **Defensa técnica** — es un patrón que se puede explicar y defender en la entrevista
2. **Escalable** — si el challenge se expandiera, el patrón ya está
3. **Mínimo boilerplate** — 4 data classes de 3-4 campos cada una
4. **Previews reales** — se ven los 3 estados de cada pantalla sin mockear ViewModels
