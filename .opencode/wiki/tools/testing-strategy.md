---
tags:
  - wiki/tool
---

# Testing Strategy

> **Last verified:** 2026-05-19 | **Verified by:** [analysis]

## Stack de testing

| Herramienta | Uso |
|---|---|
| JUnit 5 | Test runner |
| MockK | Mocking de Kotlin (coroutines, suspend) |
| kotlinx.coroutines.test | Coroutine testing (`runTest`, `StandardTestDispatcher`) |
| Turbine | Flow testing (secuencia de emisiones) |
| MockWebServer | HTTP mocking (servidor local) |
| Compose UI Test | Testing de UI Compose |

## Tipos de tests

### Unit tests (ViewModel)

Prueban el comportamiento del ViewModel con un mock del Repository usando MockK:

```kotlin
class ArticlesListViewModelTest {
    @MockK lateinit var repository: ArticlesRepository

    @Test
    fun `when load articles succeeds then state is Success`() = runTest {
        coEvery { repository.getArticles(any(), any()) } returns Result.success(testArticles)

        val vm = ArticlesListViewModel(repository)
        // assert state transitions
    }
}
```

### Integration tests (HTTP + Repository)

Usan MockWebServer para simular la API real. Verifican que Retrofit parsea bien el JSON y que el Repository maneja códigos HTTP:

```kotlin
class ApiServiceTest {
    @get:Rule val mockWebServer = MockWebServerRule()

    @Test
    fun `getArticles returns parsed articles`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody(testJson).setResponseCode(200))
        val api = createApiService(mockWebServer.url("/"))
        val response = api.getArticles(20, 0)
        assertEquals(20, response.results.size)
    }
}
```

### ViewModel tests with Turbine

Verifican secuencias exactas de estados:

```kotlin
viewModel.description.test {
    assertEquals("", awaitItem())              // Initial
    assertEquals(loadingMessage, awaitItem())   // Loading
    assertEquals(finalDescription, awaitItem()) // Final
    awaitComplete()
}
```

## Patrones avanzados

### MainCoroutineTestRule

```kotlin
class MainCoroutineTestRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }
    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
```

### Custom Semantic Properties (UI testing)

Más robusto que `testTag`:

```kotlin
object CustomSemantics {
    val UiState = SemanticsPropertyKey<String>("UiState")
    val Variant = SemanticsPropertyKey<String>("Variant")
}

// En el componente:
Modifier.semantics {
    this[CustomSemantics.UiState] = "loading"
}

// En el test:
composeTestRule.onNode(SemanticsMatcher("is loading") {
    it.config[CustomSemantics.UiState] == "loading"
}).assertExists()
```

### Fakes vs Mocks

| Aspecto | Fakes | Mocks |
|---|---|---|
| Mantenimiento | Escribir una vez | `coEvery { ... } returns ...` en cada test |
| Fragilidad | Baja | Alta (cambios de API rompen mocks) |
| Legibilidad | El fake documenta el comportamiento | Arrange es verboso |
| Costo inicial | Alto | Bajo |

Para proyectos que crecen, los **fakes** son superiores. Se inyectan via Hilt `@TestInstallIn`:

```kotlin
@TestInstallIn(components = [SingletonComponent::class], replaces = [DataModule::class])
@Module
abstract class TestDataModule {
    @Binds abstract fun bindRepository(fake: FakeRepository): ArticlesRepository
}
```

### Paparazzi Screenshot Testing

Para detectar regresiones visuales. Captura screenshots en múltiples dispositivos y modos:

```kotlin
@Test fun `article list screenshot`() {
    paparazzi.snapshot {
        ArticleListScreen(
            attributes = ArticlesListAttributes(searchQuery = "", articles = sampleFlow),
            actions = noopActions,
        )
    }
}
```

## Archivos de testing en el proyecto

| Archivo | Tipo | Prueba |
|---|---|---|
| `TestArticleData.kt` | Fixture | Constantes de prueba compartidas |
| `TestJson.kt` | Fixture | JSON para MockWebServer |
| `MainDispatcherRule.kt` | Rule | Test CoroutineDispatcher |
| `MockWebServerRule.kt` | Rule | Servidor HTTP local |
| `ApiServiceTest.kt` | Integration | Parser y HTTP |
| `ArticlesRepositoryTest.kt` | Integration | Pipeline completo |
| `ArticlesListViewModelTest.kt` | Unit | ViewModel states |
| `ArticleDetailViewModelTest.kt` | Unit | Detail states |

## Ver también

- [[processes/build-and-test]] — Cómo compilar y testear
- [[patterns/error-handling]] — Error handling + testing
- [[tools/retrofit-setup]] — Retrofit + CallAdapter + testing