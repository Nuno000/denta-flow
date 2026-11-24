# CHANGES — Secuencia de Implementación

> Índice canónico de todos los changes del proyecto **DentaFlow** (agenda online de turnos para consultorios odontológicos chicos de una sola sede, Argentina).
> Cada change es atómico: un agente puede implementarlo en una sesión (~4-6 horas).
> **Leer este archivo antes de empezar un item.**
>
> **Stack decidido el 2026-11-23 (DD-08, PA-02 resuelta):** Java 21 + Spring Boot 3 (Maven), PostgreSQL 16 + Flyway, React + Vite + TypeScript, monorepo `backend/` + `frontend/`, hosting gratuito Render/Vercel/Neon. C-01 sigue siendo la compuerta: queda pendiente el plazo (PA-01) y agendar PA-03/05/06/10; ningún otro change debe empezar antes.

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

### Paralelismo por fase

> Cada "gate" es un punto de sincronización. Los changes dentro de un gate pueden ejecutarse en paralelo (equipo de 4: usar A/B/C + una cuarta persona de apoyo/revisión).

```
GATE 0: ninguna
  → C-01 decision-stack-y-plazo          (solo, equipo completo decide)

GATE 1: C-01 ✓
  → C-02 scaffolding-proyecto            (solo)

GATE 2: C-02 ✓
  → C-03 modelo-recursos-consultorio     (solo)

GATE 3: C-03 ✓
  → C-04 auth-usuarios-roles             (solo — CRITICO)

GATE 4: C-04 ✓                           ← FORK (4 paralelos)
  → C-08 auditoria-cambios               [Agente A]
  → C-06 horarios-y-bloqueos             [Agente B]
  → C-05 gestion-recursos-admin          [Agente C]
  → C-07 parametros-consultorio          [Agente C]

GATE 5: C-08 ✓ (y C-03 ✓)
  → C-09 modelo-turno-paciente           [Agente A]

GATE 6: C-09 ✓                           ← FORK (4 paralelos)
  → C-10 motor-disponibilidad            [Agente A — si C-06 ✓]
  → C-14 email-transaccional-y-enlaces   [Agente B]
  → C-15 privacidad-ley-25326            [Agente B — si C-07 ✓]
  → C-11 agenda-interna-vistas           [Agente C]

GATE 7: C-10 + C-11 + C-14 + C-15 ✓      ← FORK
  → C-12 carga-manual-sobreturno         [Agente A]
  → C-16 reserva-online-publica          [Agente B]
  → C-13 asistencia-y-registro-cambios   [Agente C]
  → C-18 recordatorios-programados       [Agente C — solo necesita C-14 + C-09]

GATE 8: C-16 + C-07 + C-14 ✓
  → C-17 confirmar-cancelar-reprogramar  [Agente B]

GATE 9: C-12 + C-13 + C-17 + C-18 ✓
  → C-19 cierre-mvp-e2e-y-despliegue     [todo el equipo]

GATE 10 (opcional, post-MVP núcleo): C-19 ✓ (según dependencia de cada uno)
  → C-20 lista-de-espera                 [Agente A — requiere C-10 + C-17]
  → C-21 whatsapp-prellenado             [Agente C — requiere C-11]
  → C-22 reintento-confirmacion          [Agente B — requiere C-18]
  → C-23 panel-ausentismo                [Agente C — requiere C-13]
  → C-24 manual-publico                  [Agente C — requiere C-19]
```

### Camino crítico (10 changes — mínimo irreducible)

```
C-01 → C-02 → C-03 → C-04 → C-08 → C-09 → C-10 → C-16 → C-17 → C-19
```

(C-12 y C-18 también convergen en C-19 pero no alargan el camino.)

### Plan óptimo con 3 agentes

