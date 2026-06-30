# ADR-001 — Uso de Monorepo para el Sistema de Gestión de Órdenes

## Estado
Aceptado

## Contexto
El sistema está compuesto por cuatro microservicios en tres lenguajes distintos
(Java/Spring Boot, Python/FastAPI, Node.js/Express) que colaboran a través de
RabbitMQ. El desarrollo lo lleva una sola persona, con el objetivo de construir
un proyecto de portafolio que demuestre prácticas reales de arquitectura
distribuida, CI/CD y orquestación.

Existían dos alternativas para organizar el código:
1. **Polyrepo**: un repositorio independiente por servicio.
2. **Monorepo**: un único repositorio que contiene los cuatro servicios, la
   infraestructura local (`docker-compose.yml`) y la documentación de
   arquitectura.

Para un equipo de un solo desarrollador, el polyrepo introduce overhead de
coordinación (cuatro repos, cuatro pipelines independientes, sincronización de
versiones de contratos de eventos) sin un beneficio claro a esta escala.

## Decisión
Se adopta un **monorepo** con la siguiente estructura, donde cada servicio
mantiene su independencia interna (su propio `Dockerfile`, dependencias,
README y pipeline de build dentro del CI compartido):

```
order-system/
├── docker-compose.yml
├── .github/workflows/ci.yml
├── docs/architecture/
├── api-gateway/
├── order-service/
├── inventory-service/
└── notification-service/
```

El monorepo no implica que los servicios compartan código de producción ni
que se desplieguen juntos — cada uno conserva un Dockerfile propio y se
construye, prueba y versiona de forma independiente dentro del mismo pipeline
de CI (`.github/workflows/ci.yml`), que crece de forma incremental por fase.

## Consecuencias

**Positivas:**
- Un solo `docker-compose up` levanta el sistema completo, útil para demos y
  para validar el flujo end-to-end sin saltar entre repos.
- Los cambios que cruzan varios servicios (por ejemplo, cambiar el contrato de
  un evento de RabbitMQ) se revisan en un solo PR, con contexto completo.
- La documentación de arquitectura (ADRs, diagramas C4) vive en un solo lugar
  y referencia directamente el código de los servicios que describe.
- Más simple de mantener para un solo desarrollador: un solo `git clone`, un
  solo historial de commits, una sola configuración de branch protection.

**Negativas / trade-offs:**
- El pipeline de CI debe filtrar qué servicio construir en cada push (o
  construir todos), lo que añade algo de complejidad a `ci.yml` a medida que
  se agregan servicios.
- En un equipo real más grande, un monorepo con lenguajes tan distintos
  dificultaría dar permisos granulares por servicio y podría generar
  cuellos de botella en revisiones de PR si no se delimitan bien los
  `CODEOWNERS`.
- Si el proyecto creciera a producción real con equipos separados por
  servicio, esta decisión se reevaluaría a favor de polyrepo con versionado
  de contratos de eventos (esquemas compartidos vía paquete o registry).

**Decisión de revisión:** si en fases avanzadas (Fase 5 o 6) el pipeline de CI
se vuelve difícil de mantener por la mezcla de lenguajes, se evaluará separar
en repos independientes con un repositorio adicional solo para la
documentación de arquitectura.
