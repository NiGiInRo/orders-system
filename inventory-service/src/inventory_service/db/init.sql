-- Este script lo ejecuta PostgreSQL automáticamente la primera vez que
-- levanta el contenedor, antes de aceptar conexiones.
-- El mecanismo es el directorio /docker-entrypoint-initdb.d/ de la imagen oficial.
-- Solo corre si el volumen está vacío (primer arranque o volumen borrado).

-- Habilita la extensión que permite generar UUIDs dentro de la DB.
-- En Postgres 15+ gen_random_uuid() ya está disponible sin extensión,
-- pero la declaramos explícita para compatibilidad y claridad de intención.
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Crea la tabla que mapea al dominio Product.
-- Cada columna corresponde a un campo del dataclass en product.py.
CREATE TABLE IF NOT EXISTS products (
    -- UUID generado por la DB, no por la aplicación.
    -- Ventaja: la aplicación no necesita conocer el ID antes de persistir.
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- SKU es el identificador de negocio que viaja en los eventos de RabbitMQ.
    -- UNIQUE garantiza que no haya dos productos con el mismo código.
    sku              VARCHAR(50)  NOT NULL UNIQUE,

    name             VARCHAR(255) NOT NULL,

    -- CHECK evita stock negativo a nivel de DB, como segunda línea de defensa.
    -- La primera línea de defensa es la lógica de aplicación (HU-13).
    stock_quantity   INTEGER      NOT NULL CHECK (stock_quantity >= 0)
);

-- Seed de productos de ejemplo.
-- Incluye uno con stock 0 para poder probar el caso de rechazo de orden (HU-13).
INSERT INTO products (sku, name, stock_quantity) VALUES
    ('PROD-001', 'Laptop Lenovo ThinkPad',   10),
    ('PROD-002', 'Mouse Logitech MX Master',  50),
    ('PROD-003', 'Monitor LG 27"',             5),
    ('PROD-004', 'Teclado Mecánico Keychron',  0);  -- sin stock, para probar rechazo