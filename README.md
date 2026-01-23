# CoShift – Shift Management for a Cooperative Supermarket

CoShift is an open-source project.
Its goal is to help a **co-operative supermarket with ~500 members** to plan
and book the mandatory **work shifts** every member performs each
month.  Reduced staffing costs translate directly into lower product prices.

---

## Tech Stack

Backend  
• Java 21 • Spring Boot 3.5 • Spring Security • Maven • JUnit 5 • PostgreSQL

Frontend  
• React 19 • TypeScript 5 • Vite • ESLint

Tooling  
• Git • Docker 

---

## Architecture at a Glance

CoShift follows **Clean / Hexagonal Architecture** with four source packages:

```text
org.coshift
├─ a_domain // business objects (Shift, Person, …)
├─ b_application // use-cases, ports
├─ c_adapters // web, persistence, mapping, …
└─ d_frameworks // Spring wiring, JPA entities, Gson file access
```
Key points  
* Domain and application core are **framework-agnostic**.  


---

## API Overview 

| Method | Path | Description | Auth | Role |
|--------|------|-------------|------|------|
| GET | `/api/shifts` | Returns all shifts as JSON | ✔ | USER |
| GET | `/api/shifts/day?date=YYYY-MM-DD` | Returns public shift details for a day | ✔ | USER |
| POST | `/api/shifts` | Create a shift (`startTime`, `durationInMinutes`, `capacity`) | ✔ | ADMIN |
| PUT | `/api/shifts/{id}` | Update a shift | ✔ | ADMIN |
| DELETE | `/api/shifts/{id}` | Delete a shift | ✔ | ADMIN |
| PUT | `/api/shifts/{id}/participation` | Add person to shift (body: `personId`) | ✔ | USER |
| DELETE | `/api/shifts/{id}/participation` | Remove person from shift (body: `personId`) | ✔ | USER |
| GET | `/api/week` | Returns current week as `DayCellViewModel` list | ✔ | USER |
| GET | `/api/week?count=3` | Returns multiple weeks (default 3) | ✔ | USER |
| POST | `/api/persons` | Create a person (body: `nickname`, `password`, `role?`) | ✔ | USER |
| GET | `/api/persons` | List all persons | ✔ | USER |
| GET | `/api/persons/{id}` | Get person by id | ✔ | USER |
| GET | `/api/persons/{id}/timeaccount` | Get time account for person | ✔ | USER |
| GET | `/api/persons/me` | Get current authenticated person | ✔ | USER |
| GET | `/api/persons/me/timeaccount` | Get current person's time account | ✔ | USER |
| PUT | `/api/persons/{id}/role` | Update person role | ✔ | USER |
| PUT | `/api/persons/{id}` | Update person | ✔ | ADMIN |
| DELETE | `/api/persons/{id}` | Delete person | ✔ | ADMIN |


---

## Build & Run 

Prerequisites  
* Java 21+  
* Maven 3.9+  
* Node 20+ (for the React client)
* PostgreSQL 16+ (DB: `coshifting`, user/password: `coshift`)

1. **Start the backend**

   ```bash
   cd CoShift
   mvn spring-boot:run          # http://localhost:8080
   ```

2. **Start the frontend (in a second terminal)**

   ```bash
   cd CoShift/frontend
   npm install
   npm run dev                  # http://localhost:5173
   ```

## Data Persistence

Person data is persisted in PostgreSQL via JPA. Shifts and time accounts are
still stored as JSON in the `data/` directory via `Gson`.


---

## Roadmap

| Milestone | Details |
|-----------|---------|
| v0.1      | Walking skeleton (login + week view)  ✔ |
| v0.2      | Book / cancel a shift |
| v0.3      | Member time account (balance, transactions) |
| v0.4      | Replace BasicAuth with token (JWT / Session) |
| v0.5      | Mobile-first UI, PWA packaging |
| v1.0      | Switch persistence to relational DB (PostgreSQL) |

---


## License

CoShift is distributed under the Apache License 2.0.