# 🌐 Opportunity Sync Service

The optional cross-device synchronization backend for the **Opportunity** personal task and life management system.

[![Node.js](https://img.shields.io/badge/Node.js-20+-339933?style=flat-square&logo=nodedotjs&logoColor=white)](https://nodejs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.5-3178C6?style=flat-square&logo=typescript&logoColor=white)](https://www.typescriptlang.org/)
[![Express](https://img.shields.io/badge/Framework-Express_4-000000?style=flat-square&logo=express&logoColor=white)](https://expressjs.com/)
[![Prisma](https://img.shields.io/badge/ORM-Prisma_5-2D3748?style=flat-square&logo=prisma&logoColor=white)](https://www.prisma.io/)
[![PostgreSQL](https://img.shields.io/badge/Database-PostgreSQL-4169E1?style=flat-square&logo=postgresql&logoColor=white)](https://www.postgresql.org/)

---

## 📌 Architectural Overview

The Opportunity Android app is strictly **offline-first**: all core workflows, Room persistence, decision engines, and timers operate completely offline without a network connection.

This sync service provides an optional, lightweight cross-device synchronization mechanism for users with multiple Android devices or companion endpoints.

### Sync Protocol
- **Versioned API**: Mounted at `/api/v1` (with backwards-compatible `/auth` and `/sync` aliases).
- **Layered Architecture**:
  - `src/config/env.ts`: Centralized Zod-validated environment configuration.
  - `src/schemas/`: Request validation schemas separated from HTTP handlers.
  - `src/services/`: Business logic and Prisma transactions (`auth.service.ts`, `sync.service.ts`).
  - `src/middleware/`: JWT verification, async error forwarding, 404, and unified JSON error handler.
  - `src/routes/v1/`: Versioned Express routers.
- **Push-then-Pull Flow**:
  1. The client queries Room for modified local entities (`isDirty = 1`).
  2. Client pushes dirty records to `POST /sync/push` with JWT Bearer authentication.
  3. Server updates records and responds with server-assigned mapping.
  4. Client updates local records to `isDirty = 0`.
  5. Client pulls records updated since `lastSyncEpochMillis` via `GET /sync/pull`.
  6. Client applies server updates using **timestamp comparison** (`updatedAtEpochMillis`), ensuring unsaved local edits are never clobbered.

---

## 🚀 API Specification

### Authentication

#### `POST /auth/dev-login`
Exchanges an email address for a JWT access token without password requirement (designed for local prototyping and personal sync).

**Request Body:**
```json
{
  "email": "user@example.com"
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsIn...",
  "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
}
```

---

### Synchronization

All sync endpoints require the `Authorization: Bearer <token>` header.

#### `POST /sync/push`
Pushes modified tasks from client to server.

**Request Body:**
```json
{
  "opportunities": [
    {
      "clientId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
      "title": "Deep work session",
      "type": "TASK",
      "status": "ACTIVE",
      "priorityBucket": "DO_SOON",
      "estimatedDurationMinutes": 45,
      "createdAtEpochMillis": 1726000000000,
      "updatedAtEpochMillis": 1726003600000,
      "deletedLocally": false
    }
  ]
}
```

**Response (200 OK):**
```json
{
  "synced": {
    "f47ac10b-58cc-4372-a567-0e02b2c3d479": "server-record-uuid"
  }
}
```

#### `GET /sync/pull?since=<epochMillis>`
Retrieves all opportunities modified after the given timestamp.

**Response (200 OK):**
```json
{
  "serverTimeEpochMillis": 1726005000000,
  "opportunities": [ ... ]
}
```

---

## 🛠️ Local Development Setup

### 1. Prerequisites
- Node.js 20+
- PostgreSQL database instance running locally or via Docker:
  ```bash
  docker run --name opportunity-postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=opportunity -p 5432:5432 -d postgres:16-alpine
  ```

### 2. Environment Configuration
Copy `.env.example` to `.env`:
```bash
cp .env.example .env
```
Ensure the connection string and secret are set:
```env
DATABASE_URL=postgresql://postgres:postgres@localhost:5432/opportunity
JWT_SECRET=your-random-secure-secret-string-at-least-32-chars
PORT=4000
```

### 3. Install & Migrate
```bash
npm install
npm run prisma:generate
npm run prisma:migrate
```

### 4. Run Development Server
```bash
npm run dev
```

### 5. Production Build
```bash
npm run build
npm start
```