| Paso | Agente A (Backend Core) | Agente B (Backend Aux / Notif / Privacidad) | Agente C (Frontend / Paneles) |
|------|-------------------------|---------------------------------------------|-------------------------------|
| 1 | C-01 (decisión, todos participan) | C-01 | C-01 |
| 2 | C-02 scaffolding | — | — |
| 3 | C-03 modelo-recursos | — | — |
| 4 | C-04 auth-roles | — | — |
| 5 | C-08 auditoría | C-06 horarios-bloqueos | C-05 recursos-admin + C-07 parámetros |
| 6 | C-09 modelo-turno-paciente | — (adelantar diseño de plantillas de email) | — (adelantar diseño de pantallas) |
| 7 | C-10 motor-disponibilidad | C-14 email-enlaces + C-15 privacidad | C-11 agenda-vistas |
| 8 | C-12 carga-manual-sobreturno | C-16 reserva-online-publica | C-13 asistencia-registro + C-18 recordatorios |
| 9 | — (apoyo a C-17 / revisión) | C-17 confirmar-cancelar-reprogramar | — (apoyo UI / pruebas) |
| 10 | C-19 cierre-mvp | C-19 | C-19 |
| 11+ | C-20 lista-de-espera | C-22 reintento | C-21 wa.me + C-23 ausentismo + C-24 manual |

---

## FASE 0 — Decisión y base del proyecto

> Secuencial y corto. C-01 no es código: es la decisión que destraba todo el roadmap.

### [C-01] `decision-stack-y-plazo`
- **Estado**: `[ ]` pendiente (parte del stack **hecha** el 2026-11-23; falta el plazo y el agendado de PAs)
- **Scope**:
  - ~~Decidir y registrar el stack~~ **Hecho**: DD-08 en `knowledge-base/09_decisiones_y_supuestos.md` (Java 21 + Spring Boot 3, PostgreSQL 16, React + Vite + TypeScript, Render/Vercel/Neon); estructura de directorios y variables de entorno completadas en `knowledge-base/08_arquitectura_propuesta.md`. PA-02 resuelta.
  - **Pendiente**: confirmar el plazo de entrega (PA-01) y recortar el alcance del MVP/diferenciadores según el plazo.
  - **Pendiente**: resolver o agendar PA-03, PA-05, PA-06, PA-10 con fecha límite (bloquean C-10, C-16, C-09).
  - Entregable: documento de decisión; sin código.
- **Dependencias**: ninguna
- **Governance**: MEDIO
- **Zona**: `knowledge-base/`, `docs/`
- **Bloqueado por**: PA-01 (plazo) — este change es el que la resuelve.
- **Leer antes**:
  - `knowledge-base/10_preguntas_abiertas.md` §Preguntas abiertas
  - `knowledge-base/08_arquitectura_propuesta.md`
  - `knowledge-base/09_decisiones_y_supuestos.md`
  - `knowledge-base/01_vision_y_objetivos.md`

### [C-02] `scaffolding-proyecto`
- **Estado**: `[ ]` pendiente
- **Scope**:
  - Estructura del monorepo (DD-08): `backend/` (Maven, Spring Boot 3, paquetes `ar.dentaflow.{dominio,aplicacion,infraestructura,presentacion}`) y `frontend/` (Vite + React + TypeScript), según `08_arquitectura_propuesta.md`.
  - Configuración por variables de entorno: `SPRING_DATASOURCE_URL`/`_USERNAME`/`_PASSWORD`, `SPRING_MAIL_HOST`/`_PORT`/`_USERNAME`/`_PASSWORD`, `APP_MAIL_FROM`, `APP_BASE_URL`, `AUTH_SECRET`, `TZ=America/Argentina/Buenos_Aires` (SU-09), `VITE_API_BASE_URL`; `.env.example`, sin secretos en el repo.
  - Nota de stack: Maven + Vite; Flyway para migraciones; Testcontainers (Postgres real) para tests de integración; `docker-compose.yml` (Postgres + Mailpit opcional) para desarrollo local; CI con GitHub Actions (backend, frontend); Vitest + Testing Library en frontend; OpenAPI con springdoc-openapi.
  - Runner de tests, lint, CI mínimo.
  - Utilidad de zona horaria del consultorio (todos los horarios se interpretan en esa zona).
  - Tests: arranque de la app, conexión a BD (Testcontainers), test de humo de CI.
- **Dependencias**: `C-01`
- **Governance**: BAJO
- **Zona**: `backend/pom.xml`, `backend/src/main/resources/`, `frontend/`, `docker-compose.yml`, `.github/workflows/`
- **Leer antes**:
  - `knowledge-base/08_arquitectura_propuesta.md` §Patrones, §Estructura de directorios, §Variables de entorno
  - `knowledge-base/09_decisiones_y_supuestos.md` §SU-09

---

