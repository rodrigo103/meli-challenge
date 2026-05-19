---
tags:
  - wiki/architecture
---

# Clean Architecture Guide

> **Last verified:** 2026-05-19 | **Verified by:** [analysis]

## Las tres capas

Clean Architecture organiza el código en capas concéntricas donde **las capas internas no conocen a las externas**.

```
┌──────────────────────────────────────────────┐
│  presentation (UI, ViewModels, navegación)   │
│  ┌────────────────────────────────────────┐  │
│  │  domain (entidades, interfaces, casos) │  │
│  │  ┌──────────────────────────────────┐  │  │
│  │  │  data (APIs, DB, implementaciones)│  │  │
│  │  └──────────────────────────────────┘  │  │
│  └────────────────────────────────────────┘  │
└──────────────────────────────────────────────┘

       data → domain ← presentation
       (nadie depende de domain, domain depende de nadie)
```

### Capa `data` — "Cómo obtengo y guardo los datos"

**Contiene:** API services, DAOs, DataSources, implementaciones de repositorios, DTOs, entities de Room, mappers.

**No contiene:** Lógica de negocio, lógica de UI, ViewModels.

```kotlin
// data/datasource/remote/api/ApiService.kt
interface ApiService {
    @GET("articles/")
    suspend fun getArticles(
        @Query("limit") limit: Int, @Query("offset") offset: Int
    ): ArticleResponse
}

// data/datasource/remote/RemoteDataSource.kt
class RemoteDataSource @Inject constructor(private val api: ApiService) {
    suspend fun fetchArticles(limit: Int, offset: Int) = api.getArticles(limit, offset)
}

// data/datasource/remote/models/ArticleDto.kt
@Serializable
data class ArticleDto(
    val id: Int,
    val title: String,
    @SerialName("image_url") val imageUrl: String?,
)

// data/datasource/local/entities/ArticleEntity.kt
@Entity(tableName = "articles")
data class ArticleEntity(@PrimaryKey val id: Int, val title: String, val imageUrl: String?)

// data/datasource/local/LocalDataSource.kt
class LocalDataSource @Inject constructor(private val dao: ArticlesDao) {
    fun getArticlesPaged() = dao.getAll()
    suspend fun cacheArticles(articles: List<ArticleEntity>) = dao.insertAll(articles)
}

// data/Mappers.kt
fun ArticleDto.toEntity() = ArticleEntity(id = id, title = title, imageUrl = imageUrl)
fun ArticleEntity.toDomain() = Article(id = id, title = title, imageUrl = imageUrl)

// data/repository/ArticlesRepositoryImpl.kt
class ArticlesRepositoryImpl @Inject constructor(
    private val remote: RemoteDataSource,
    private val local: LocalDataSource,
) : ArticlesRepository {
    override fun getArticles(): Flow<PagingData<Article>> =
        Pager(PagingConfig(pageSize = 20)) { local.getArticlesPaged() }
            .flow.map { it.map { entity -> entity.toDomain() } }
}
```

### Capa `domain` — "Qué reglas de negocio aplican"

**Contiene:** Entidades de dominio, interfaces de repositorio, use cases.

**No contiene:** Nada de Android (`Context`, `ViewModel`), nada de frameworks (`@GET`, `@Entity`).

**Es la capa más pura:** si migrás de Android a KMM o a un backend, esta capa se reutiliza.

```kotlin
// domain/models/Article.kt
data class Article(val id: Int, val title: String, val imageUrl: String?)

// domain/repository/ArticlesRepository.kt (interfaz, NO implementación)
interface ArticlesRepository {
    fun getArticles(): Flow<PagingData<Article>>
    suspend fun getArticle(id: Int): Result<Article>
}

// domain/usecases/GetArticlesUseCase.kt
class GetArticlesUseCase @Inject constructor(
    private val repository: ArticlesRepository
) {
    operator fun invoke(): Flow<PagingData<Article>> = repository.getArticles()
}
```

El `operator fun invoke()` permite llamar al use case como función:

```kotlin
val useCase = GetArticlesUseCase(repository)
useCase()  // vs useCase.execute()
```

### Capa `presentation` — "Qué ve y qué hace el usuario"

**Contiene:** ViewModels, UiState, Composable screens, Navigation.

**No contiene:** Lógica de negocio, acceso directo a APIs o bases de datos.

```kotlin
@HiltViewModel
class ArticlesViewModel @Inject constructor(
    private val getArticles: GetArticlesUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ArticlesUiState())
    val uiState = _uiState.asStateFlow()

    fun loadArticles() {
        viewModelScope.launch {
            getArticles().collect { pagingData ->
                _uiState.update { it.copy(articles = pagingData) }
            }
        }
    }
}
```

## La regla de dependencia

```
  data ──conoce──→ domain ←──conoce── presentation
```

- **domain** no tiene imports de Android, Retrofit, Room ni Compose. Es Kotlin puro.
- **data** implementa las interfaces que define domain. Conoce a domain.
- **presentation** consume las interfaces de domain. Conoce a domain.

El pegamento es **Hilt**: en los módulos de `di/` se bindea la interfaz a la implementación concreta.

## ¿Cuándo se justifica Clean Architecture completa?

| Señal | Acción |
|---|---|
| Más de una fuente de datos (API + Room) | Agregar `LocalDataSource` + mappers |
| Lógica de negocio en el ViewModel | Extraer a use cases en `domain/` |
| Código repetido entre ViewModels | Unificar en use cases |
| Múltiples módulos o features | Separar por feature con su propia capa domain |
| Proyecto con +5 pantallas | Clean Architecture desde el día 1 |

## ¿Cuándo NO usar use cases?

Si un use case solo delega al repositorio sin lógica real:

```kotlin
class GetArticlesUseCase(repo: ArticlesRepository) {
    operator fun invoke() = repo.getArticles() // pasamanos vacío
}
```

Esto no agrega valor. Con el Repository Pattern + interfaz ya se desacopla el ViewModel de la fuente de datos. Aplicar YAGNI.

## Agregar offline-first

1. Crear `ArticleEntity` (Room) en `data/datasource/local/entities/`
2. Crear `ArticlesDao` + `AppDatabase`
3. Crear `LocalDataSource` que wrappea el DAO
4. Crear mappers DTO → Entity → Domain
5. Modificar el repositorio: primero devuelve datos de Room, después refresca de API
6. ViewModel no cambia — sigue consumiendo la misma interfaz

## Ver también

- [[patterns/mvvm-repository]] — MVVM + Repository
- [[architecture/di-hierarchy]] — DI modules
- [[architecture/data-layer]] — Data layer concreto del proyecto