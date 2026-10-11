"""Application settings, read from environment variables (or a local .env file)."""
import os

from dotenv import load_dotenv

load_dotenv()


def _get_bool(name: str, default: bool) -> bool:
    return os.getenv(name, str(default)).strip().lower() in {"1", "true", "yes"}


DATABASE_URL = os.getenv(
    "DATABASE_URL",
    "mysql+pymysql://root:your_mysql_password@localhost:3306/salon_db",
)

# Must be set to a long random value (32+ chars). There is deliberately no default.
JWT_SECRET = os.getenv("JWT_SECRET", "")
JWT_ALGORITHM = "HS256"
JWT_EXPIRATION_SECONDS = int(os.getenv("JWT_EXPIRATION_SECONDS", "3600"))

COOKIE_NAME = "auth_token"
# Secure cookies are only sent over HTTPS. Set COOKIE_SECURE=false for plain-http local testing.
COOKIE_SECURE = _get_bool("COOKIE_SECURE", True)
