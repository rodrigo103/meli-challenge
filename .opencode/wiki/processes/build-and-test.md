---
tags:
  - wiki/process
---

# Build & Test

> **Last verified:** 2026-05-17 | **Verified by:** [source]

## Build

- `./gradlew assembleDebug` — build debug APK
- `./gradlew assembleRelease` — build release APK (minification disabled por ahora)
- Usar Gradle wrapper (`./gradlew`), no gradle global

## Static Analysis

- `./gradlew detekt` — Run Detekt static analysis [source]
- `./gradlew detektBaseline` — Regenerar baseline para suprimir issues existentes
- `./gradlew detekt --auto-correct` — Auto-corregir issues corregibles
- Config en `config/detekt/detekt.yml`, baseline en `app/detekt-baseline.xml`
- Compose rules via `io.nlopez.compose.rules:detekt:0.4.2`

## Test

- `./gradlew test` — Unit tests (JUnit 5 + MockK + Turbine + kotlinx.coroutines.test)
- `./gradlew connectedAndroidTest` — Instrumenteded tests (Compose UI test + Espresso)

## CI Pipeline

El workflow en `.github/workflows/ci.yml` corre 3 jobs en paralelo en cada PR a `main` [source]:

| Job | Comando | Propósito |
|---|---|---|
| `detekt` | `./gradlew detekt` | Static analysis |
| `unit-tests` | `./gradlew test` | Tests unitarios |
| `build` | `./gradlew assembleDebug` | Build de debug |

El CI usa Ubuntu latest + JDK 17 (Temurin) + Gradle setup action v4.

## SonarCloud

Workflow en `.github/workflows/sonar.yml` que corre en cada PR y push a `main`:

- Corre `./gradlew test sonar` para análisis estático de código + cobertura
- Usa `SONAR_TOKEN` de secrets para autenticación
- Cachea paquetes de SonarCloud (`~/.sonar/cache`) para acelerar ejecuciones
- `fetch-depth: 0` necesario para blame analysis en Sonar

## Dependencias de test

| Dependencia | Uso |
|---|---|
| JUnit 5 | Test runner |
| MockK | Mocking Kotlin |
| kotlinx.coroutines.test | Coroutine testing |
| Turbine | Flow testing |
| MockWebServer | HTTP mocking |
| Compose UI Test | Screenshot/compose testing |

## ProGuard / R8

Minification deshabilitado en release por ahora. ProGuard rules file: `app/proguard-rules.pro`.

## Gradle config clave

- Java 17, Kotlin 17 toolchain
- Compose compiler via `libs.plugins.compose.compiler`
- KSP para Room + Hilt compilers

## CI avanzado (a futuro)

Patrones observados en proyectos de producción:

| Práctica | Descripción |
|---|---|
| **Release train automático** | Workflow schedule que incrementa versión, genera changelog, crea branch y PR |
| **MobSF security scanning** | Escanea el APK en busca de vulnerabilidades mobile |
| **Dependency diff** | Script bash que compara árbol de dependencias antes/después de cambios |
| **Auto-labeler** | Labels automáticos por equipo/área |
| **Gradle Managed Devices** | Dispositivos preconfigurados para consistencia local/CI |

## Ver también

- [[tools/testing-strategy]] — Testing patterns
- [[tools/detekt-setup]] — Detekt lint