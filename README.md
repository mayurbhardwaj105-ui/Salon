# Salon Customer Registration & JWT Authentication System

FastAPI + MySQL backend for a salon management platform's customer registration and login.
On login it issues a signed JSON Web Token (JWT) and stores it in an HTTP-only cookie, so
sessions work across requests without any server-side session state.

**Tech stack:** Python, FastAPI, PyJWT, SQLAlchemy, MySQL

## How it works

1. **Register** (`POST /api/auth/register`) — validates the input, creates a customer record,
   and stores a bcrypt hash of the password, never the password itself. Duplicate emails
   return `409`.
2. **Login** (`POST /api/auth/login`) — verifies credentials, generates a signed JWT (HS256)
   containing the customer's email and id, and sets it as an `HttpOnly`, `Secure`,
   `SameSite=Strict` cookie named `auth_token`. The token is never returned in the JSON body,
   which keeps it out of reach of JavaScript (mitigates XSS token theft).
3. **Protected routes** (anything under `/api/customer/**`) — a FastAPI dependency
   (`get_current_customer`) reads the `auth_token` cookie, verifies the JWT signature and
   expiry, loads the customer, and rejects the request with `401` if the token is missing,
   invalid, or expired.
4. **Logout** (`POST /api/auth/logout`) — clears the cookie.

## Project structure

```
app/
├── main.py              # FastAPI app, creates tables on startup
├── config.py            # settings from environment variables
├── database.py          # SQLAlchemy engine/session
├── models.py            # Customer table
├── schemas.py           # request validation (email, password length, etc.)
├── security.py          # bcrypt, JWT create/verify, cookie auth dependency
└── routers/
    ├── auth.py          # register / login / logout
    └── customer.py      # example protected endpoint
tests/test_auth.py       # registration, login, cookie and protected-route tests
```

## Setup

1. Create a MySQL database (for example `CREATE DATABASE salon_db;`). The `customers` table is
   created automatically on startup.
2. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```
3. Copy `.env.example` to `.env`, then set your MySQL credentials in `DATABASE_URL` and a long
   random `JWT_SECRET` (the app refuses to issue tokens without one):
   ```bash
   python -c "import secrets; print(secrets.token_urlsafe(48))"
   ```
4. Run:
   ```bash
   uvicorn app.main:app --reload --port 8080
   ```
   Interactive API docs are at http://localhost:8080/docs.

## Try it

```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Priya Sharma","email":"priya@example.com","phone":"9999999999","password":"secret123"}'

# Login (saves the cookie to cookies.txt)
curl -c cookies.txt -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"priya@example.com","password":"secret123"}'

# Access a protected route using the saved cookie
curl -b cookies.txt http://localhost:8080/api/customer/profile
```

## Tests

```bash
pytest
```

The tests use a temporary SQLite database, so MySQL isn't needed to run them.

## Notes

- The cookie is `Secure`, so it is only sent over HTTPS. For plain `http://localhost` testing
  set `COOKIE_SECURE=false` in `.env` (curl and browsers otherwise won't send it back).
- `JWT_SECRET` and database credentials come from environment variables, so keep `.env` out of
  version control (it is in `.gitignore`).
