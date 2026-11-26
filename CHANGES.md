# CHANGES — Secuencia de Implementación

> Índice canónico de todos los changes del proyecto **DentaFlow** (agenda online de turnos para consultorios odontológicos chicos de una sola sede, Argentina).
> Cada change es atómico: un agente puede implementarlo en una sesión (~4-6 horas).
> **Leer este archivo antes de empezar un item.**
>
> **Stack (DD-08 + DD-09, 2026-11-24):** Java 21 + Spring Boot 3 (Maven), **MySQL 8 nativo + Flyway**, tests sobre **H2 `MODE=MySQL`** y concurrencia contra MySQL real (perfil `it-mysql`), **sin Docker ni Testcontainers**; React + Vite + TypeScript; monorepo `backend/` + `frontend/`; hosting gratuito Render (API) y Vercel (frontend), proveedor de MySQL a elegir en C-19 (PA-14). Anti-solapamiento: bloqueo pesimista del profesional y después del sillón dentro de la transacción (DD-09).
>
> **Plazo (DD-10, PA-01 resuelta):** entrega el **jueves 2026-11-26 a las 12:00**. Los changes marcados "sí" en la columna `Corte 26/11` forman la entrega; el resto sigue en el roadmap. Si el corte no llega completo, la prioridad es C-09 + C-10.

---

## Cómo usar este documento

1. Identificar el item a implementar y verificar que sus dependencias figuran completas en este roadmap.
2. Leer los docs de la knowledge-base indicados en "Leer antes".
3. Acordar el alcance y los criterios de aceptación del item (si tiene "Bloqueado por", resolver primero esa pregunta abierta con el equipo; no asumirla).
4. Implementar, verificar los criterios acordados y registrar la evidencia.
5. Marcar el checkbox `[x]` en este archivo al completar la verificación técnica.

Conservá los IDs de las fichas al editar o reordenar. Los items "post-MVP" sin ID (ficha clínica, odontograma, cobros, etc.) están fuera de este roadmap a propósito (ver al final).

---

## Árbol de dependencias

```
C-01 decision-stack-y-plazo                 ← compuerta (stack hecho, DD-08; resta PA-01)
  └── C-02 scaffolding-proyecto
        └── C-03 modelo-recursos-consultorio
              └── C-04 auth-usuarios-roles           ← CRITICO: desbloquea paneles internos
                    │
                    ├── C-05 gestion-recursos-admin
                    ├── C-06 horarios-y-bloqueos
                    ├── C-07 parametros-consultorio
                    └── C-08 auditoria-cambios
                          └── C-09 modelo-turno-paciente  ← + C-03 (integridad anti-solapamiento)
                                │
                                ├── C-10 motor-disponibilidad      ← + C-06
                                │     ├── C-12 carga-manual-sobreturno  ← + C-11
                                │     └── C-16 reserva-online-publica   ← + C-14 + C-15
                                │           └── C-17 confirmar-cancelar-reprogramar ← + C-07 + C-14
                                ├── C-11 agenda-interna-vistas
                                │     └── C-13 asistencia-y-registro-cambios  ← + C-08
                                ├── C-14 email-transaccional-y-enlaces
                                │     └── C-18 recordatorios-programados
                                └── C-15 privacidad-ley-25326       ← + C-07

C-19 cierre-mvp-e2e-y-despliegue  ← C-12 + C-13 + C-17 + C-18

Diferenciadores (post-MVP núcleo):
C-20 lista-de-espera        ← C-10 + C-17
C-21 whatsapp-prellenado    ← C-11
C-22 reintento-confirmacion ← C-18
C-23 panel-ausentismo       ← C-13
C-24 manual-publico         ← C-19
```

### Tabla de changes (responsable, oleada y dependencias)

> Plan de trabajo del equipo acordado el 2026-11-24. Detalle, protocolo Git + OpenSpec y comandos de arranque: [docs/plan-trabajo-equipo.md](docs/plan-trabajo-equipo.md).
> **Regla de arranque**: un change empieza cuando **todos** sus "Depende de" están mergeados en `main`; la oleada es la ventana prevista, no una espera a que termine toda la oleada anterior. Máximo **1 change activo por persona**.
> **Revisor**: Florencia aprueba todos los PR; los PR de Florencia los aprueba Bruno (GitHub no permite aprobar el PR propio).

| ID | Change | Depende de | Oleada | Responsable | Revisor | Gov. | Tam. | Corte 26/11 | Estado |
|----|--------|------------|--------|-------------|---------|------|------|-------------|--------|
| C-01 | decision-stack-y-plazo | — | 0 | Florencia (+ Bruno: GitHub/CI) | Bruno | MEDIO | S | sí | `[x]` |
| C-02 | scaffolding-proyecto | C-01 | 0 | Florencia (+ Bruno: GitHub/CI) | Bruno | BAJO | M | sí | `[x]` |
| C-03 | modelo-recursos-consultorio | C-02 | 1 | Renzo | Florencia | CRITICO | M | sí | `[x]` |
| C-04 | auth-usuarios-roles | C-03 | 2 | Bruno | Florencia | CRITICO | L | sí | `[x]` |
| C-05 | gestion-recursos-admin | C-03, C-04 | 3 | Florencia | Bruno | BAJO | M | no | `[x]` |
| C-06 | horarios-y-bloqueos | C-03, C-04 | 3 | Jessica | Florencia | MEDIO | M | sí | `[x]` |
| C-07 | parametros-consultorio | C-04 | 3 | Renzo | Florencia | BAJO | S | sí | `[x]` |
| C-08 | auditoria-cambios | C-04 | 3 | Bruno | Florencia | CRITICO | M | sí | `[x]` |
| C-09 | modelo-turno-paciente | C-03, C-08 | 4 | Renzo | Florencia | CRITICO | L | sí | `[x]` |
| C-10 | motor-disponibilidad | C-06, C-09 | 5 | Renzo | Florencia | CRITICO | L | sí | `[x]` |
| C-11 | agenda-interna-vistas | C-04, C-09 | 5 | Florencia | Bruno | MEDIO | L | no | `[x]` |
| C-14 | email-transaccional-y-enlaces | C-02, C-09 | 5 | Jessica | Florencia | ALTO | M | sí | `[x]` |
| C-15 | privacidad-ley-25326 | C-07, C-09 | 5 | Bruno | Florencia | ALTO | M | sí | `[x]` |
| C-12 | carga-manual-sobreturno | C-10, C-11 | 6 | Renzo | Florencia | CRITICO | M | no | `[ ]` |
| C-13 | asistencia-y-registro-cambios | C-08, C-11 | 6 | Florencia | Bruno | MEDIO | M | no | `[x]` |
| C-16 | reserva-online-publica | C-10, C-14, C-15 | 6 | Jessica | Florencia | ALTO | L | sí | `[x]` |
| C-18 | recordatorios-programados | C-09, C-14 | 6 | Bruno | Florencia | ALTO | M | no | `[ ]` |
| C-17 | confirmar-cancelar-reprogramar | C-07, C-14, C-16 | 7 | Jessica | Florencia | ALTO | M | sí | `[x]` |
| C-22 | reintento-confirmacion [DIF] | C-18 | 7 | Bruno | Florencia | MEDIO | S | no | `[ ]` |
| C-23 | panel-ausentismo [DIF] | C-13 | 7 | Florencia | Bruno | BAJO | M | no | `[x]` |
| C-19 | cierre-mvp-e2e-y-despliegue | C-12, C-13, C-17, C-18 (en el corte 26/11: solo C-17 y los demás changes del corte) | 8 | Bruno (E2E de cada flujo: su autor) | Florencia | MEDIO | M | sí | `[ ]` |
| C-20 | lista-de-espera [DIF] | C-10, C-17 | 8 | Renzo | Florencia | MEDIO | M | no | `[ ]` |
| C-21 | whatsapp-prellenado [DIF] | C-11 | 8 | Jessica | Florencia | BAJO | S | no | `[x]` |
| C-24 | manual-publico [DIF] | C-19 | 9 | Jessica | Florencia | BAJO | S | no | `[ ]` |