## FASE 1 — Núcleo: recursos, acceso y auditoría

> Tras C-04 se abre el primer fork (C-05, C-06, C-07, C-08 en paralelo).

### [C-03] `modelo-recursos-consultorio`
- **Estado**: `[ ]` pendiente
- **Scope**:
  - Modelos: Profesional, Sillon, Prestacion (con `duracion_minutos`), relación N:M Profesional-Prestacion.
  - Migración 001 con restricciones (activo/inactivo, duración > 0); repositorios base.
  - Seed de desarrollo/demo: 2-3 profesionales, 2-3 sillones, 4-5 prestaciones con duraciones distintas.
  - Tests: invariantes de modelo, duración obligatoria, desactivar sin borrar.
- **Dependencias**: `C-02`
- **Governance**: CRITICO (modelos core que todo lo demás referencia)
- **Zona**: `backend/src/main/java/ar/dentaflow/dominio/recursos/`, `backend/src/main/resources/db/migration/`
- **Leer antes**:
  - `knowledge-base/04_modelo_de_datos.md` §Profesional, §Sillon, §Prestacion, §Seed data inicial
  - `knowledge-base/05_reglas_de_negocio.md` §RN-TU-03
  - `knowledge-base/08_arquitectura_propuesta.md` §Patrones

### [C-04] `auth-usuarios-roles`
- **Estado**: `[ ]` pendiente
- **Scope**:
  - Modelos: Usuario (email, hash de credencial, activo, `profesional_id` opcional), Rol (`administrador`, `recepcion`, `profesional`).
  - Login/logout del personal con Spring Security; **decidir cookie de sesión vs JWT**: front (Vercel) y API (Render) están en dominios distintos, lo que complica SameSite/CORS (zona CRITICA, decisión explícita de este change, no de C-01); hash de credencial; rate limiting de login.
  - Autorización RBAC verificada en el backend (no solo en la UI) según la matriz de permisos; guardas reutilizables por rol.
  - Gestión de usuarios y roles por el administrador: crear, desactivar, asignar rol (US-003).
  - Seed: usuario administrador inicial (credencial desde variable de entorno, nunca hardcodeada).
  - Tests: acceso denegado por rol, endpoint protegido sin sesión, usuario desactivado no ingresa.
- **Dependencias**: `C-03`
- **Governance**: CRITICO
- **Zona**: `backend/src/main/java/ar/dentaflow/infraestructura/seguridad/`, `backend/src/main/java/ar/dentaflow/dominio/usuarios/`, `frontend/src/features/auth/`
- **Bloqueado por**: PA-09 (un usuario con múltiples roles: asumir un solo rol hasta decidir)
- **Leer antes**:
  - `knowledge-base/03_actores_y_roles.md` §RBAC — Matriz de permisos
  - `knowledge-base/05_reglas_de_negocio.md` §RN-AU-02
  - `knowledge-base/08_arquitectura_propuesta.md` §Seguridad
  - `knowledge-base/06_funcionalidades.md` §US-003

### [C-05] `gestion-recursos-admin`
- **Estado**: `[ ]` pendiente
- **Scope**:
  - Endpoints/pantallas de administrador para alta, edición y desactivación de profesionales, sillones y prestaciones; asociación de prestaciones por profesional (US-001).
  - Validación de input en backend; solo rol administrador (lectura para recepción y profesional según matriz).
  - Tests: CRUD, permisos por rol, duración obligatoria.
