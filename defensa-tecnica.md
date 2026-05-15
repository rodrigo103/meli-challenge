# Guía de Defensa Técnica — Space Flight News App

## Stack resumen

| Capa | Tecnología |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose (type-safe routes) |
| DI | Hilt + KSP |
| Red | Retrofit + OkHttp + kotlinx.serialization |
| Imágenes | Coil 3 |
| Estado | StateFlow + UiState data classes |
| Testing | JUnit 4 + MockK + MockWebServer |
| Memoria | LeakCanary (debug only) |
| Logging | OkHttp logging condicional (BODY en debug, BASIC en release) |
| Errores HTTP | `Response<T>` con `extractBody()` |

---

## 1. ¿Por qué Hilt y no Dagger vanilla?

**Hilt ES Dagger.** Hilt es una capa sobre Dagger que elimina boilerplate. Cuando decís "uso Hilt", estás diciendo que entendés Dagger pero además sabés que Google recomienda no escribirlo a mano.

**Puntos para defender:**

1. *"Hilt simplifica el setup sin perder poder de Dagger. En vez de armar `@Component`, `@Subcomponent`, builders y módulos manuales, uso `@HiltAndroidApp` y `@InstallIn`. Es menos código, menos errores, misma capacidad."*

2. *"En un challenge de 2 pantallas, Dagger vanilla es over-engineering. Pero saber que existe y que Hilt se apoya en él muestra que entiendo la diferencia."*

3. *"Si el proyecto escala a más módulos o features, Hilt escala igual que Dagger porque es el mismo motor."*

4. *"Google recomienda Hilt explícitamente en la documentación oficial. Usarlo muestra que sigo buenas prácticas actuales."*

**Si preguntan por KSP vs KAPT:**

- *"Usé KSP en vez de KAPT porque es más rápido, tiene mejor soporte en AGP 9 (built-in Kotlin support) y es el reemplazo natural de KAPT. Dagger Hilt 2.50+ lo soporta."*

---

## 2. ¿Por qué Navigation Compose y no navigation3?

navigation3 es experimental y no está listo para producción. Tuve problemas concretos con él (SavedStateHandle no recibía argumentos, crash al instanciar ViewModels), lo que confirmó que era una mala elección.

**Respuesta sólida:**

- *"Usé Navigation Compose con type-safe routes (`@Serializable` data classes). Es la API estable y recomendada por Google para navegación en Compose."*
- *"Los argumentos se pasan con tipos seguros: `navController.navigate(DetailRoute(articleId))`. No hay strings mágicas ni errores en runtime."*
- *"El `SavedStateHandle` se integra naturalmente con `@HiltViewModel` y los argumentos de navegación."*

---

## 3. Arquitectura: MVVM + Repository Pattern

```
┌─────────────────────────────────────────────────────┐
│  Screen (Composable)                                │
│   └── hiltViewModel() → ViewModel                    │
│         └── @Inject constructor(repository)          │
│               └── ArticlesRepository (interfaz)      │
│                     └── DefaultArticlesRepository    │
│                           └── ApiService (Retrofit)  │
└─────────────────────────────────────────────────────┘
```

**Puntos clave:**

1. *"Cada capa tiene una única responsabilidad y depende de interfaces, no implementaciones concretas."*
2. *"El ViewModel no sabe si los datos vienen de la red o de caché. Solo conoce `ArticlesRepository`."*
3. *"Los `UiState` son `data class` imutables. El ViewModel los expone como `StateFlow` y la UI reacciona a cambios."*
4. *"No hay estados imposibles: `isLoading=true` con `error!=null` nunca ocurre porque el ViewModel los actualiza atómicamente con `update {}`."*

---

## 4. Manejo de errores

**Dos niveles:**

| Nivel | Mecanismo |
|---|---|
| Developer | `Log.e(TAG, ...)` con contexto del error + interceptor de OkHttp para debugging |
| Usuario | Snackbar con mensaje amigable + posibilidad de reintentar |

**HTTP errors:**

