"""Registration, login and logout."""
from fastapi import APIRouter, Depends, HTTPException, Response, status
from sqlalchemy.orm import Session

from .. import config
from ..database import get_db
from ..models import Customer
from ..schemas import LoginRequest, RegisterRequest
from ..security import create_access_token, hash_password, verify_password

router = APIRouter(prefix="/api/auth", tags=["auth"])


@router.post("/register", status_code=status.HTTP_201_CREATED)
def register(req: RegisterRequest, db: Session = Depends(get_db)):
    email = req.email.lower()
    if db.query(Customer).filter(Customer.email == email).first():
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="An account with this email already exists",
        )
    customer = Customer(
        full_name=req.full_name,
        email=email,
        phone=req.phone,
        password_hash=hash_password(req.password),
    )
    db.add(customer)
    db.commit()
    db.refresh(customer)
    return {"message": "Registration successful", "customerId": customer.id, "email": customer.email}


@router.post("/login")
def login(req: LoginRequest, response: Response, db: Session = Depends(get_db)):
    customer = db.query(Customer).filter(Customer.email == req.email.lower()).first()
    if customer is None or not verify_password(req.password, customer.password_hash):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid email or password",
        )
    token = create_access_token(customer.email, customer.id)
    # The token is only ever delivered in the cookie, never in the JSON body.
    # HttpOnly keeps it away from JavaScript (mitigates XSS token theft).
    response.set_cookie(
        key=config.COOKIE_NAME,
        value=token,
        max_age=config.JWT_EXPIRATION_SECONDS,
        path="/",
        httponly=True,
        secure=config.COOKIE_SECURE,
        samesite="strict",
    )
    return {"message": "Login successful", "customerId": customer.id, "fullName": customer.full_name}


@router.post("/logout")
def logout(response: Response):
    response.delete_cookie(
        key=config.COOKIE_NAME,
        path="/",
        httponly=True,
        secure=config.COOKIE_SECURE,
        samesite="strict",
    )
    return {"message": "Logged out successfully"}