- **Dependencias**: `C-03, C-04`
- **Governance**: BAJO
- **Zona**: `backend/src/main/java/ar/dentaflow/aplicacion/recursos/`, `backend/src/main/java/ar/dentaflow/presentacion/`, `frontend/src/features/agenda/`
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` §US-001
  - `knowledge-base/03_actores_y_roles.md` §RBAC
  - `knowledge-base/07_flujos_principales.md` §Flujo 7

### [C-06] `horarios-y-bloqueos`
- **Estado**: `[ ]` pendiente
- **Scope**:
  - Modelos: HorarioAtencion (profesional, día de semana, hora_inicio, hora_fin; sin rangos superpuestos por profesional y día), Bloqueo (ámbito profesional | sillón | consultorio, inicio, fin, motivo).
  - Migración 002; alta/edición de horarios y bloqueos por el administrador (US-002).
  - Servicio de consulta "¿está disponible este recurso en este rango?" (horario − bloqueos), reutilizable por el motor de disponibilidad.
  - Aviso al crear un bloqueo que afecta turnos existentes (comportamiento exacto abierto, PA-11; cablear el aviso real cuando exista C-09).
  - Tests: horarios superpuestos rechazados, bloqueo por cada ámbito, zona horaria.
- **Dependencias**: `C-03, C-04`
- **Governance**: MEDIO
- **Zona**: `backend/src/main/java/ar/dentaflow/dominio/disponibilidad/`, `backend/src/main/resources/db/migration/`
- **Bloqueado por**: PA-11 (qué pasa con turnos existentes al bloquear; no codificar la resolución hasta decidir)
- **Leer antes**:
  - `knowledge-base/04_modelo_de_datos.md` §HorarioAtencion, §Bloqueo
  - `knowledge-base/05_reglas_de_negocio.md` §RN-TU-04, §RN-GL-01
  - `knowledge-base/06_funcionalidades.md` §US-002
  - `knowledge-base/07_flujos_principales.md` §Flujo 7

### [C-07] `parametros-consultorio`
- **Estado**: `[ ]` pendiente
- **Scope**:
  - Modelo de configuración clave-valor: anticipación mínima de cancelación/reprogramación, textos del aviso de privacidad (US-004).
  - Edición solo por administrador; el valor por defecto se carga desde seed y se marca como provisorio.
  - Tests: lectura/edición de parámetros, permisos, validación de rango.
- **Dependencias**: `C-04`
- **Governance**: BAJO
- **Zona**: `backend/src/main/java/ar/dentaflow/aplicacion/configuracion/`, `backend/src/main/resources/db/migration/`
- **Bloqueado por**: PA-04 (valor por defecto de anticipación mínima; usar un valor provisorio configurable)
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` §US-004
  - `knowledge-base/05_reglas_de_negocio.md` §RN-CA-01, §RN-CA-02
  - `knowledge-base/04_modelo_de_datos.md` §Seed data inicial

### [C-08] `auditoria-cambios`
- **Estado**: `[ ]` pendiente
- **Scope**:
  - Mecanismo de auditoría por evento reutilizable: modelo TurnoHistorial (turno_id, actor usuario/paciente/sistema, fecha-hora, acción, valores anterior/nuevo), solo-inserción (sin edición ni borrado).
  - Migración 003; servicio `registrar_cambio` invocable dentro de la misma transacción que el cambio auditado (RN-AU-01).
  - Tests: el registro se escribe junto al cambio, se revierte si el cambio falla, no es modificable.
- **Dependencias**: `C-04`
- **Governance**: CRITICO (audit trail)
- **Zona**: `backend/src/main/java/ar/dentaflow/dominio/auditoria/`, `backend/src/main/resources/db/migration/`
- **Leer antes**:
  - `knowledge-base/04_modelo_de_datos.md` §TurnoHistorial
  - `knowledge-base/05_reglas_de_negocio.md` §RN-AU-01, §RN-SO-02
  - `knowledge-base/08_arquitectura_propuesta.md` §Patrones (Auditoría por evento)

---

## FASE 2 — Agenda: el modelo de turno y el motor de disponibilidad

> Corazón del diferencial (DD-02). C-09 y C-10 son CRITICOS: ninguna regla de solapamiento puede vivir solo en la UI.

### [C-09] `modelo-turno-paciente`
- **Estado**: `[ ]` pendiente
- **Scope**:
  - Modelos: Paciente (nombre, apellido, DNI, teléfono, email, fecha de consentimiento), Turno (paciente, profesional, sillón, prestación, inicio, fin = inicio + duración, estado, origen online|recepción, `es_sobreturno`, creado_por, token de acceso del paciente).
  - Migración 004 (Flyway) con restricción de integridad a nivel de base de datos: restricción de exclusión PostgreSQL (`btree_gist`, `EXCLUDE USING gist` sobre `tstzrange`) sin solapamiento de [inicio, fin) por profesional y por sillón entre turnos activos (no cancelados), salvo `es_sobreturno` (RN-TU-01/02/07); índices (profesional_id, inicio), (sillon_id, inicio), (paciente_id), (estado, inicio).
  - Máquina de estados: `pendiente`, `confirmado`, `cancelado`, `asistio`, `ausente`, `ausente_con_aviso`, con transiciones válidas explícitas; cancelar libera profesional y sillón (RN-TU-06).
  - Servicio de creación/cambio de turno transaccional (`@Transactional`) con control de concurrencia que captura la violación de la restricción de exclusión y la traduce a conflicto, e invoca la auditoría (C-08). Tests de concurrencia con Testcontainers.
  - Deduplicación de paciente por DNI (SU-03) y token no adivinable por turno.
  - Tests: solapamiento rechazado por profesional y por sillón, concurrencia (dos creaciones simultáneas, solo una gana), transiciones inválidas, sobreturno permitido solo con flag.