- *"Uso `Response<T>` de Retrofit para capturar códigos HTTP. Si la API responde con 404 o 500, `extractBody()` tira un `IOException` con el código y el mensaje. El `runCatching` en el Repository lo transforma en `Result.failure`."*
- *"En debug, el interceptor de OkHttp loguea bodies completos. En release, solo loguea headers para no exponer datos."*
- *"LeakCanary detecta memory leaks automáticamente en debug builds."*

---

## 5. Testing

```
tests/
├── TestArticleData.kt          # Constantes de prueba compartidas
├── test/MainDispatcherRule.kt   # Rule para coroutines
├── ui/articles/list/
│   └── ArticlesListViewModelTest.kt  # 8 tests con MockK
└── ui/articles/detail/
    └── ArticleDetailViewModelTest.kt # 4 tests con MockK
```

**Lo que cubren los tests:**

- Success y failure de cada operación del ViewModel
- Paginación (append de artículos)
- Búsqueda (replace de resultados)
- Limpieza de estados (clearSearch, clearError)
- Casos borde (SavedStateHandle sin articleId)

**Por qué MockK y no Mockito:**

- *"MockK tiene soporte nativo para coroutines (`coEvery`, `coVerify`) y funciones `suspend`. Con Mockito necesitarías `mockito-kotlin` y extensiones adicionales."*

---

## 6. Clean Architecture + Use Cases (para cuando pregunten)

**Texto de referencia:**

> *"En este proyecto no implementé Clean Architecture formal con use cases porque es un challenge de 2 pantallas con una sola fuente de datos (API REST). Agregar una capa `domain` con use cases habría sido over-engineering: cada use case sería un pasamanos que delega directo al repositorio."*
>
> *"Pero conozco el patrón. Clean Architecture separa el código en 3 capas:*
>
> - ***data**: APIs, Room, repositorios concretos*
> - ***domain**: entidades de negocio, interfaces de repositorio, use cases*
> - ***presentation**: ViewModels, UI, navegación*
>
> *Los use cases encapsulan una operación de negocio atómica (`SearchCitiesUseCase`, `FetchCitiesUseCase`, etc.). Se justifican cuando:*
> 1. *La lógica de negocio se reutiliza entre varios ViewModels*
> 2. *Hay reglas complejas (validaciones, transformaciones, caching)*
> 3. *Se combinan múltiples fuentes de datos (API + Room + preferencias)*
>
> *Para este challenge, el Repository Pattern alcanza. Pero si hubiera tenido que agregar offline-first con Room o compartir lógica entre pantallas, habría usado use cases."*

---

## 7. Decisiones técnicas adicionales

### Coil vs Glide
- *"Coil 3 tiene soporte nativo para Compose, Kotlin coroutines y OkHttp. Glide requiere adapters adicionales para funcionar con Compose."*

### kotlinx.serialization vs Gson
- *"Es nativo de Kotlin, no usa reflection (más rápido, más seguro), y se integra con los type-safe routes de Navigation Compose."*

### StateFlow vs LiveData
- *"StateFlow es reactivo, tiene soporte nativo para coroutines, y `collectAsStateWithLifecycle()` respeta el lifecycle de Compose. LiveData está diseñado para Views/Fragments, no para Compose."*

### Paging
- *"No implementé Paging3 porque el endpoint de Space Flight News devuelve listas paginadas con `offset`/`limit`. Para 2 pantallas, la paginación manual con `LazyColumn` + `derivedStateOf` es suficiente."*

---

## 8. Si pudieras volver a empezar, ¿qué harías diferente?

Buena pregunta para el final. Respuesta honesta:

1. *"No habría usado navigation3. Perdí tiempo debugueando problemas de una librería experimental."*
2. *"Agregaría tests de integración con MockWebServer para la capa HTTP. Los tests unitarios de ViewModels están bien, pero un test que verifique que el parseo de la API funciona da más seguridad."*
3. *"Tal vez usaría buildSrc para las versiones de dependencias en vez del version catalog TOML, pero es cuestión de preferencia."*
