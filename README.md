# DentaFlow

Sistema de turnos online para consultorios odontológicos chicos de Argentina, con una sola sede: reserva online, confirmación por email, agenda sin solapamientos por profesional y por sillón, asistencia y auditoría. Trabajo final de Desarrollo de Software (UTN).

- Dominio y decisiones: [knowledge-base/](knowledge-base/) (empezá por [08 — Arquitectura](knowledge-base/08_arquitectura_propuesta.md) y [09 — Decisiones](knowledge-base/09_decisiones_y_supuestos.md)).
- Roadmap de changes: [CHANGES.md](CHANGES.md). Reparto y protocolo del equipo: [docs/plan-trabajo-equipo.md](docs/plan-trabajo-equipo.md).
- Reglas para agentes y para el equipo: [CLAUDE.md](CLAUDE.md).

## Estructura

```
backend/    API Java 21 + Spring Boot 3 (Maven Wrapper), MySQL 8 + Flyway
frontend/   SPA React + Vite + TypeScript, Tailwind, TanStack Query
knowledge-base/, openspec/, CHANGES.md   documentación y specs
```

## Requisitos

| Herramienta | Versión | Notas |
|---|---|---|
| Java (JDK) | 21 | Por ejemplo Temurin 21. **No hace falta instalar Maven**: se usa `./mvnw`. |
| Node.js | 22 o superior | Incluye npm. |
| MySQL | 8.0, nativo | Instalado como servicio (en Windows, `MySQL80`). **Sin Docker** (DD-09). |

## 1. Base de datos local (una sola vez)

Abrí una consola de MySQL como `root`. En Windows, si `mysql` no está en el `PATH`:

```powershell
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p
```

Creá la base y el usuario de la aplicación (elegí tu propia contraseña):

```sql
CREATE DATABASE dentaflow CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'dentaflow'@'localhost' IDENTIFIED BY 'tu-contraseña';
GRANT ALL PRIVILEGES ON dentaflow.* TO 'dentaflow'@'localhost';
FLUSH PRIVILEGES;
```

Las tablas las crea Flyway al arrancar (`backend/src/main/resources/db/migration/`); nunca se crean a mano.

## 2. Variables de entorno

```bash
cp .env.example .env                    # raíz: backend
cp frontend/.env.example frontend/.env  # frontend: VITE_API_BASE_URL
```

Completá en `.env` al menos `SPRING_DATASOURCE_PASSWORD` con la contraseña del paso 1. Los archivos `.env` están ignorados por Git: **nunca commitees secretos ni datos personales reales** (Ley 25.326). Los perfiles `dev` e `it-mysql` leen el `.env` de la raíz; una variable de entorno definida en la consola tiene prioridad sobre el archivo.

## 3. Levantar el backend (perfil `dev`)

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

En Windows (PowerShell o cmd) usá `.\mvnw.cmd` en lugar de `./mvnw`.

- Estado de salud: <http://localhost:8080/actuator/health> → `{"status":"UP"}`
- Documentación de la API: <http://localhost:8080/swagger-ui.html>

## 4. Levantar el frontend

```bash
cd frontend
npm install
npm run dev
```

Abrí <http://localhost:5173>: muestra la página inicial de DentaFlow.

## 5. Pruebas

| Comando | Qué corre | Necesita |
|---|---|---|
| `cd backend && ./mvnw verify` | Todas las pruebas del backend sobre H2 en memoria (`MODE=MySQL`, perfil `test`), sin las marcadas `@Tag("mysql")` | Nada externo |
| `cd backend && ./mvnw verify -Pit-mysql` | Solo las pruebas `@Tag("mysql")` (conexión, UTC y, desde C-09, concurrencia) contra MySQL 8 real | La base del paso 1 y el `.env` |
| `cd frontend && npm run lint` | oxlint (prohibido `any`) | — |
| `cd frontend && npm test` | Vitest + Testing Library | — |
| `cd frontend && npm run build` | Chequeo de tipos (`strict`) y build de producción | — |

Un test de concurrencia solo cuenta como verificado con `-Pit-mysql` (DD-09). CI (`.github/workflows/ci.yml`) corre los tres jobs —`backend`, `it-mysql` y `frontend`— en cada PR a `main`.

## Flujo de trabajo

Una rama por change de `CHANGES.md` (`feat/C-XX-nombre-corto`), TDD estricto, Conventional Commits en castellano y PR a `main`. El detalle está en [CLAUDE.md](CLAUDE.md) y en [docs/plan-trabajo-equipo.md](docs/plan-trabajo-equipo.md).