- **Dependencias**: `C-03, C-08`
- **Governance**: CRITICO
- **Zona**: `backend/src/main/java/ar/dentaflow/dominio/turnos/`, `backend/src/main/java/ar/dentaflow/dominio/pacientes/`, `backend/src/main/resources/db/migration/`
- **Bloqueado por**: PA-10 (estado inicial del turno online: parametrizar `pendiente`/`confirmado`), PA-06 (asignación de sillón), PA-13 (email obligatorio del paciente), SU-03 (DNI como clave)
- **Leer antes**:
  - `knowledge-base/04_modelo_de_datos.md` §Turno, §Paciente
  - `knowledge-base/05_reglas_de_negocio.md` §RN-TU-01..07, §RN-GL-02
  - `knowledge-base/08_arquitectura_propuesta.md` §Patrones (concurrencia, integridad en BD, máquina de estados)
  - `knowledge-base/09_decisiones_y_supuestos.md` §DD-02, §DD-04, §SU-03

### [C-10] `motor-disponibilidad`
- **Estado**: `[ ]` pendiente
- **Scope**:
  - Motor único de disponibilidad: dado prestación (y profesional si aplica) y rango de fechas, calcula huecos = horario de atención − bloqueos − turnos activos de profesional y sillón, solo huecos donde entra la duración completa (RN-DI-01).
  - Asignación de profesional y sillón al hueco elegido; verificación final dentro de la transacción de creación (reusa C-09) para que el hueco tomado en el medio se rechace.
  - Reutilizado por reserva online, carga manual, reprogramación y lista de espera (sin reglas duplicadas).
  - Nota de stack: la verificación final transaccional se apoya en la restricción de exclusión de C-09 (Postgres) como red de seguridad; el motor no la reemplaza.
  - Tests: hueco que no entra la duración, bloqueo parcial, borde exacto de horario, sillón ocupado con profesional libre, zona horaria/cambio de día.
- **Dependencias**: `C-06, C-09`
- **Governance**: CRITICO
- **Zona**: `backend/src/main/java/ar/dentaflow/dominio/disponibilidad/`, `backend/src/main/java/ar/dentaflow/aplicacion/agenda/`
- **Bloqueado por**: PA-05 (elige profesional o primero disponible), PA-06 (quién se asigna primero, sillón por paciente o sistema); implementar la API con el parámetro profesional opcional y no codificar la política hasta decidir
- **Leer antes**:
  - `knowledge-base/05_reglas_de_negocio.md` §RN-DI-01, §RN-DI-02, §RN-DI-04, §RN-TU-07
  - `knowledge-base/07_flujos_principales.md` §Flujo 1 (pasos 2 y 4)
  - `knowledge-base/08_arquitectura_propuesta.md` §Patrones (Motor de disponibilidad único)
  - `knowledge-base/10_preguntas_abiertas.md` §PA-05, §PA-06

### [C-11] `agenda-interna-vistas`
- **Estado**: `[ ]` pendiente
- **Scope**:
  - Vistas diaria y semanal de la agenda para recepción/administrador, filtrables por profesional y por sillón, mostrando el estado de cada turno (US-020).
  - Vista "mi agenda del día" del profesional: solo ve sus turnos, verificado en backend (US-023).
  - Endpoints de lectura paginados/por rango; diseño responsive del panel.
  - Tests: filtros, aislamiento por rol (profesional no ve turnos ajenos), orden y estados.
