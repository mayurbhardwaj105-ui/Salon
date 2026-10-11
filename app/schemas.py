"""Request validation models."""
from pydantic import BaseModel, EmailStr, Field, field_validator


class RegisterRequest(BaseModel):
    full_name: str = Field(alias="fullName", min_length=1)
    email: EmailStr
    phone: str = Field(min_length=1)
    password: str = Field(min_length=6, description="At least 6 characters")

    model_config = {"populate_by_name": True}

    @field_validator("full_name", "phone")
    @classmethod
    def not_blank(cls, value: str) -> str:
        value = value.strip()
        if not value:
            raise ValueError("must not be blank")
        return value


class LoginRequest(BaseModel):
    email: EmailStr
    password: str = Field(min_length=1)
