"""Password hashing, JWT creation/verification and the cookie-based auth dependency."""
from datetime import datetime, timedelta, timezone

import bcrypt
import jwt
from fastapi import Cookie, Depends, HTTPException, status
from sqlalchemy.orm import Session

from . import config
from .database import get_db
from .models import Customer


def hash_password(password: str) -> str:
    return bcrypt.hashpw(password.encode("utf-8"), bcrypt.gensalt()).decode("utf-8")


def verify_password(password: str, password_hash: str) -> bool:
    try:
        return bcrypt.checkpw(password.encode("utf-8"), password_hash.encode("utf-8"))
    except ValueError:
        return False


def create_access_token(email: str, customer_id: int) -> str:
    if len(config.JWT_SECRET) < 32:
        raise RuntimeError("JWT_SECRET must be set to a random value of at least 32 characters")
    now = datetime.now(timezone.utc)
    payload = {
        "sub": email,
        "customerId": customer_id,
        "iat": now,
        "exp": now + timedelta(seconds=config.JWT_EXPIRATION_SECONDS),
    }
    return jwt.encode(payload, config.JWT_SECRET, algorithm=config.JWT_ALGORITHM)


def decode_access_token(token: str) -> dict:
    """Verify signature and expiry; raises jwt.PyJWTError if the token is invalid."""
    return jwt.decode(token, config.JWT_SECRET, algorithms=[config.JWT_ALGORITHM])


def get_current_customer(
    auth_token: str | None = Cookie(default=None, alias=config.COOKIE_NAME),
    db: Session = Depends(get_db),
) -> Customer:
    """Dependency for protected routes: reads the JWT from the cookie and loads the customer."""
    unauthorized = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Missing or invalid authentication token",
    )
    if not auth_token:
        raise unauthorized
    try:
        claims = decode_access_token(auth_token)
    except jwt.PyJWTError:
        raise unauthorized
    customer = db.query(Customer).filter(Customer.email == claims.get("sub")).first()
    if customer is None:
        raise unauthorized
    return customer