- **Dependencias**: `C-04, C-09`
- **Governance**: MEDIO
- **Zona**: `frontend/src/features/agenda/`, `backend/src/main/java/ar/dentaflow/aplicacion/agenda/`
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` §US-020, §US-023
  - `knowledge-base/03_actores_y_roles.md` §RBAC
  - `knowledge-base/07_flujos_principales.md` §Flujo 5

### [C-12] `carga-manual-sobreturno`
- **Estado**: `[ ]` pendiente
- **Scope**:
  - Carga de turno por recepción: buscar/crear paciente (DNI, teléfono, email opcional), elegir prestación, profesional, sillón y horario; rechaza conflictos de profesional/sillón (no es una alerta), respeta horarios y bloqueos (US-021).
  - Sobreturno explícito: acción separada, solo recepción/administrador, marca `es_sobreturno`, registra quién y cuándo; inexistente en el flujo online (US-022, RN-SO-01/02).
  - Ante conflicto, sugerir la alternativa más cercana usando C-10.
  - Tests: conflicto rechazado, sobreturno permitido solo con permiso y auditado, profesional no puede cargar sobreturno.
- **Dependencias**: `C-10, C-11`
- **Governance**: CRITICO
- **Zona**: `backend/src/main/java/ar/dentaflow/aplicacion/turnos/`, `frontend/src/features/agenda/`
- **Leer antes**:
  - `knowledge-base/07_flujos_principales.md` §Flujo 4
  - `knowledge-base/06_funcionalidades.md` §US-021, §US-022
  - `knowledge-base/05_reglas_de_negocio.md` §RN-SO-01, §RN-SO-02, §RN-GL-02
  - `knowledge-base/09_decisiones_y_supuestos.md` §DD-03

### [C-13] `asistencia-y-registro-cambios`
- **Estado**: `[ ]` pendiente
- **Scope**:
  - Marcar turno como `asistio`, `ausente` o `ausente_con_aviso` por profesional (propios) o recepción, con auditoría (US-024).
  - Pantalla de consulta del registro de cambios por turno (quién, cuándo, qué cambió), solo administrador (US-025).
  - Tests: transiciones de asistencia válidas/inválidas, permisos por rol, registro visible y no editable.
- **Dependencias**: `C-08, C-11`
- **Governance**: MEDIO
- **Zona**: `backend/src/main/java/ar/dentaflow/aplicacion/turnos/`, `frontend/src/features/agenda/`
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` §US-024, §US-025
  - `knowledge-base/07_flujos_principales.md` §Flujo 5
  - `knowledge-base/05_reglas_de_negocio.md` §RN-TU-05, §RN-AU-01

---

## FASE 3 — Paciente: privacidad, email y reserva pública

> C-14 y C-15 pueden ir en paralelo con C-10/C-11. Todo lo público requiere validación estricta de input y anti-abuso.

### [C-14] `email-transaccional-y-enlaces`
- **Estado**: `[ ]` pendiente
- **Scope**:
  - Modelo Notificacion (turno_id, tipo recordatorio|confirmación|reintento, canal email, enviada_en, estado de envío); Migración 005.
  - Adaptador de email transaccional detrás de una interfaz (sin acoplar el dominio al proveedor); plantillas de confirmación de reserva.
  - Nota de stack: Spring Mail con SMTP genérico (`SPRING_MAIL_*`, `APP_MAIL_FROM`); elegir en este change el proveedor gratuito (Brevo o Resend); Mailpit en docker-compose para desarrollo.
  - Generación y validación de enlaces con token no adivinable para confirmar, cancelar y reprogramar (SU-03), armados con `APP_BASE_URL`; manejo de enlace vencido o turno ya cancelado.
  - Reintento y registro del estado ante fallo de envío.
  - Tests: envío con proveedor simulado, token inválido/vencido, fallo de envío registrado.
- **Dependencias**: `C-02, C-09`
- **Governance**: ALTO (datos personales en canal de notificación)
- **Zona**: `backend/src/main/java/ar/dentaflow/infraestructura/email/`, `backend/src/main/java/ar/dentaflow/dominio/notificaciones/`, `backend/src/main/resources/db/migration/`
- **Bloqueado por**: PA-13 (email obligatorio)
- **Leer antes**:
  - `knowledge-base/04_modelo_de_datos.md` §Notificacion
  - `knowledge-base/05_reglas_de_negocio.md` §RN-NO-01, §RN-NO-02
  - `knowledge-base/07_flujos_principales.md` §Flujo 2 (casos de error)
  - `knowledge-base/08_arquitectura_propuesta.md` §Seguridad, §Variables de entorno

