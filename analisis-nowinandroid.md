# Análisis: Now in Android (Google Sample App)

> **Repo:** [google/nowinandroid](https://github.com/google/nowinandroid) | **Last verified:** 2026-05-18 | **Verified by:** [analysis]
> **Path:** `/Users/rodrigo/Downloads/UalaCodigo/nowinandroid`

App oficial de Google que demuestra las mejores prácticas de Android. Compose, MVVM, Hilt, Room, Navigation3, WorkManager, screenshot testing, y benchmarks.

---

## Stack técnico

| Aspecto | nowinandroid | proyecto-android | Recomendación |
|---|---|---|---|
| UI | Compose + Material3 | Compose + Material3 | ✅ Ok |
| Navigation | **Navigation3** (`1.0.0`) con NavKeys tipadas | Navigation Compose (`2.9.0`) con Routes serializables | Evaluar Navigation3 a futuro |
| DI | Hilt (15 módulos) | Hilt (5 módulos) | ✅ Ok |
| Estado | `StateFlow` + sealed interface `UiState` | `StateFlow` + sealed class `UiState` | ✅ Ok |
| Red | Retrofit + kotlinx-serialization | Retrofit + kotlinx-serialization | ✅ Ok |
| DB | Room (6 entidades, 14 auto-migrations, FTS) | Room (1 entidad) | ✅ Ok |
| Concurrencia | `combine` + `flatMapLatest` + `cachedIn` | `flowOf` básico | Usar más Flow operators |
| Testing | **Fakes + Hilt test** (sin mocks) | MockK + MockWebServer | Evaluar fakes |
| Screenshots | Roborazzi multi-device + multi-theme | No tiene | — |
| CI/CD | Convention plugins + Gradle Managed Devices | GitHub Actions manual | Evaluar GMD |
| Formateo | **Spotless** | Detekt | Evaluar Spotless |
| Performance | Baseline Profiles + Benchmarks + JankStats | No tiene | Agregar baseline profiles |

---

## Arquitectura de módulos

```
app/
core/
  analytics/       — interface + Firebase/stub impl por flavor
  common/          — utilidades compartidas
  data/            — repositorios offline-first
  data-test/       — fakes para testing
  database/        — Room + DAOs + FTS
  datastore/       — DataStore Preferences + Proto
  designsystem/    — tema, colores, tipografía, componentes
  domain/          — modelos de dominio (sin Android)
  model/           — data classes de API
  navigation/      — Navigation3 con NavBackStack + NavKeys
  network/         — Retrofit + demo/prod por flavor
  notifications/   — FCM wrapper
  screenshot-testing/
  testing/         — utilidades de test
  ui/              — componentes Compose compartidos
feature/
  foryou/:api|impl
  interests/:api|impl
  bookmarks/:api|impl
  topic/:api|impl
  search/:api|impl
  settings/:impl
sync/
  work/            — WorkManager sync con delta changes
lint/
benchmarks/
```

Cada feature tiene dos módulos: `:api` (NavKey) y `:impl` (UI + ViewModel + DI). No hay dependencias circulares — los features solo se conocen en `:app`.

---

## Patrones destacados

### 1. Convention plugins en build-logic

15 plugins custom que encapsulan toda la configuración de build. Cada módulo aplica 2-3 plugins y hereda SDK, Kotlin, Compose, testing automáticamente.

```kotlin
plugins {
    id("nowinandroid.android.application")
    id("nowinandroid.android.application.compose")
    id("nowinandroid.android.application.flavors")
}
```

**No más copy-paste de `compileSdk`, `minSdk`, `jvmTarget` entre módulos.** [source]

### 2. Sealed interface UiState (sin mocks)

Todos los ViewModels exponen un `StateFlow<UiState>` con `Loading` / `Success(data)`:

```kotlin
sealed interface MainActivityUiState {
    data object Loading : MainActivityUiState
    data class Success(val userData: UserData) : MainActivityUiState
}
```

Tests sin MockK — se inyectan fakes via `@TestInstallIn` en Hilt: [source]

```kotlin
@TestInstallIn(components = [SingletonComponent::class], replaces = [DataModule::class])
@Module
abstract class TestDataModule {
    @Binds abstract fun bindNewsRepository(fake: FakeNewsRepository): NewsRepository
}
```

### 3. Offline-first con delta sync

El `Synchronizer` + `Syncable` implementa sincronización eficiente mediante change lists: [source]

```kotlin
interface Synchronizer {
    suspend fun getChangeListVersions(): ChangeListVersions
}

interface Syncable {
    suspend fun syncWith(synchronizer: Synchronizer): Boolean
}
```

Solo descarga los cambios desde la última versión conocida, no el dataset completo.

### 4. CompositeUserNewsResourceRepository

Combina dos repositorios vía `combine` para enriquecer datos de noticias con preferencias de usuario: [source]

```kotlin
fun observeAll(query: NewsResourceQuery): Flow<List<UserNewsResource>> =
    newsRepository.getNewsResources(query)
        .flatMapLatest { newsResources ->
            userDataRepository.userData.map { userData ->
                newsResources.map { UserNewsResource(it, userData) }
            }
        }
```

### 5. Navigation3 con NavKeys tipadas

Sin strings. Cada destino es un `NavKey` data class: [source]

```kotlin
// En :feature:foryou:api
data class ForYouNavKey(val deepLinkUri: String?) : NavKey {
    override val key: String get() = "for_you"
}
```

### 6. enableEdgeToEdge() reactivo

Se re-ejecuta cuando cambia el tema dark/light para ajustar los scrims de las barras del sistema: [source]

```kotlin
lifecycleScope.launch {
    lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        combine(isSystemInDarkTheme(), viewModel.uiState) { dark, _ ->
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.auto(
                    lightScrim = Color.TRANSPARENT,
                    darkScrim = Color.TRANSPARENT,
                ) { dark },
            )
        }.collect()
    }
}
```

### 7. Screenshot testing multi-dispositivo con Roborazzi

Captura screenshots en phone, foldable, y tablet × light/dark × default/Android/dynamic theme: [source]

```kotlin
enum class DefaultTestDevices(val spec: String) {
    PHONE("spec:shape=Normal,width=640,height=360,unit=dp,dpi=480"),
    FOLDABLE("spec:shape=Normal,width=673,height=841,unit=dp,dpi=480"),
    TABLET("spec:shape=Normal,width=1280,height=800,unit=dp,dpi=480"),
}
```

Pixel-perfect con `changeThreshold = 0f`.

### 8. dagger.Lazy para dependencias pesadas

OkHttp, Coil ImageLoader y JankStats se inicializan lazy para no bloquear el startup: [source]

```kotlin
@Inject lateinit var imageLoader: dagger.Lazy<ImageLoader>
@Inject lateinit var lazyStats: dagger.Lazy<JankStats>
```

### 9. Flavor-specific DI modules

Las implementaciones de red, analytics y sync se eligen en compile-time sin `if` checks: [source]

```
core/network/src/
  main/          — interface + modelos
  demo/          — DemoNiaNetworkDataSource (lee JSON local)
  prod/          — RetrofitNiaNetwork (API real)
```

### 10. Gradle Managed Devices para CI

Dispositivos preconfigurados que garantizan consistencia entre local y CI: [source]

```kotlin
configureGmdProvider {
    "pixel4api34"(GmdPixel4)
    "pixel6api34"(GmdPixel6)
    "tabletPixelCApi34"(GmdPixelC)
}
```

---

## Cosas que ya tenemos (o están en camino)

| Patrón | Estado |
|---|---|
| `UiState` sealed class | ✅ Implementado |
| Hilt multi-módulo | ✅ Implementado |
| Room + Paging 3 | ✅ Implementado |
| Attributes/Actions pattern | ✅ Implementado (inspirado en Ualá) |
| `AnalyticsHelper` interface + Firebase impl | ✅ Implementado |
| Compose BOM en libs.versions.toml | ✅ Implementado |
| Detekt | ✅ Implementado |
| CI workflow (GitHub Actions) | ✅ Implementado |
| Instrumented tests | ✅ Implementado |
| `flatMapLatest` + `debounce` en búsqueda | ✅ Implementado |

---

## Mejoras que podríamos adoptar de nowinandroid

| Prioridad | Patrón | Esfuerzo | Impacto |
|---|---|---|---|
| 🥇 | **Convention plugins** (build-logic) | Alto | Centraliza build config, elimina copypaste |
| 🥇 | **Fakes en vez de mocks** para testing | Medio | Menos brittle, más legible |
| 🥈 | **Flavor-specific source sets** (`demo`/`prod`) | Medio | Cambio de implementación en compile-time |
| 🥈 | **Synchronizer pattern** para sync eficiente | Alto | Solo si necesitás delta updates |
| 🥈 | **Screenshot testing** con Roborazzi | Medio | Detecta regresiones visuales |
| 🥉 | **Baseline Profiles** + Benchmarks | Medio | Mejora startup en release |
| 🥉 | **Spotless** para formateo automático | Bajo | Complementa a Detekt |
| 🥉 | **Dependency Guard** | Bajo | Evita leaks de dependencias |
| 🥉 | **dagger.Lazy** para dependencias caras | Bajo | Optimización de startup |

---

## Lecciones para nuestra app

1. **El patrón sealed interface UiState es el estándar** — ya lo tenemos, mantenerlo consistente en todos los ViewModels es clave.

2. **Los fakes son superiores a los mocks** — eliminan la fragilidad de `coEvery { ... } returns ...` y permiten tests que leen como documentación. El costo inicial de escribir fakes se paga solo con tests más mantenibles.

3. **La modularización por feature** con `:api`/`:impl` evita dependencias circulares sin sacrificar navegación type-safe. Para nuestro proyecto más chico no aplica todavía, pero es el norte si crece.

4. **El offline-first con Room + WorkManager + change lists** es el gold standard de sincronización. Para nuestra app que solo consume una API pública, Paging con RemoteMediator ya alcanza.

5. **Los screenshot tests con Roborazzi + multi-dispositivo** atrapan regresiones visuales que ningún unit test ve. Vale la pena cuando la UI madura.

6. **NIA no usa `SearchBar` de Material3** — usa `TextField` simple, igual que nosotros ahora.