### Paralelismo por oleada (1 change activo por persona)

```
Oleada | Arranca cuando            | Florencia        | Bruno            | Jessica          | Renzo
-------+---------------------------+------------------+------------------+------------------+------------------
  0    | —                         | C-01 → C-02      | apoyo GitHub/CI  | (preparación)    | (preparación)
  1    | C-02 ✓                    | revisa C-03      | (preparación)    | (preparación)    | C-03
  2    | C-03 ✓                    | revisa C-04      | C-04             | (preparación)    | (preparación)
  3    | C-04 ✓          ← FORK x4 | C-05             | C-08             | C-06             | C-07
  4    | C-08 ✓                    | revisa C-09      | (preparación)    | (preparación)    | C-09
  5    | C-09 ✓          ← FORK x4 | C-11             | C-15             | C-14             | C-10
  6    | C-10/11/14/15 ✓ ← FORK x4 | C-13             | C-18             | C-16             | C-12
  7    | C-13/16/18 ✓    ← FORK x3 | C-23 [DIF]       | C-22 [DIF]       | C-17             | revisa C-17 / E2E
  8    | C-17 ✓ (+12,13,18)        | revisión final   | C-19             | C-21 [DIF]       | C-20 [DIF]
  9    | C-19 ✓                    | —                | —                | C-24 [DIF]       | —
```

- Las oleadas 0, 1, 2 y 4 tienen **un solo change** porque el bloqueo es real (C-01 → C-02 → C-03 → C-04 y C-08 → C-09). Quien no tiene change activo hace preparación sin código de producción (ver `docs/plan-trabajo-equipo.md`).
- [DIF] = diferenciador opcional. Si PA-01 recorta el plazo, se omite y su responsable apoya el cierre del MVP; el MVP no depende de ninguno.

### Camino crítico (10 changes — mínimo irreducible)

```
C-01 → C-02 → C-03 → C-04 → C-08 → C-09 → C-10 → C-16 → C-17 → C-19
Flor   Flor   Renzo  Bruno  Bruno  Renzo  Renzo  Jess.  Jess.  Bruno
```

(C-12 y C-18 también convergen en C-19 pero no alargan el camino.)

### Reparto del equipo (4 integrantes)

> Tamaño estimado: S = 1, M = 2, L = 3 puntos (ajustar al planificar cada change).

| Integrante | Área | Changes propios | Puntos | Aprueba PR de |
|------------|------|-----------------|--------|---------------|
| **Florencia** (líder) | Base del proyecto y frontend de paneles internos | C-01, C-02, C-05, C-11, C-13, C-23 | 12 | Todos los changes de Bruno, Jessica y Renzo |
| **Bruno** | Plataforma, seguridad, auditoría y tareas programadas | C-04, C-08, C-15, C-18, C-22, C-19 | 12 | Los changes de Florencia |
| **Jessica** | Configuración de agenda, notificaciones y paciente (reserva pública) | C-06, C-14, C-16, C-17, C-21, C-24 | 11 | — (revisión de apoyo opcional) |
| **Renzo** | Núcleo de turnos (backend) | C-03, C-07, C-09, C-10, C-12, C-20 | 13 | — (revisión de apoyo opcional) |

Criterios del reparto:

- **Por área**: cada integrante concentra una zona del código (Renzo `dominio/turnos` y `dominio/disponibilidad`; Bruno `infraestructura/seguridad`, `dominio/auditoria` e `infraestructura/scheduler`; Jessica `infraestructura/email` y `frontend/src/features/reserva`; Florencia `frontend/src/features/agenda`) y hay continuidad: C-09 → C-10 → C-12 (Renzo), C-14 → C-16 → C-17 (Jessica), C-18 → C-22 (Bruno), C-11 → C-13 → C-23 (Florencia).
- **Paralelismo**: en cada fork (oleadas 3, 5, 6 y 7) cada integrante tiene un change independiente, sin dos changes activos de la misma persona.
- **Camino crítico repartido**: ningún integrante concentra más de 3 changes del camino crítico, así una demora no frena a todos.

---

## FASE 0 — Decisión y base del proyecto

> Secuencial y corto. C-01 no es código: es la decisión que destraba todo el roadmap.

### [C-01] `decision-stack-y-plazo`
- **Estado**: `[x]` completado el 2026-11-24 (DD-09, DD-10; PA-01, PA-03, PA-05, PA-06 y PA-10 resueltas)
- **Scope**:
  - ~~Decidir y registrar el stack~~ **Hecho**: DD-08 (2026-11-23) y DD-09 (2026-11-24, base de datos y entorno sin Docker) en `knowledge-base/09_decisiones_y_supuestos.md`; arquitectura, migraciones y pruebas en `knowledge-base/08_arquitectura_propuesta.md`. PA-02 resuelta.
  - **Hecho**: plazo y corte de entrega (PA-01, DD-10).
  - **Hecho**: PA-03, PA-05, PA-06 y PA-10 resueltas el 2026-11-24 (ver `knowledge-base/10_preguntas_abiertas.md`).
  - **Hecho**: `CLAUDE.md` y `.atl/skill-registry.md` regenerados con el stack DD-09.
  - Entregable: documento de decisión; sin código.
- **Dependencias**: ninguna
- **Governance**: MEDIO
- **Responsable**: Florencia (apoyo de Bruno en GitHub y CI)
- **Oleada**: 0
- **Revisor (aprueba el PR)**: Bruno
- **Tamaño estimado**: S
- **Zona**: `knowledge-base/`, `docs/`
- **Bloqueado por**: — (PA-01 resuelta en este change, DD-10)
- **Leer antes**:
  - `knowledge-base/10_preguntas_abiertas.md` §Preguntas abiertas
  - `knowledge-base/08_arquitectura_propuesta.md`
  - `knowledge-base/09_decisiones_y_supuestos.md`
  - `knowledge-base/01_vision_y_objetivos.md`