### [C-15] `privacidad-ley-25326`
- **Estado**: `[ ]` pendiente
- **Scope**:
  - Aviso de privacidad (texto editable desde parámetros) visible en la reserva, con aceptación obligatoria; registro de la fecha de consentimiento en Paciente (US-040).
  - Minimización de datos: solo DNI, nombre, teléfono, email; sin campos clínicos (RN-PR-03).
  - Acceso a datos personales restringido por rol; transporte solo por HTTPS; no registrar datos personales en logs.
  - Política de retención y baja pendiente: dejar el punto de extensión documentado, sin implementarlo.
  - Tests: reserva sin aceptación rechazada, fecha de consentimiento guardada, recurso protegido por rol.
- **Dependencias**: `C-09, C-07`
- **Governance**: ALTO (datos personales, Ley 25.326)
- **Zona**: `backend/src/main/java/ar/dentaflow/dominio/pacientes/`, `frontend/src/features/reserva/`, `backend/src/main/java/ar/dentaflow/infraestructura/seguridad/`
- **Bloqueado por**: PA-12 (retención y baja de datos), revisión legal del texto del aviso
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` §US-040
  - `knowledge-base/05_reglas_de_negocio.md` §RN-PR-01..03
  - `knowledge-base/08_arquitectura_propuesta.md` §Seguridad
  - `knowledge-base/09_decisiones_y_supuestos.md` §DD-07, §SU-07

### [C-16] `reserva-online-publica`
- **Estado**: `[ ]` pendiente
- **Scope**:
  - Flujo público mobile-first sin registro: elegir prestación (y profesional según PA-05) → ver solo huecos donde entra la duración completa → ingresar DNI, teléfono, email → aceptar aviso (US-010).
  - Creación de turno en una única transacción que revalida disponibilidad, asigna profesional y sillón, crea el turno con estado inicial parametrizable, registra historial y dispara el email de confirmación con enlaces.
  - Manejo de hueco tomado entre selección y confirmación: informar y ofrecer otros huecos; mensaje con contacto si no hay huecos.
  - Nunca permite solapamientos ni sobreturno (RN-DI-02); validación de input en backend; rate limiting/captcha a evaluar contra abuso.
  - Tests: reserva exitosa, dos reservas simultáneas del mismo hueco (una falla), aviso no aceptado, datos inválidos, sin huecos.
- **Dependencias**: `C-10, C-14, C-15`
- **Governance**: ALTO
- **Zona**: `frontend/src/features/reserva/`, `backend/src/main/java/ar/dentaflow/aplicacion/turnos/`, `backend/src/main/java/ar/dentaflow/presentacion/` (rutas públicas)
- **Bloqueado por**: PA-05, PA-06, PA-10, PA-13
- **Leer antes**:
  - `knowledge-base/07_flujos_principales.md` §Flujo 1
  - `knowledge-base/06_funcionalidades.md` §US-010
  - `knowledge-base/05_reglas_de_negocio.md` §RN-DI-01..04
  - `knowledge-base/08_arquitectura_propuesta.md` §Seguridad (anti-abuso)
  - `knowledge-base/03_actores_y_roles.md` §Rutas públicas

### [C-17] `confirmar-cancelar-reprogramar`
- **Estado**: `[ ]` pendiente
- **Scope**:
  - Enlace de confirmación de un toque: `pendiente` → `confirmado`, reflejado en la agenda (US-011).
  - Cancelación por el paciente hasta la anticipación mínima configurada; libera profesional y sillón; auditada (US-012, RN-CA-01/02).
  - Reprogramación: solo huecos válidos del motor (C-10), cambio transaccional que valida solapamientos, auditado.
  - Fuera de la ventana: mensaje indicando contactar al consultorio (RN-CA-03); enlace vencido o turno cancelado: mensaje explicativo.
  - Tests: dentro/fuera de ventana, hueco nuevo tomado, token ajeno rechazado, doble confirmación idempotente.
- **Dependencias**: `C-16, C-07, C-14`
- **Governance**: ALTO
- **Zona**: `frontend/src/features/reserva/`, `backend/src/main/java/ar/dentaflow/aplicacion/turnos/`
- **Bloqueado por**: PA-04 (anticipación mínima por defecto, se usa el parámetro de C-07)
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
- **Zona**: `backend/src/main/java/ar/dentaflow/infraestructura/scheduler/`, `backend/src/main/java/ar/dentaflow/dominio/notificaciones/`
- **Bloqueado por**: PA-07 (cuánto antes y cuántos recordatorios; dejar parametrizable)
- **Leer antes**:
  - `knowledge-base/07_flujos_principales.md` §Flujo 2
  - `knowledge-base/05_reglas_de_negocio.md` §RN-NO-01
  - `knowledge-base/08_arquitectura_propuesta.md` §Patrones (Tareas programadas)

---

## FASE 4 — Cierre del MVP

### [C-19] `cierre-mvp-e2e-y-despliegue`
- **Estado**: `[ ]` pendiente
- **Scope**:
  - Tests end-to-end con Playwright de los flujos principales (reserva, recordatorio/confirmación, cancelación/reprogramación, carga manual con sobreturno, asistencia, configuración).
  - Despliegue (planes gratuitos): API en Render, frontend en Vercel, Postgres en Neon; configurar CORS/cookies entre dominios según lo decidido en C-04 y el cron externo de C-18.
  - Prueba de concurrencia explícita contra la garantía de no solapamiento (reserva online + carga manual simultáneas).
  - Revisión de seguridad: RBAC en backend, rutas públicas, rate limiting, secretos en variables de entorno, HTTPS, sin datos personales en logs.
  - Seed de demo, despliegue a un entorno accesible, guía de puesta en marcha en el README.
  - Validación con al menos un consultorio o demo real para contrastar SU-01 y SU-02.
- **Dependencias**: `C-12, C-13, C-17, C-18`
- **Governance**: MEDIO
- **Zona**: `frontend/` (Playwright), `.github/workflows/`, configuración de despliegue, documentación
- **Bloqueado por**: PA-01 (plazo: define el corte real del MVP)
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
- **Zona**: `backend/src/main/java/ar/dentaflow/dominio/listaespera/`, `backend/src/main/java/ar/dentaflow/infraestructura/email/`
- **Bloqueado por**: plazo de oferta no definido (sin PA asignada: dejar parametrizable)
- **Leer antes**:
  - `knowledge-base/07_flujos_principales.md` §Flujo 6
  - `knowledge-base/06_funcionalidades.md` §US-030
  - `knowledge-base/05_reglas_de_negocio.md` §RN-CA-04
  - `knowledge-base/04_modelo_de_datos.md` §ListaEspera

### [C-21] `whatsapp-prellenado`
- **Estado**: `[ ]` pendiente
- **Scope**:
  - Botón en el panel de recepción que abre un enlace `wa.me` con mensaje prellenado (recordatorio/confirmación) para el turno (US-032).
  - Plantilla de mensaje configurable; sin envío automático ni API (RN-NO-04).
  - Tests: construcción del enlace (codificación, teléfono normalizado), caso sin teléfono.
- **Dependencias**: `C-11`
- **Governance**: BAJO
- **Zona**: `frontend/src/features/agenda/`
- **Bloqueado por**: PA-03 (si se automatiza con la API oficial, sería otro change; este asume enlace prellenado, IN-01)
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
- **Zona**: `backend/src/main/java/ar/dentaflow/infraestructura/scheduler/`, `frontend/src/features/agenda/`
- **Bloqueado por**: PA-08 (valor de X; dejar parametrizable)
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` §US-033
  - `knowledge-base/05_reglas_de_negocio.md` §RN-NO-03
  - `knowledge-base/07_flujos_principales.md` §Flujo 2 (paso 4)

### [C-23] `panel-ausentismo`
- **Estado**: `[ ]` pendiente
- **Scope**:
  - Consultas agregadas de tasa de ausentismo por profesional, franja horaria y paciente a partir de los estados de asistencia (US-031).
  - Vista para administrador (todo), recepción (supuesto) y profesional (propio) según la matriz de permisos.
  - Tests: cálculo de tasas con datos de ejemplo, permisos por rol.
- **Dependencias**: `C-13`
- **Governance**: BAJO
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

**Primer item elegible**: `C-01` (decision-stack-y-plazo).
