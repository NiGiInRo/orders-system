-- Este script lo ejecuta PostgreSQL automáticamente la primera vez que
-- levanta el contenedor, antes de aceptar conexiones.
-- Solo corre si el volumen está vacío (primer arranque o volumen borrado).

-- Habilita la extensión para generar UUIDs dentro de la DB.
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Crea la tabla que mapea al dominio Product.
CREATE TABLE IF NOT EXISTS products (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sku              VARCHAR(50)  NOT NULL UNIQUE,
    name             VARCHAR(255) NOT NULL,
    stock_quantity   INTEGER      NOT NULL CHECK (stock_quantity >= 0)
);

-- Seed de productos de ejemplo.
-- PROD-004 tiene stock 0 para probar el caso de rechazo de orden.
INSERT INTO products (sku, name, stock_quantity) VALUES
    ('PROD-001', 'Laptop Lenovo ThinkPad',    10),
    ('PROD-002', 'Mouse Logitech MX Master',  50),
    ('PROD-003', 'Monitor LG 27 pulgadas',     5),
    ('PROD-004', 'Teclado Mecanico Keychron',  0);
