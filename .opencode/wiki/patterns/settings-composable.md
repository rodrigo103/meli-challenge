---
tags:
  - wiki/pattern
---

# Settings + Composable Pattern

> **Last verified:** 2026-05-19 | **Verified by:** [analysis]

## Qué es

Patrón de diseño de componentes donde cada componente tiene una **Settings data class** (interfaz `@Stable`) que encapsula toda su configuración y puede ser invocada como composable mediante `operator fun invoke()`.

Separa la **configuración** del componente de su **renderizado**, permitiendo reusar configuraciones, testearlas independientemente, y crear DSLs declarativos.

## Estructura

```kotlin
@Stable
interface ArticleCardSettings {
    val article: Article
    val onFavoriteClick: (Article) -> Unit
    val modifier: Modifier

    @Composable
    operator fun invoke(
        modifier: Modifier = Modifier,
    ) = ArticleCard(
        article = article,
        onFavoriteClick = onFavoriteClick,
        modifier = modifier,
    )
}

// Factory function
fun articleCardSettings(
    article: Article,
    onFavoriteClick: (Article) -> Unit,
): ArticleCardSettings = object : ArticleCardSettings {
    override val article = article
    override val onFavoriteClick = onFavoriteClick
    override val modifier = Modifier
}
```

### Uso

```kotlin
// Definir configuración
val settings = articleCardSettings(
    article = article,
    onFavoriteClick = { favoriteArticle(it) },
)

// Renderizar con valores default
settings()

// Renderizar con override parcial
settings(modifier = Modifier.padding(16.dp))
```

## Color interfaces con estado

Cada componente define su propia interfaz de colores con métodos que reciben el estado de interacción:

```kotlin
@Stable
interface CardColors {
    @Composable fun backgroundColor(enabled: Boolean, pressed: Boolean): State<Color>
    @Composable fun contentColor(enabled: Boolean, pressed: Boolean): State<Color>
    @Composable fun borderColor(enabled: Boolean, pressed: Boolean): State<Color?>
}
```

## Sistema de tipografía escalable

```kotlin
// Typography con nombres semánticos
data class AppTypography(
    val bodyLgRegular: TextStyle,
    val bodyLgBold: TextStyle,
    val bodyMdRegular: TextStyle,
    val bodySmRegular: TextStyle,
    val labelLgBold: TextStyle,
    val buttonLgRegular: TextStyle,
    val titleMd: TextStyle,
    val headingSm: TextStyle,
)

val LocalAppTypography = staticCompositionLocalOf<AppTypography> { ... }
```

## `CompositionLocal`-based theming

```kotlin
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val typography = if (darkTheme) DarkTypography else LightTypography

    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalAppTypography provides typography,
        content = content
    )
}
```

## Relación con Attributes/Actions

Settings y Attributes/Actions son **ortogonales**:
- **Settings**: envuelven componentes pequeños con interfaz `@Stable` + factory function
- **Attributes/Actions**: estructuran pantallas completas y su relación con el ViewModel

Ambos conviven: la screen usa `articleCardSettings(...)()` dentro del renderizado, mientras recibe datos via `attributes/actions`.

## Beneficios

| Beneficio | Descripción |
|-----------|-------------|
| Configuración reutilizable | Un mismo Settings se puede usar en múltiples pantallas |
| Testeable independientemente | Se puede testear el componente sin la pantalla completa |
| DSL declarativo | Las pantallas arman Settings y los invocan |
| API explícita | La interfaz documenta cada opción de configuración |
| Override parcial | Se pueden cambiar props específicas sin redefinir todo |

## Ver también

- [[patterns/attributes-actions]] — Attributes/Actions pattern
- [[patterns/mvvm-repository]] — MVVM + Repository