### [C-02] `scaffolding-proyecto`
- **Estado**: `[x]` completado el 2026-11-24 (Spring Boot 3.5.16; jobs de CI `backend`, `it-mysql` y `frontend` en verde en el PR; evidencia en `openspec/changes/archive/2026-11-24-c-02-scaffolding-proyecto/evidencia/`)
- **Scope**:
  - Estructura del monorepo (DD-08): `backend/` (Maven, Spring Boot 3, paquetes `ar.dentaflow.{dominio,aplicacion,infraestructura,presentacion}`) y `frontend/` (Vite + React + TypeScript), según `08_arquitectura_propuesta.md`.
  - Configuración por variables de entorno: `SPRING_DATASOURCE_URL`/`_USERNAME`/`_PASSWORD`, `SPRING_MAIL_HOST`/`_PORT`/`_USERNAME`/`_PASSWORD`, `APP_MAIL_FROM`, `APP_BASE_URL`, `AUTH_SECRET`, `TZ=America/Argentina/Buenos_Aires` (SU-09), `VITE_API_BASE_URL`; `.env.example`, sin secretos en el repo.
  - Nota de stack (DD-09): Maven + Vite; MySQL 8 nativo + Flyway (`flyway-mysql`, `mysql-connector-j`); perfil `test` con H2 `MODE=MySQL;DATABASE_TO_LOWER=TRUE`; perfil `it-mysql` para tests `@Tag("mysql")` contra MySQL real; sin Docker ni Testcontainers; CI con GitHub Actions (backend, job `it-mysql` con el MySQL preinstalado del runner, frontend); Vitest + Testing Library en frontend; OpenAPI con springdoc-openapi.
  - Runner de tests, lint, CI mínimo.
  - Utilidad de zona horaria del consultorio (todos los horarios se interpretan en esa zona).
  - Tests: arranque de la app (perfil `test`, H2), conexión a MySQL (perfil `it-mysql`), test de humo de CI.
- **Dependencias**: `C-01`
- **Governance**: BAJO
- **Responsable**: Florencia (apoyo de Bruno en GitHub y CI)
- **Oleada**: 0
- **Revisor (aprueba el PR)**: Bruno
- **Tamaño estimado**: M
- **Zona**: `backend/pom.xml`, `backend/src/main/resources/`, `frontend/`, `.github/workflows/`
- **Leer antes**:
  - `knowledge-base/08_arquitectura_propuesta.md` §Patrones, §Estructura de directorios, §Variables de entorno
  - `knowledge-base/09_decisiones_y_supuestos.md` §SU-09

---

## FASE 1 — Núcleo: recursos, acceso y auditoría

> Tras C-04 se abre el primer fork (C-05, C-06, C-07, C-08 en paralelo).

### [C-03] `modelo-recursos-consultorio`
- **Estado**: `[x]` completado el 2026-11-25 (H2 108/108, it-mysql 16/16, seed dev 3/2/5; evidencia en `openspec/changes/archive/2026-11-25-c-03-modelo-recursos-consultorio/evidencia/`)
- **Scope**:
  - Modelos: Profesional, Sillon, Prestacion (con `duracion_minutos`), relación N:M Profesional-Prestacion.
  - Migración `V003_1__crear_recursos_consultorio.sql` con restricciones (activo/inactivo, duración > 0); repositorios base.
  - Seed de desarrollo/demo: 2-3 profesionales, 2-3 sillones, 4-5 prestaciones con duraciones distintas.
  - Tests: invariantes de modelo, duración obligatoria, desactivar sin borrar.
- **Dependencias**: `C-02`
- **Governance**: CRITICO (modelos core que todo lo demás referencia)
- **Responsable**: Renzo
- **Oleada**: 1
- **Revisor (aprueba el PR)**: Florencia
- **Tamaño estimado**: M
- **Zona**: `backend/src/main/java/ar/dentaflow/dominio/recursos/`, `backend/src/main/resources/db/migration/`
- **Leer antes**:
  - `knowledge-base/04_modelo_de_datos.md` §Profesional, §Sillon, §Prestacion, §Seed data inicial
  - `knowledge-base/05_reglas_de_negocio.md` §RN-TU-03
  - `knowledge-base/08_arquitectura_propuesta.md` §Patrones

### [C-04] `auth-usuarios-roles`
- **Estado**: `[x]` completado el 2026-11-25 (JWT Bearer, DD-11; H2 226/226, it-mysql 25/25, frontend 52 tests; evidencia en `openspec/changes/archive/2026-11-25-c-04-auth-usuarios-roles/evidencia/`)
- **Scope**:
  - Modelos: Usuario (email, hash de credencial, activo, `profesional_id` opcional), Rol (`administrador`, `recepcion`, `profesional`).
  - Login/logout del personal con Spring Security; **decidir cookie de sesión vs JWT**: front (Vercel) y API (Render) están en dominios distintos, lo que complica SameSite/CORS (zona CRITICA, decisión explícita de este change, no de C-01); hash de credencial; rate limiting de login.
  - Autorización RBAC verificada en el backend (no solo en la UI) según la matriz de permisos; guardas reutilizables por rol.
  - Gestión de usuarios y roles por el administrador: crear, desactivar, asignar rol (US-003).
  - Seed: usuario administrador inicial (credencial desde variable de entorno, nunca hardcodeada).
  - Tests: acceso denegado por rol, endpoint protegido sin sesión, usuario desactivado no ingresa.
- **Dependencias**: `C-03`
- **Governance**: CRITICO
- **Responsable**: Bruno
- **Oleada**: 2
- **Revisor (aprueba el PR)**: Florencia
- **Tamaño estimado**: L
- **Zona**: `backend/src/main/java/ar/dentaflow/infraestructura/seguridad/`, `backend/src/main/java/ar/dentaflow/dominio/usuarios/`, `frontend/src/features/auth/`
- **Bloqueado por**: PA-09 (un usuario con múltiples roles: asumir un solo rol hasta decidir)
- **Leer antes**:
  - `knowledge-base/03_actores_y_roles.md` §RBAC — Matriz de permisos
  - `knowledge-base/05_reglas_de_negocio.md` §RN-AU-02
  - `knowledge-base/08_arquitectura_propuesta.md` §Seguridad
  - `knowledge-base/06_funcionalidades.md` §US-003

