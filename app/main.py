"""Salon customer registration and JWT authentication API.

Run with:  uvicorn app.main:app --reload --port 8080
"""
from contextlib import asynccontextmanager

from fastapi import FastAPI

from .database import Base, engine
from .routers import auth, customer


@asynccontextmanager
async def lifespan(_: FastAPI):
    Base.metadata.create_all(bind=engine)  # creates the customers table if it doesn't exist
    yield


app = FastAPI(title="Salon Customer Auth API", lifespan=lifespan)
app.include_router(auth.router)
app.include_router(customer.router)
