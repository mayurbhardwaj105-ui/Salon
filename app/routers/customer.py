"""Protected customer routes. Every endpoint requires a valid auth_token cookie."""
from fastapi import APIRouter, Depends

from ..models import Customer
from ..security import get_current_customer

router = APIRouter(prefix="/api/customer", tags=["customer"])


@router.get("/profile")
def get_profile(customer: Customer = Depends(get_current_customer)):
    return {
        "id": customer.id,
        "fullName": customer.full_name,
        "email": customer.email,
        "phone": customer.phone,
    }
