# Deuda Técnica

Registro de decisiones deliberadas de dejar algo pendiente, con contexto de por qué y cuándo resolverlo.

---

## TD-001 — `__pycache__` commiteado en inventory-service

**Servicio:** `inventory-service`
**Detectado en:** Fase 2
**Prioridad:** Media

### Qué pasó
No existía un `.gitignore` para Python en `inventory-service/` al momento de hacer el primer commit. Los archivos compilados de Python (`__pycache__/`, `*.pyc`) entraron al historial del repo.

### Impacto
- El diff de futuros commits incluye ruido de archivos compilados.
- El repo pesa más de lo necesario.
- No afecta el funcionamiento del sistema.

### Cómo resolverlo
1. Crear `inventory-service/.gitignore` con:
   ```
   __pycache__/
   *.pyc
   *.pyo
   .pytest_cache/
   .coverage
   htmlcov/
   dist/
   *.egg-info/
   ```
2. Remover del tracking sin borrar del disco:
   ```bash
   git rm -r --cached inventory-service/src/inventory_service/**/__pycache__
   git rm -r --cached inventory-service/tests/**/__pycache__
   git commit -m "chore: remove __pycache__ del tracking y agregar .gitignore Python"
   ```

**Cuándo:** antes de Fase 5 (CI/CD) o cuando se retome inventory-service.

---

## TD-002 — Faltan integration tests en inventory-service

**Servicio:** `inventory-service`
**Detectado en:** Fase 2
**Prioridad:** Alta

### Qué pasó
Se implementaron tests de dominio (`test_product.py`) y aplicación (`test_check_stock_service.py`) pero no se hicieron integration tests equivalentes al `OrderControllerIntegrationTest` de `order-service`.

### Impacto
- La capa de persistencia (SQLAlchemy + PostgreSQL real) no está testeada.
- El consumidor de RabbitMQ no está testeado end-to-end.
- La cobertura real del servicio es menor de lo que parece.

### Cómo resolverlo
Agregar tests de integración usando `testcontainers-python` con PostgreSQL real:
- Test del repositorio: verificar que `product_repository_adapter` persiste y consulta correctamente.
- Test del consumidor: verificar que al recibir un `order.created` el stock se descuenta y se publica `inventory.checked`.

**Cuándo:** antes del merge final de `develop → main` en Fase 5, o como HU adicional si se retoma inventory-service.
