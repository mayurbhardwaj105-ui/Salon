# Salon Customer Registration & JWT Authentication System

Spring Boot + MySQL backend for a salon's customer registration and login,
using JSON Web Tokens (JWT) stored in an HTTP-only cookie for session
management.

**Tech stack:** Java, Spring Boot, JWT, MySQL

## How it works

1. **Register** (`POST /api/auth/register`) — creates a customer record;
   the password is hashed with BCrypt before it's ever stored.
2. **Login** (`POST /api/auth/login`) — verifies credentials, generates a
   signed JWT (HS256) containing the customer's email + id, and sets it
   as an `HttpOnly`, `Secure`, `SameSite=Strict` cookie named
   `auth_token`. The token is never returned in the JSON body, only the
   cookie — this keeps it out of reach of JavaScript (mitigates XSS
   token theft).
3. **Protected routes** (anything under `/api/customer/**`) — a
   `HandlerInterceptor` (`JwtAuthInterceptor`) runs before the
   controller, reads the `auth_token` cookie, validates the JWT
   signature + expiry, and rejects the request with `401` if it's
   missing or invalid. If valid, it attaches the customer's email/id to
   the request so controllers can use it.
4. **Logout** (`POST /api/auth/logout`) — clears the cookie.

## Project structure

```
src/main/java/com/salon/auth/
├── SalonAuthApplication.java     # entry point
├── model/Customer.java           # JPA entity
├── repository/CustomerRepository.java
├── dto/RegisterRequest.java      # + validation annotations
├── dto/LoginRequest.java
├── security/JwtUtil.java         # token generation/validation (jjwt)
├── security/JwtAuthInterceptor.java   # reads/validates the cookie
├── config/WebConfig.java         # wires the interceptor to /api/customer/**
├── controller/AuthController.java     # register/login/logout
└── controller/CustomerController.java # example protected endpoint
```

## Setup

1. Create a MySQL database (or let `createDatabaseIfNotExist=true` in
   `application.properties` do it for you).
2. Update `src/main/resources/application.properties` with your MySQL
   credentials.
3. **Important:** change `jwt.secret` to a long, random value before
   deploying anywhere real — never use the placeholder in production.
4. Run:
   ```bash
   mvn spring-boot:run
   ```

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

## Notes

- `cookie.setSecure(true)` means the cookie is only sent over HTTPS. Set
  it to `false` temporarily if you're testing over plain `http://localhost`
  and your browser/client refuses to store it.
- This project skips the full `spring-security` filter chain on purpose
  (just `spring-security-crypto` for BCrypt) to keep the JWT/cookie flow
  easy to follow. For a production system, consider migrating to a proper
  `SecurityFilterChain` with role-based access control as the app grows.
