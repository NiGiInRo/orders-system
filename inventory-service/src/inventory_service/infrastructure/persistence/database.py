import os

from sqlalchemy import create_engine
from sqlalchemy.orm import DeclarativeBase, sessionmaker

# variables de entorno con defaults para desarrollo local
_host = os.environ.get("POSTGRES_HOST", "localhost")
_port = os.environ.get("POSTGRES_PORT", "5432")
_db   = os.environ.get("POSTGRES_DB",   "inventory_db")
_user = os.environ.get("POSTGRES_USER", "inventory_user")
_pass = os.environ.get("POSTGRES_PASSWORD", "inventory_pass")

DATABASE_URL = f"postgresql://{_user}:{_pass}@{_host}:{_port}/{_db}"

# engine es la conexión al pool de la DB — se crea una sola vez al arrancar
engine = create_engine(DATABASE_URL)

# SessionLocal es la fábrica de sesiones — cada operación de DB abre una sesión nueva
SessionLocal = sessionmaker(bind=engine)


# Base es la clase padre de todos los modelos SQLAlchemy de este servicio
class Base(DeclarativeBase):
    pass