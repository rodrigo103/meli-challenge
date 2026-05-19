---
tags:
  - wiki/process
---

# Tech Defense Guide

> **Last verified:** 2026-05-19 | **Verified by:** [analysis]

Guía de defensa técnica para entrevistas, con argumentos sólidos sobre cada decisión arquitectónica del proyecto.

## Stack resumen

| Capa | Tecnología |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose (type-safe routes) |
| DI | Hilt + KSP |
| Red | Retrofit + OkHttp + kotlinx.serialization |
| Imágenes | Coil 3 |
| Estado | StateFlow + UiState sealed class |
| Testing | JUnit 5 + MockK + MockWebServer |
| Memoria | LeakCanary (debug only) |
| Logging | OkHttp logging condicional (BODY debug, BASIC release) |
| Errores HTTP | CallAdapter.Factory custom |

---

## 1. ¿Por qué Hilt y no Dagger vanilla?

"Hilt **es** Dagger. Hilt es una capa sobre Dagger que elimina boilerplate. En vez de armar `@Component`, `@Subcomponent`, builders y módulos manuales, uso `@HiltAndroidApp` y `@InstallIn`. Es menos código, menos errores, misma capacidad."

**Si preguntan por KSP vs KAPT:**

"Usé KSP porque es más rápido, tiene mejor soporte en AGP 9 (built-in Kotlin support) y es el reemplazo natural de KAPT. Dagger Hilt 2.50+ lo soporta."

---

## 2. ¿Por qué Navigation Compose y no navigation3?

"navigation3 es experimental y no está listo para producción. Tuve problemas concretos con SavedStateHandle que no recibía argumentos y crashes al instanciar ViewModels."

"Usé Navigation Compose con type-safe routes (`@Serializable` data classes). Es la API estable y recomendada por Google. Los argumentos se pasan con tipos seguros: `navController.navigate(DetailRoute(articleId))`. No hay strings mágicas ni errores en runtime."

---

## 3. Arquitectura: MVVM + Repository Pattern

```
Screen (Composable)
  └── hiltViewModel() → ViewModel
        └── @Inject constructor(repository)
              └── ArticlesRepository (interfaz)
                    └── DefaultArticlesRepository
                          └── ApiService (Retrofit)
```

**Puntos clave:**

1. "Cada capa tiene una única responsabilidad y depende de interfaces, no implementaciones concretas."
2. "El ViewModel no sabe si los datos vienen de la red o de caché. Solo conoce `ArticlesRepository`."
3. "Los `UiState` son `data class` inmutables. El ViewModel los expone como `StateFlow` y la UI reacciona a cambios."
4. "No hay estados imposibles: `isLoading=true` con `error!=null` nunca ocurre porque el ViewModel los actualiza atómicamente con `update {}`."

---

## 4. Manejo de errores

| Nivel | Mecanismo |
|---|---|
| Developer | `Timber.e()` con contexto + interceptor OkHttp para debugging |
| Usuario | Snackbar con mensaje amigable + reintentar |

"Uso un `CallAdapter.Factory` custom registrado en Retrofit. Intercepta la respuesta HTTP: si es 2xx la deja pasar, si no, parsea el error y tira una `ApiException` tipada. El Repository lo captura con `runCatching {}`."

"En debug, el interceptor de OkHttp loguea bodies completos. En release, solo loguea headers para no exponer datos."

### Timber vs android.util.Log

| Aspecto | `android.util.Log` | Timber |
|---|---|---|
| Tag automático | No | Sí, desde la clase |
| Logs en release | Quedan | Solo DebugTree en debug |
| Excepciones | `Log.e(TAG, msg, tr)` | `Timber.e(tr, msg)` natural |
| Formato | Solo concatenación | Type-safe con `%s` |
| Extensibilidad | No | Trees custom (Crashlytics, etc.) |
| Adopción | SDK estándar | De facto en apps profesionales |

---

## 5. Testing

```
tests/
├── data/
│   ├── ApiServiceTest.kt        # 7 tests de integración HTTP
│   └── ArticlesRepositoryTest.kt # Pipeline completo
├── ui/articles/list/
│   └── ArticlesListViewModelTest.kt  # 8 tests con MockK
└── ui/articles/detail/
    └── ArticleDetailViewModelTest.kt # 4 tests con MockK
```

### MockWebServer

"Mockea el servidor HTTP real. Retrofit le pega como si fuera la API de Space Flight News. Verifica que: 1) el endpoint está bien definido, 2) el JSON se serializa/deserializa correctamente, 3) los códigos HTTP de error se traducen a `Result.failure`."

### MockK vs Mockito

"MockK tiene soporte nativo para coroutines (`coEvery`, `coVerify`) y funciones `suspend`. Con Mockito necesitarías extensiones adicionales."

---

## 6. Decisiones técnicas adicionales

### Coil vs Glide

"Coil 3 tiene soporte nativo para Compose, Kotlin coroutines y OkHttp. Glide requiere adapters adicionales para funcionar con Compose."

### kotlinx.serialization vs Gson

"Es nativo de Kotlin, no usa reflection (más rápido, más seguro), y se integra con los type-safe routes de Navigation Compose."

### StateFlow vs LiveData

"StateFlow es reactivo, tiene soporte nativo para coroutines, y `collectAsStateWithLifecycle()` respeta el lifecycle de Compose. LiveData está diseñado para Views/Fragments, no para Compose."

### TOML Version Catalog vs buildSrc

| Aspecto | TOML | buildSrc |
|---|---|---|
| IDE support | Autocompletado parcial | Autocompletado, go-to-definition |
| Error detection | En runtime | En compilación |
| Velocidad de sync | Instantáneo | buildSrc compila primero |
| Custom logic | No soporta | Funciones helper |
| Recomendación oficial | **Sí** (Gradle 7.0+) | Anterior estándar |

"Usé TOML version catalog porque es la recomendación oficial de Gradle desde la 7.0. Para un proyecto de 1 módulo con ~20 dependencias, es más simple, más rápido y es lo que Google recomienda hoy."

### SplashScreen API vs Lottie

| Aspecto | SplashScreen API | Lottie |
|---|---|---|
| Cuándo | Cold start (antes de que la Activity exista) | Dentro de la app |
| Qué muestra | Icono + color de fondo | Animaciones After Effects |
| Personalización | Mínima | Total |

"Usé la SplashScreen API de Android porque es la solución oficial del OS. Mientras la app carga, el sistema ya muestra el icono. No hay stutter ni flash blanco."

### CoroutineDispatcher DI injectable

```kotlin
@Module @InstallIn(SingletonComponent::class)
object DispatcherModule {
    @IoDispatcher @Provides
    fun providesIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
```

"Este patrón permite inyectar dispatchers vía Hilt en vez de hardcodear `Dispatchers.IO`. En tests de integración, puedo reemplazar el módulo para proveer `StandardTestDispatcher`."

---

## 7. Si pudieras volver a empezar

1. "No habría usado navigation3. Perdí tiempo debugueando problemas de una librería experimental."
2. "Agregaría tests de integración con MockWebServer desde el día 1 para la capa HTTP."
3. "Aplicaría el patrón Attributes/Actions desde el principio para tener previews y screens puras sin depender de ViewModel."

## Ver también

- [[architecture/clean-architecture-guide]] — Clean Architecture en profundidad
- [[tools/testing-strategy]] — Testing patterns
- [[patterns/attributes-actions]] — Attributes/Actions pattern