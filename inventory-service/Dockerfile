# ── Stage 1: builder ──────────────────────────────────────────────────────────
FROM python:3.11-slim AS builder
WORKDIR /app

# creamos el venv en una ruta fija para poder copiarlo al siguiente stage
RUN python -m venv /opt/venv

# activamos el venv para que pip instale ahí y no en el sistema
ENV PATH="/opt/venv/bin:$PATH"

# requirements primero — Docker cachea esta capa
# si el código cambia pero requirements.txt no, no reinstala nada
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt


# ── Stage 2: runtime ──────────────────────────────────────────────────────────
FROM python:3.11-slim AS runtime
WORKDIR /app

# copiamos solo el venv compilado — pip, setuptools y compiladores se descartan
COPY --from=builder /opt/venv /opt/venv

# activamos el venv en el stage de runtime
ENV PATH="/opt/venv/bin:$PATH"

# copiamos el código fuente — va después del venv para aprovechar el cache
COPY src/ ./src/

# equivalente al pythonpath = src de pytest.ini
# sin esto los imports como 'from inventory_service.domain...' fallan en runtime
ENV PYTHONPATH=/app/src

EXPOSE 8082

# uvicorn como entrypoint — no hay uvicorn.run() en el código
# --host 0.0.0.0 necesario dentro de Docker para aceptar conexiones externas al contenedor
ENTRYPOINT ["uvicorn", "inventory_service.main:app", "--host", "0.0.0.0", "--port", "8082"]