### [C-05] `gestion-recursos-admin`
- **Estado**: `[x]` completado el 2026-11-26 (PR #15; H2 418/418, it-mysql 56/56, frontend 96 tests; evidencia en `openspec/changes/archive/2026-11-26-c-05-gestion-recursos-admin/evidencia/`)
- **Scope**:
  - Endpoints/pantallas de administrador para alta, edición y desactivación de profesionales, sillones y prestaciones; asociación de prestaciones por profesional (US-001).
  - Validación de input en backend; solo rol administrador (lectura para recepción y profesional según matriz).
  - Tests: CRUD, permisos por rol, duración obligatoria.
- **Dependencias**: `C-03, C-04`
- **Governance**: BAJO
- **Responsable**: Florencia
- **Oleada**: 3
- **Revisor (aprueba el PR)**: Bruno
- **Tamaño estimado**: M
- **Zona**: `backend/src/main/java/ar/dentaflow/aplicacion/recursos/`, `backend/src/main/java/ar/dentaflow/presentacion/`, `frontend/src/features/agenda/`
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` §US-001
  - `knowledge-base/03_actores_y_roles.md` §RBAC
  - `knowledge-base/07_flujos_principales.md` §Flujo 7

### [C-06] `horarios-y-bloqueos`
- **Estado**: `[x]` completado el 2026-11-26 (migración `V006_1__crear_horarios_y_bloqueos.sql`, H2 460/460, MySQL 80/80, verificación dev; evidencia en `openspec/changes/c-06-horarios-y-bloqueos/evidencia/`)
- **Scope**:
  - Modelos: HorarioAtencion (profesional, día de semana, hora_inicio, hora_fin; sin rangos superpuestos por profesional y día), Bloqueo (ámbito profesional | sillón | consultorio, inicio, fin, motivo).
  - Migración 002; alta/edición de horarios y bloqueos por el administrador (US-002).
  - Servicio de consulta "¿está disponible este recurso en este rango?" (horario − bloqueos), reutilizable por el motor de disponibilidad.
  - Aviso al crear un bloqueo que afecta turnos existentes (comportamiento exacto abierto, PA-11; cablear el aviso real cuando exista C-09).
  - Tests: horarios superpuestos rechazados, bloqueo por cada ámbito, zona horaria.
- **Dependencias**: `C-03, C-04`
- **Governance**: MEDIO
- **Responsable**: Jessica
- **Oleada**: 3
- **Revisor (aprueba el PR)**: Florencia
- **Tamaño estimado**: M
- **Zona**: `backend/src/main/java/ar/dentaflow/dominio/disponibilidad/`, `backend/src/main/resources/db/migration/`
- **Bloqueado por**: nada. PA-11 resuelta (DD-12): el bloqueo no cancela turnos existentes; avisa e impide reservas nuevas
- **Leer antes**:
  - `knowledge-base/04_modelo_de_datos.md` §HorarioAtencion, §Bloqueo
  - `knowledge-base/05_reglas_de_negocio.md` §RN-TU-04, §RN-GL-01
  - `knowledge-base/06_funcionalidades.md` §US-002
  - `knowledge-base/07_flujos_principales.md` §Flujo 7

### [C-07] `parametros-consultorio`
- **Estado**: `[x]` completado el 2026-11-25 (H2 280/280, MySQL 43/43; evidencia en `openspec/changes/archive/2026-11-25-c-07-parametros-consultorio/evidencia/`)
- **Scope**:
  - Modelo de configuración clave-valor: anticipación mínima de cancelación/reprogramación, textos del aviso de privacidad (US-004).
  - Edición solo por administrador; el valor por defecto se carga desde seed y se marca como provisorio.
  - Tests: lectura/edición de parámetros, permisos, validación de rango.
- **Dependencias**: `C-04`
- **Governance**: BAJO
- **Responsable**: Renzo
- **Oleada**: 3
- **Revisor (aprueba el PR)**: Florencia
- **Tamaño estimado**: S
- **Zona**: `backend/src/main/java/ar/dentaflow/aplicacion/configuracion/`, `backend/src/main/resources/db/migration/`
- **Bloqueado por**: PA-04 (valor por defecto de anticipación mínima; usar un valor provisorio configurable)
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` §US-004
  - `knowledge-base/05_reglas_de_negocio.md` §RN-CA-01, §RN-CA-02
  - `knowledge-base/04_modelo_de_datos.md` §Seed data inicial

### [C-08] `auditoria-cambios`
- **Estado**: `[x]` completado
- **Scope**:
  - Mecanismo de auditoría por evento reutilizable: modelo TurnoHistorial (turno_id, actor usuario/paciente/sistema, fecha-hora, acción, valores anterior/nuevo), solo-inserción (sin edición ni borrado).
  - Migración 003; servicio `registrar_cambio` invocable dentro de la misma transacción que el cambio auditado (RN-AU-01).
  - Tests: el registro se escribe junto al cambio, se revierte si el cambio falla, no es modificable.
- **Dependencias**: `C-04`
- **Governance**: CRITICO (audit trail)
- **Responsable**: Bruno
- **Oleada**: 3
- **Revisor (aprueba el PR)**: Florencia
- **Tamaño estimado**: M
- **Zona**: `backend/src/main/java/ar/dentaflow/dominio/auditoria/`, `backend/src/main/resources/db/migration/`
- **Leer antes**:
  - `knowledge-base/04_modelo_de_datos.md` §TurnoHistorial
  - `knowledge-base/05_reglas_de_negocio.md` §RN-AU-01, §RN-SO-02
  - `knowledge-base/08_arquitectura_propuesta.md` §Patrones (Auditoría por evento)

---

## FASE 2 — Agenda: el modelo de turno y el motor de disponibilidad

> Corazón del diferencial (DD-02). C-09 y C-10 son CRITICOS: ninguna regla de solapamiento puede vivir solo en la UI.

### [C-09] `modelo-turno-paciente`
- **Estado**: `[x]` completado el 2026-11-26 (H2 641/641, it-mysql 135/135; evidencia en `openspec/changes/archive/2026-11-26-c-09-modelo-turno-paciente/evidencia/`)
- **Scope**:
  - Modelos: Paciente (nombre, apellido, DNI, teléfono, email, fecha de consentimiento), Turno (paciente, profesional, sillón, prestación, inicio, fin = inicio + duración, estado, origen online|recepción, `es_sobreturno`, creado_por, token de acceso del paciente).
  - Migración `V009_1__...` (Flyway, MySQL 8 + H2 `MODE=MySQL`); índices (profesional_id, estado, inicio, fin), (sillon_id, estado, inicio, fin), (paciente_id), (estado, inicio).
  - Anti-solapamiento (DD-09): sin solapamiento de [inicio, fin) por profesional y por sillón entre turnos activos (no cancelados), salvo `es_sobreturno` (RN-TU-01/02/07), garantizado con `SELECT ... FOR UPDATE` sobre la fila del profesional y después la del sillón, y verificación leyendo el último estado confirmado (`READ COMMITTED` o `FOR SHARE`).
  - Estado inicial del turno online: `pendiente` (PA-10).
  - Máquina de estados: `pendiente`, `confirmado`, `cancelado`, `asistio`, `ausente`, `ausente_con_aviso`, con transiciones válidas explícitas; cancelar libera profesional y sillón (RN-TU-06).
  - Servicio de creación/cambio de turno transaccional (`@Transactional`), única vía de escritura de turnos, que toma los bloqueos en orden, devuelve conflicto si hay solapamiento e invoca la auditoría (C-08). Tests de concurrencia `@Tag("mysql")` en el perfil `it-mysql`.
  - Deduplicación de paciente por DNI (SU-03) y token no adivinable por turno.
  - Tests: solapamiento rechazado por profesional y por sillón, concurrencia (dos creaciones simultáneas, solo una gana), transiciones inválidas, sobreturno permitido solo con flag.
- **Dependencias**: `C-03, C-08`
- **Governance**: CRITICO
- **Responsable**: Renzo
- **Oleada**: 4
- **Revisor (aprueba el PR)**: Florencia
- **Tamaño estimado**: L
- **Zona**: `backend/src/main/java/ar/dentaflow/dominio/turnos/`, `backend/src/main/java/ar/dentaflow/dominio/pacientes/`, `backend/src/main/resources/db/migration/`
- **Bloqueado por**: nada. PA-13 y SU-03 resueltas (DD-12): email obligatorio, DNI obligatorio y único. PA-10 y PA-06 resueltas: nace `pendiente`; sillón asignado por el sistema después del profesional
- **Leer antes**:
  - `knowledge-base/04_modelo_de_datos.md` §Turno, §Paciente
  - `knowledge-base/05_reglas_de_negocio.md` §RN-TU-01..07, §RN-GL-02
  - `knowledge-base/08_arquitectura_propuesta.md` §Patrones (concurrencia, integridad en BD, máquina de estados)
  - `knowledge-base/09_decisiones_y_supuestos.md` §DD-02, §DD-04, §SU-03

### [C-10] `motor-disponibilidad`
- **Estado**: `[x]` completado el 2026-11-26 (H2 958/958, it-mysql verificado por Renzo; evidencia en `openspec/changes/archive/2026-11-26-c-10-motor-disponibilidad/evidencia/`)
- **Scope**:
  - Motor único de disponibilidad: dado prestación (y profesional si aplica) y rango de fechas, calcula huecos = horario de atención − bloqueos − turnos activos de profesional y sillón, solo huecos donde entra la duración completa (RN-DI-01).
  - Asignación de profesional y sillón al hueco elegido; verificación final dentro de la transacción de creación (reusa C-09) para que el hueco tomado en el medio se rechace.
  - Reutilizado por reserva online, carga manual, reprogramación y lista de espera (sin reglas duplicadas).
  - Nota de stack: la verificación final se apoya en el servicio transaccional con bloqueo de C-09 (DD-09); el motor no la reemplaza. Para "cualquier profesional" entrega candidatos ordenados y la creación prueba uno por transacción.
  - Tests: hueco que no entra la duración, bloqueo parcial, borde exacto de horario, sillón ocupado con profesional libre, zona horaria/cambio de día.
- **Dependencias**: `C-06, C-09`
- **Governance**: CRITICO
- **Responsable**: Renzo
- **Oleada**: 5
- **Revisor (aprueba el PR)**: Florencia
- **Tamaño estimado**: L
- **Zona**: `backend/src/main/java/ar/dentaflow/dominio/disponibilidad/`, `backend/src/main/java/ar/dentaflow/aplicacion/agenda/`
- **Bloqueado por**: — (PA-05 y PA-06 resueltas: profesional opcional o "cualquiera"; primero el profesional y después el primer sillón libre, asignado por el sistema)
- **Leer antes**:
  - `knowledge-base/05_reglas_de_negocio.md` §RN-DI-01, §RN-DI-02, §RN-DI-04, §RN-TU-07
  - `knowledge-base/07_flujos_principales.md` §Flujo 1 (pasos 2 y 4)
  - `knowledge-base/08_arquitectura_propuesta.md` §Patrones (Motor de disponibilidad único)
  - `knowledge-base/10_preguntas_abiertas.md` §PA-05, §PA-06

### [C-11] `agenda-interna-vistas`
- **Estado**: `[x]` completado el 2026-11-26 (H2 834 casos, it-mysql 174/174, frontend 167 tests; `GET /api/agenda/turnos` y `/panel/agenda`; evidencia en `openspec/changes/archive/2026-11-26-c-11-agenda-interna-vistas/evidencia/`)
- **Scope**:
  - Vistas diaria y semanal de la agenda para recepción/administrador, filtrables por profesional y por sillón, mostrando el estado de cada turno (US-020).
  - Vista "mi agenda del día" del profesional: solo ve sus turnos, verificado en backend (US-023).
  - Endpoints de lectura paginados/por rango; diseño responsive del panel.
  - Tests: filtros, aislamiento por rol (profesional no ve turnos ajenos), orden y estados.
- **Dependencias**: `C-04, C-09`
- **Governance**: MEDIO
- **Responsable**: Florencia
- **Oleada**: 5
- **Revisor (aprueba el PR)**: Bruno
- **Tamaño estimado**: L
- **Zona**: `frontend/src/features/agenda/`, `backend/src/main/java/ar/dentaflow/aplicacion/agenda/`
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` §US-020, §US-023
  - `knowledge-base/03_actores_y_roles.md` §RBAC
  - `knowledge-base/07_flujos_principales.md` §Flujo 5

### [C-12] `carga-manual-sobreturno`
- **Estado**: `[x]` completado el 2026-11-26 (PR #31; responsable final: Bruno). Incluye `POST /api/turnos`, `POST /api/turnos/sobreturnos`, `POST /api/pacientes/busqueda` y la pantalla `/panel/agenda/nuevo-turno`. Tests: H2 1039, it-mysql 212, frontend 249. La spec principal es `openspec/specs/carga-manual-turnos/`. Archivado en `openspec/changes/archive/2026-11-26-c-12-carga-manual-sobreturno/`. **Pendiente**: la tarea 7.2 (prueba punta a punta contra el backend real); por ahora la cubren solo los tests MockMvc sobre H2.
- **Scope**:
  - Carga de turno por recepción: buscar/crear paciente (DNI, teléfono, email obligatorio según DD-12/PA-13), elegir prestación, profesional, sillón y horario; rechaza conflictos de profesional/sillón (no es una alerta), respeta horarios y bloqueos (US-021; la restricción de horario y bloqueos `RestriccionDeHorarioYBloqueos` ya queda activa en `ServicioDeTurnos` desde C-10).
  - Sobreturno explícito: acción separada, solo recepción/administrador, marca `es_sobreturno`, registra quién y cuándo; inexistente en el flujo online (US-022, RN-SO-01/02).
  - Ante conflicto, sugerir la alternativa más cercana usando C-10.
  - Tests: conflicto rechazado, sobreturno permitido solo con permiso y auditado, profesional no puede cargar sobreturno.
- **Dependencias**: `C-10, C-11`
- **Governance**: CRITICO
- **Responsable**: Renzo
- **Oleada**: 6
- **Revisor (aprueba el PR)**: Florencia
- **Tamaño estimado**: M
- **Zona**: `backend/src/main/java/ar/dentaflow/aplicacion/turnos/`, `frontend/src/features/agenda/`
- **Leer antes**:
  - `knowledge-base/07_flujos_principales.md` §Flujo 4
  - `knowledge-base/06_funcionalidades.md` §US-021, §US-022
  - `knowledge-base/05_reglas_de_negocio.md` §RN-SO-01, §RN-SO-02, §RN-GL-02
  - `knowledge-base/09_decisiones_y_supuestos.md` §DD-03

### [C-13] `asistencia-y-registro-cambios`
- **Estado**: `[x]` completado el 2026-11-26 — **US-024** (`POST /api/turnos/{id}/asistencia` y acciones en `/panel/agenda`, archivado en `openspec/changes/archive/2026-11-26-c-13-registro-asistencia-turno/`) y **US-025** (`GET /api/turnos/{id}/historial` solo administrador y diálogo de historial en `/panel/agenda`; H2 993/993, it-mysql 207/207, frontend 185 tests; archivado en `openspec/changes/archive/2026-11-26-c-13-registro-cambios-turno/`).
- **Scope**:
  - Marcar turno como `asistio`, `ausente` o `ausente_con_aviso` por profesional (propios) o recepción, con auditoría (US-024).
  - Pantalla de consulta del registro de cambios por turno (quién, cuándo, qué cambió), solo administrador (US-025).
  - Tests: transiciones de asistencia válidas/inválidas, permisos por rol, registro visible y no editable.
- **Dependencias**: `C-08, C-11`
- **Governance**: MEDIO
- **Responsable**: Florencia
- **Oleada**: 6
- **Revisor (aprueba el PR)**: Bruno
- **Tamaño estimado**: M
- **Zona**: `backend/src/main/java/ar/dentaflow/aplicacion/turnos/`, `frontend/src/features/agenda/`
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` §US-024, §US-025
  - `knowledge-base/07_flujos_principales.md` §Flujo 5
  - `knowledge-base/05_reglas_de_negocio.md` §RN-TU-05, §RN-AU-01

---

## FASE 3 — Paciente: privacidad, email y reserva pública

> C-14 y C-15 pueden ir en paralelo con C-10/C-11. Todo lo público requiere validación estricta de input y anti-abuso.

### [C-14] `email-transaccional-y-enlaces`
- **Estado**: `[x]` completado el 2026-11-26 (migración `V014_1__crear_notificacion.sql`, servicio transaccional y enlaces con token; archivado en `openspec/changes/archive/2026-11-26-c-14-email-transaccional-y-enlaces/`)
- **Scope**:
  - Modelo Notificacion (turno_id, tipo recordatorio|confirmación|reintento, canal email, enviada_en, estado de envío); Migración 005.
  - Adaptador de email transaccional detrás de una interfaz (sin acoplar el dominio al proveedor); plantillas de confirmación de reserva.
  - Nota de stack: Spring Mail con SMTP genérico (`SPRING_MAIL_*`, `APP_MAIL_FROM`); elegir en este change el proveedor gratuito (Brevo o Resend); en desarrollo, adaptador simulado o la cuenta de prueba del proveedor (sin contenedores).
  - Generación y validación de enlaces con token no adivinable para confirmar, cancelar y reprogramar (SU-03), armados con `APP_BASE_URL`; manejo de enlace vencido o turno ya cancelado.
  - Reintento y registro del estado ante fallo de envío.
  - Tests: envío con proveedor simulado, token inválido/vencido, fallo de envío registrado.
- **Dependencias**: `C-02, C-09`
- **Governance**: ALTO (datos personales en canal de notificación)
- **Responsable**: Jessica
- **Oleada**: 5
- **Revisor (aprueba el PR)**: Florencia
- **Tamaño estimado**: M
- **Zona**: `backend/src/main/java/ar/dentaflow/infraestructura/email/`, `backend/src/main/java/ar/dentaflow/dominio/notificaciones/`, `backend/src/main/resources/db/migration/`
- **Bloqueado por**: nada. PA-13 resuelta (DD-12); SMTP estándar por `.env` (Brevo o Gmail App Password), simulado en tests
- **Leer antes**:
  - `knowledge-base/04_modelo_de_datos.md` §Notificacion
  - `knowledge-base/05_reglas_de_negocio.md` §RN-NO-01, §RN-NO-02
  - `knowledge-base/07_flujos_principales.md` §Flujo 2 (casos de error)
  - `knowledge-base/08_arquitectura_propuesta.md` §Seguridad, §Variables de entorno

### [C-15] `privacidad-ley-25326`
- **Estado**: `[x]` completo (2026-11-26, PR #16; archivado en `openspec/changes/archive/2026-11-26-c-15-privacidad-ley-25326/`). La revisión del componente a 390 px y con teclado pasa a C-16
- **Scope**:
  - Aviso de privacidad (texto editable desde parámetros) visible en la reserva, con aceptación obligatoria; registro de la fecha de consentimiento en Paciente (US-040).
  - Minimización de datos: solo DNI, nombre, teléfono, email; sin campos clínicos (RN-PR-03).
  - Acceso a datos personales restringido por rol; transporte solo por HTTPS; no registrar datos personales en logs.
  - Anonimización/baja de los datos personales del paciente por endpoint protegido; retención de 5 años (PA-12, DD-12).
  - Tests: reserva sin aceptación rechazada, fecha de consentimiento guardada, recurso protegido por rol.
- **Dependencias**: `C-09, C-07`
- **Governance**: ALTO (datos personales, Ley 25.326)
- **Responsable**: Bruno
- **Oleada**: 5
- **Revisor (aprueba el PR)**: Florencia
- **Tamaño estimado**: M
- **Zona**: `backend/src/main/java/ar/dentaflow/dominio/pacientes/`, `frontend/src/features/reserva/`, `backend/src/main/java/ar/dentaflow/infraestructura/seguridad/`
- **Bloqueado por**: revisión legal del texto del aviso (el texto es editable en parámetros). PA-12 resuelta (DD-12): aceptación obligatoria con instante, endpoint de anonimización/baja, retención 5 años
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` §US-040
  - `knowledge-base/05_reglas_de_negocio.md` §RN-PR-01..03
  - `knowledge-base/08_arquitectura_propuesta.md` §Seguridad
  - `knowledge-base/09_decisiones_y_supuestos.md` §DD-07, §SU-07

### [C-16] `reserva-online-publica`
- **Estado**: `[x]` completo (2026-11-26, PR #29; archivado en `openspec/changes/archive/2026-11-26-c-16-reserva-online-publica/`). Specs sincronizadas: `reserva-online` (nueva) y `parametros-consultorio` (catálogo con `contacto-consultorio`)
- **Scope**:
  - Flujo público mobile-first sin registro: elegir prestación (y profesional según PA-05) → ver solo huecos donde entra la duración completa → ingresar DNI, teléfono, email → aceptar aviso (US-010).
  - Creación de turno en una única transacción que revalida disponibilidad, asigna profesional y sillón, crea el turno con estado inicial parametrizable, registra historial y dispara el email de confirmación con enlaces.
  - Manejo de hueco tomado entre selección y confirmación: informar y ofrecer otros huecos; mensaje con contacto si no hay huecos.
  - Nunca permite solapamientos ni sobreturno (RN-DI-02); validación de input en backend; rate limiting/captcha a evaluar contra abuso.
  - Tests: reserva exitosa, dos reservas simultáneas del mismo hueco (una falla), aviso no aceptado, datos inválidos, sin huecos.
- **Dependencias**: `C-10, C-14, C-15`
- **Governance**: ALTO
- **Responsable**: Jessica
- **Oleada**: 6
- **Revisor (aprueba el PR)**: Florencia
- **Tamaño estimado**: L
- **Zona**: `frontend/src/features/reserva/`, `backend/src/main/java/ar/dentaflow/aplicacion/turnos/`, `backend/src/main/java/ar/dentaflow/presentacion/` (rutas públicas)
- **Bloqueado por**: nada. PA-13 resuelta (DD-12). PA-05, PA-06 y PA-10 resueltas (ver C-09 y C-10)
- **Leer antes**:
  - `knowledge-base/07_flujos_principales.md` §Flujo 1
  - `knowledge-base/06_funcionalidades.md` §US-010
  - `knowledge-base/05_reglas_de_negocio.md` §RN-DI-01..04
  - `knowledge-base/08_arquitectura_propuesta.md` §Seguridad (anti-abuso)
  - `knowledge-base/03_actores_y_roles.md` §Rutas públicas

### [C-17] `confirmar-cancelar-reprogramar`
- **Estado**: `[x]` completo (2026-11-26, PR #34; archivado en `openspec/changes/archive/2026-11-26-c-17-confirmar-cancelar-reprogramar/`)
- **Scope**:
  - Enlace de confirmación de un toque: `pendiente` → `confirmado`, reflejado en la agenda (US-011).
  - Cancelación por el paciente hasta la anticipación mínima configurada; libera profesional y sillón; auditada (US-012, RN-CA-01/02).
  - Reprogramación: solo huecos válidos del motor (C-10), cambio transaccional que valida solapamientos, auditado.
  - **Contrato C-10 para reprogramar** (C-10 no implementó su grupo opcional 8): usar `MotorDeDisponibilidad.buscarHuecos(prestacionId, profesionalId, desde, hasta, turnoAExcluir = turnoId)` para obtener los huecos válidos excluyendo el turno propio, y confirmar cada candidato con `ServicioDeTurnos.reprogramar`. El 409 de hueco tomado llega como `HuecoNoDisponibleException` / `TurnoSolapadoException`.
  - Fuera de la ventana: mensaje indicando contactar al consultorio (RN-CA-03); enlace vencido o turno cancelado: mensaje explicativo.
  - Tests: dentro/fuera de ventana, hueco nuevo tomado, token ajeno rechazado, doble confirmación idempotente.
- **Dependencias**: `C-16, C-07, C-14`
- **Governance**: ALTO
- **Responsable**: Jessica
- **Oleada**: 7
- **Revisor (aprueba el PR)**: Florencia
- **Tamaño estimado**: M
- **Zona**: `frontend/src/features/reserva/`, `backend/src/main/java/ar/dentaflow/aplicacion/turnos/`
- **Bloqueado por**: nada. PA-04 resuelta (DD-12): 24 h por defecto, ajustable a 0 h para la demo (requiere ampliar el rango de C-07, hoy 1..168)
- **Leer antes**:
  - `knowledge-base/07_flujos_principales.md` §Flujo 2, §Flujo 3
  - `knowledge-base/06_funcionalidades.md` §US-011, §US-012
  - `knowledge-base/05_reglas_de_negocio.md` §RN-CA-01..03, §RN-NO-02

### [C-18] `recordatorios-programados`
- **Estado**: `[ ]` pendiente
- **Scope**:
  - Tarea programada que selecciona turnos próximos en estado `pendiente` y envía el email recordatorio con enlace de confirmación (RN-NO-01).
  - Idempotencia: no enviar dos veces el mismo recordatorio; registro en Notificacion.
  - Antelación configurable (parámetro); soporte para uno o varios recordatorios.
  - Nota de stack (decidir en este change): los planes gratuitos de hosting suspenden el servidor por inactividad, por lo que un `@Scheduled` en proceso puede no dispararse. Propuesta: el envío es una operación idempotente (sin emails duplicados) expuesta como endpoint protegido y disparada por un cron externo gratuito; `@Scheduled` solo en desarrollo local.
  - Tests: selección por ventana, no duplicados, turno cancelado/confirmado no se recuerda, falla de envío registrada, endpoint protegido rechaza llamadas sin credencial.
- **Dependencias**: `C-14, C-09`
- **Governance**: ALTO
- **Responsable**: Bruno
- **Oleada**: 6
- **Revisor (aprueba el PR)**: Florencia
- **Tamaño estimado**: M
- **Zona**: `backend/src/main/java/ar/dentaflow/infraestructura/scheduler/`, `backend/src/main/java/ar/dentaflow/dominio/notificaciones/`
- **Bloqueado por**: PA-07 (cuánto antes y cuántos recordatorios; dejar parametrizable)
- **Leer antes**:
  - `knowledge-base/07_flujos_principales.md` §Flujo 2
  - `knowledge-base/05_reglas_de_negocio.md` §RN-NO-01
  - `knowledge-base/08_arquitectura_propuesta.md` §Patrones (Tareas programadas)

---

## FASE 4 — Cierre del MVP

### [C-19] `cierre-mvp-e2e-y-despliegue`
- **Estado**: `[ ]` parcial el 2026-11-26.
  - **Hecho**: bloque A (perfil `prod`, Dockerfile para Render según DD-13, `vercel.json`, espera del arranque en frío, esqueleto Playwright y job `e2e` en CI), plan B ensayado y revisión de seguridad.
  - **Decisión de las 11:00**: se presenta con el plan B local (`evidencia/decision-1100.md`).
  - **Pendiente**: despliegue en Aiven, Render y Vercel (tareas 3.2, 3.3 y 3.5); E2E del bloque B (6.x; C-16 y C-17 ya están en `main`); cron de C-18 (7.1).
  - No marcar `[x]` hasta desplegar y completar los E2E.
- **Scope**:
  - Tests end-to-end con Playwright de los flujos principales (reserva, recordatorio/confirmación, cancelación/reprogramación, carga manual con sobreturno, asistencia, configuración).
  - Despliegue (planes gratuitos): API en Render, frontend en Vercel, MySQL 8 en el proveedor que se elija en este change (PA-14); configurar CORS/cookies entre dominios según lo decidido en C-04 y el cron externo de C-18 (si C-18 entra).
  - Corte 26/11 (DD-10): E2E y despliegue solo de los flujos incluidos en el corte (reserva, confirmación, cancelación y reprogramación).
  - Prueba de concurrencia explícita contra la garantía de no solapamiento (reserva online + carga manual simultáneas).
  - Revisión de seguridad: RBAC en backend, rutas públicas, rate limiting, secretos en variables de entorno, HTTPS, sin datos personales en logs.
  - Seed de demo, despliegue a un entorno accesible, guía de puesta en marcha en el README.
  - Validación con al menos un consultorio o demo real para contrastar SU-01 y SU-02.
- **Dependencias**: `C-12, C-13, C-17, C-18`
- **Governance**: MEDIO
- **Responsable**: Bruno (aportan E2E todos los autores)
- **Oleada**: 8
- **Revisor (aprueba el PR)**: Florencia
- **Tamaño estimado**: M
- **Zona**: `frontend/` (Playwright), `.github/workflows/`, configuración de despliegue, documentación
- **Bloqueado por**: nada. PA-14 resuelta (DD-12): Aiven MySQL gratuito; plan B demo local completa. PA-01 resuelta (DD-10)
- **Leer antes**:
  - `knowledge-base/07_flujos_principales.md`
  - `knowledge-base/01_vision_y_objetivos.md`
  - `knowledge-base/09_decisiones_y_supuestos.md` §SU-01, §SU-02
  - `knowledge-base/08_arquitectura_propuesta.md` §Seguridad

---

## FASE 5 — Diferenciadores (post-MVP núcleo, opcionales según plazo)

> Marcados [DIF] en la KB. Se evalúan contra PA-01; si el plazo es corto, quedan fuera sin afectar el MVP. Pueden ir en paralelo entre sí.

### [C-20] `lista-de-espera`
- **Estado**: `[ ]` pendiente
- **Scope**:
  - Modelo ListaEspera (paciente, prestación, profesional opcional, rango deseado, estado activa|ofrecida|atendida|vencida); Migración 006; alta del paciente en la lista y gestión por recepción/administrador.
  - Al cancelarse un turno, buscar pacientes compatibles y notificar la oferta por email; el primero que acepta obtiene el turno vía verificación transaccional (C-10/C-09).
  - Si nadie acepta en el plazo, el hueco vuelve a estar libre en la reserva online.
  - Tests: oferta a compatibles, dos aceptan a la vez (solo uno gana), vencimiento.
- **Dependencias**: `C-10, C-17`
- **Governance**: MEDIO
- **Responsable**: Renzo
- **Oleada**: 8
- **Revisor (aprueba el PR)**: Florencia
- **Tamaño estimado**: M
- **Zona**: `backend/src/main/java/ar/dentaflow/dominio/listaespera/`, `backend/src/main/java/ar/dentaflow/infraestructura/email/`
- **Bloqueado por**: plazo de oferta no definido (sin PA asignada: dejar parametrizable)
- **Leer antes**:
  - `knowledge-base/07_flujos_principales.md` §Flujo 6
  - `knowledge-base/06_funcionalidades.md` §US-030
  - `knowledge-base/05_reglas_de_negocio.md` §RN-CA-04
  - `knowledge-base/04_modelo_de_datos.md` §ListaEspera

### [C-21] `whatsapp-prellenado`
- **Estado**: `[x]` completo (2026-11-26, PR #36; archivado en `openspec/changes/archive/2026-11-26-c-21-whatsapp-prellenado/`)
- **Scope**:
  - Botón en el panel de recepción que abre un enlace `wa.me` con mensaje prellenado (recordatorio/confirmación) para el turno (US-032).
  - Plantilla de mensaje configurable; sin envío automático ni API (RN-NO-04).
  - Tests: construcción del enlace (codificación, teléfono normalizado), caso sin teléfono.
- **Dependencias**: `C-11`
- **Governance**: BAJO
- **Responsable**: Jessica
- **Oleada**: 8
- **Revisor (aprueba el PR)**: Florencia
- **Tamaño estimado**: S
- **Zona**: `frontend/src/features/agenda/`
- **Bloqueado por**: — (PA-03 resuelta: solo enlace `wa.me` prellenado)
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` §US-032
  - `knowledge-base/05_reglas_de_negocio.md` §RN-NO-04
  - `knowledge-base/09_decisiones_y_supuestos.md` §DD-05

### [C-22] `reintento-confirmacion`
- **Estado**: `[ ]` pendiente
- **Scope**:
  - Si el paciente no confirma en X horas: reenviar el aviso y alertar a recepción en la agenda (US-033, RN-NO-03).
  - X configurable; idempotencia del reintento.
  - Tests: reintento solo para `pendiente`, alerta visible, no se repite.
- **Dependencias**: `C-18`
- **Governance**: MEDIO
- **Responsable**: Bruno
- **Oleada**: 7
- **Revisor (aprueba el PR)**: Florencia
- **Tamaño estimado**: S
- **Zona**: `backend/src/main/java/ar/dentaflow/infraestructura/scheduler/`, `frontend/src/features/agenda/`
- **Bloqueado por**: PA-08 (valor de X; dejar parametrizable)
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` §US-033
  - `knowledge-base/05_reglas_de_negocio.md` §RN-NO-03
  - `knowledge-base/07_flujos_principales.md` §Flujo 2 (paso 4)

### [C-23] `panel-ausentismo`
- **Estado**: `[x]` completo (2026-11-26; archivado en `openspec/changes/archive/2026-11-26-c-23-panel-ausentismo/`)
- **Scope**:
  - Consultas agregadas de tasa de ausentismo por profesional, franja horaria y paciente a partir de los estados de asistencia (US-031).
  - Vista para administrador (todo), recepción (supuesto) y profesional (propio) según la matriz de permisos.
  - Tests: cálculo de tasas con datos de ejemplo, permisos por rol.
- **Dependencias**: `C-13`
- **Governance**: BAJO
- **Responsable**: Florencia
- **Oleada**: 7
- **Revisor (aprueba el PR)**: Bruno
- **Tamaño estimado**: M
- **Zona**: `backend/src/main/java/ar/dentaflow/aplicacion/reportes/`, `frontend/src/features/agenda/`
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` §US-031
  - `knowledge-base/03_actores_y_roles.md` §RBAC (Panel de ausentismo)

### [C-24] `manual-publico`
- **Estado**: `[ ]` pendiente
- **Scope**:
  - Página pública breve que muestra cada función del sistema con capturas o pasos (US-034).
  - Accesible sin autenticación; contenido en español.
- **Dependencias**: `C-19`
- **Governance**: BAJO
- **Responsable**: Jessica
- **Oleada**: 9
- **Revisor (aprueba el PR)**: Florencia
- **Tamaño estimado**: S
- **Zona**: `frontend/src/features/reserva/`, documentación
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` §US-034
  - `knowledge-base/03_actores_y_roles.md` §Rutas públicas

---

## Fuera de este roadmap (post-MVP, sin ID)

Por DD-01 y el alcance de la KB quedan fuera y no deben colarse en los changes anteriores: ficha clínica y Ley 26.529, odontograma, imágenes, presupuestos, cobros y Mercado Pago, obras sociales, ARCA, WhatsApp Business API, Google Calendar, multisede, campañas y app nativa. Al decidirse su incorporación, agregar changes con IDs nuevos (C-25 en adelante), sin renumerar los existentes.

---

## Resumen

| Fase | Changes | Governance máx. |
|------|---------|-----------------|
| 0 — Decisión y base | C-01, C-02 | MEDIO |
| 1 — Núcleo | C-03 a C-08 | CRITICO |
| 2 — Agenda | C-09 a C-13 | CRITICO |
| 3 — Paciente | C-14 a C-18 | ALTO |
| 4 — Cierre MVP | C-19 | MEDIO |
| 5 — Diferenciadores | C-20 a C-24 | MEDIO |

**Primer item elegible**: `C-02` (scaffolding-proyecto). C-01 completado el 2026-11-24.
