# ADR-002 — Estrategia de Branching y Pipeline CI/CD

## Estado
Aceptado

## Contexto
El proyecto requiere una estrategia de control de versiones y automatización que permita
desarrollar múltiples servicios en paralelo, mantener la calidad del código en cada
integración, y generar releases trazables sin intervención manual. El equipo es de un
solo desarrollador, pero la estrategia debe reflejar prácticas reales de equipos
profesionales dado el objetivo del proyecto (portafolio para roles de Tech Lead).

Se necesita decidir:
1. Qué modelo de branching adoptar.
2. Cuándo y cómo corre el pipeline de CI.
3. Qué herramientas de calidad y automatización incluir, y en qué orden.

## Decisión

### Branching: GitFlow simplificado

Se adopta un modelo GitFlow con tres niveles de ramas:

```
main          ← rama de producción, solo recibe merges desde develop
develop       ← rama de integración, recibe merges desde ramas de feature
feat/hu-XX    ← ramas de trabajo, una por HU o conjunto de HUs relacionadas
```

Las ramas de feature se desprenden de `develop` y se integran a `develop` vía PR.
El merge de `develop` a `main` marca el fin de una fase completa.

### Pipeline CI: construcción incremental

El pipeline no se construye de una sola vez en Fase 5 — crece junto con el proyecto.
El archivo `.github/workflows/ci.yml` existe desde Fase 1 y se expande en momentos
definidos:

| Momento | Qué se agrega |
|---------|---------------|
| Fase 1 — scaffolding | Compilación de `order-service` (`./mvnw verify -DskipTests`) |
| Fase 1 — unit tests listos | Se quita `-DskipTests`; se agrega Jacoco con umbral de cobertura |
| Fase 1 — Dockerfile listo | `docker build` para verificar imagen (sin push) |
| Fase 2 terminada | Compilación + tests de `inventory-service` |
| Fase 3 terminada | Build de `notification-service` |
| Fase 4 terminada | Build de `api-gateway` |
| Fase 5 | Push a GHCR, Semantic Release, paralelización de jobs |

### Triggers del pipeline

El CI corre en los siguientes eventos:
- `push` a `main` o `develop`
- `pull_request` hacia `main` o `develop`

Esto garantiza que el gate de calidad aplica en el momento del PR, antes de que el
código entre a una rama protegida.

### Herramientas de calidad adoptadas

| Herramienta | Propósito | Cuándo se agrega |
|-------------|-----------|-----------------|
| **Jacoco** | Cobertura de tests en `order-service` | Al tener unit tests escritos |
| **GHCR** | Publicar imágenes Docker oficiales | Solo desde `main`, en Fase 5 |
| **Semantic Release** | Versionado automático y changelog | Primer merge `develop → main` |

### Separación build vs. publish

- `docker build` corre en cualquier rama para verificar que el Dockerfile es válido.
- `docker push` a GHCR corre **únicamente desde `main`** para evitar sobrescribir
  imágenes oficiales con código no validado.

### Branch protection rules (configuradas en GitHub)

- `main`: requiere PR, requiere CI verde, bloquea push directo.
- `develop`: requiere PR, requiere CI verde.

## Consecuencias

**Positivas:**
- Ningún código llega a `main` sin haber pasado CI en `develop` primero.
- El historial de `main` refleja únicamente releases completos y funcionales.
- El changelog y los tags de versión se generan sin intervención manual.
- La cobertura de tests es visible y trazable en cada PR.

**Negativas / trade-offs:**
- Para un solo desarrollador, el flujo de PR puede sentirse burocrático en ramas
  pequeñas. El beneficio es la práctica del proceso real de equipo.
- Semantic Release requiere disciplina en los mensajes de commit — un commit con
  formato incorrecto no se refleja en el changelog.
- Si el umbral de Jacoco es muy alto desde el inicio, puede bloquear merges legítimos.
  Se empieza en 70% y se sube gradualmente.

**Decisión de revisión:** si en fases avanzadas el flujo GitFlow genera demasiado
overhead, se puede evaluar migrar a trunk-based development con feature flags.
