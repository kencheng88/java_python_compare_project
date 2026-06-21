import os

# Database configurations with fallback defaults matching the docker-compose setup
MARIADB_USER = os.getenv("MARIADB_USER", "todouser")
MARIADB_PASSWORD = os.getenv("MARIADB_PASSWORD", "todopassword")
MARIADB_HOST = os.getenv("MARIADB_HOST", "localhost")
MARIADB_PORT = os.getenv("MARIADB_PORT", "3406")
MARIADB_DATABASE = os.getenv("MARIADB_DATABASE", "tododb")

# SQLAlchemy Connection URL for MariaDB using pymysql driver
DATABASE_URL = f"mysql+pymysql://{MARIADB_USER}:{MARIADB_PASSWORD}@{MARIADB_HOST}:{MARIADB_PORT}/{MARIADB_DATABASE}"
