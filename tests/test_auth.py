import os

os.environ["DATABASE_URL"] = "sqlite:///./test_salon.db"
os.environ["JWT_SECRET"] = "test-secret-test-secret-test-secret-123456"
os.environ["COOKIE_SECURE"] = "false"

import pytest
from fastapi.testclient import TestClient

from app.database import Base, engine
from app.main import app

USER = {"fullName": "Priya Sharma", "email": "priya@example.com", "phone": "9999999999", "password": "secret123"}


@pytest.fixture()
def client():
    Base.metadata.drop_all(bind=engine)
    with TestClient(app) as c:
        yield c
    Base.metadata.drop_all(bind=engine)


def test_register_then_duplicate_is_conflict(client):
    r = client.post("/api/auth/register", json=USER)
    assert r.status_code == 201 and r.json()["email"] == USER["email"]
    assert client.post("/api/auth/register", json=USER).status_code == 409


def test_register_validation(client):
    assert client.post("/api/auth/register", json={**USER, "password": "123"}).status_code == 422
    assert client.post("/api/auth/register", json={**USER, "email": "nope"}).status_code == 422


def test_login_sets_httponly_cookie_and_not_in_body(client):
    client.post("/api/auth/register", json=USER)
    r = client.post("/api/auth/login", json={"email": USER["email"], "password": USER["password"]})
    assert r.status_code == 200
    assert "token" not in r.text.lower()
    cookie = r.headers["set-cookie"].lower()
    assert "auth_token=" in cookie and "httponly" in cookie and "samesite=strict" in cookie


def test_wrong_password_is_unauthorized(client):
    client.post("/api/auth/register", json=USER)
    r = client.post("/api/auth/login", json={"email": USER["email"], "password": "wrong-pass"})
    assert r.status_code == 401


def test_protected_route_requires_valid_cookie(client):
    assert client.get("/api/customer/profile").status_code == 401
    client.cookies.set("auth_token", "garbage")
    assert client.get("/api/customer/profile").status_code == 401
    client.cookies.clear()

    client.post("/api/auth/register", json=USER)
    client.post("/api/auth/login", json={"email": USER["email"], "password": USER["password"]})
    r = client.get("/api/customer/profile")
    assert r.status_code == 200 and r.json()["email"] == USER["email"]


def test_logout_clears_cookie(client):
    client.post("/api/auth/register", json=USER)
    client.post("/api/auth/login", json={"email": USER["email"], "password": USER["password"]})
    assert client.post("/api/auth/logout").status_code == 200
    client.cookies.clear()
    assert client.get("/api/customer/profile").status_code == 401
