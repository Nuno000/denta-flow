# Preguntas Abiertas

## Inconsistencias detectadas

### IN-01 — WhatsApp en el MVP: ¿enlace o API?
**discovery.md dice**: MVP con wa.me; API oficial en etapas posteriores, pero lista la decisión como pregunta abierta.
**informe-discovery.md dice**: recomienda wa.me primero y la API cuando el costo esté validado.
**Impacto**: define si el WhatsApp del MVP es automático o manual.
**RESUELTA el 2026-11-24 (PA-03)**: en el MVP, WhatsApp es solo por enlace `wa.me` prellenado; la API oficial queda fuera (DD-05).

### IN-02 — Informe sin verificación completa
**discovery.md dice**: la sección "Verificación de fuentes" aún debe completarla el equipo.
**informe-discovery.md dice**: la tabla de verificación tiene correcciones ya aplicadas, y no se hicieron demos.
**Impacto**: las afirmaciones competitivas (SU-02) no están validadas.
**Resolución propuesta**: completar la verificación y las demos.

### IN-03 — [DISCOVERY] Campos de descubrimiento no inferibles con confianza
**RESUELTA el 2026-11-23.** Antes, `system_type`, `stack` y `needs_infra` no podían inferirse con confianza porque el stack no estaba decidido. Con DD-08 (ver `09_decisiones_y_supuestos.md`) quedan confirmados: `system_type` = aplicación web (SPA React + API REST Spring Boot), `stack` = Java 21 + Spring Boot 3 / MySQL 8 / React + Vite + TypeScript (actualizado por DD-09 el 2026-11-24), `needs_infra` = sí (MySQL 8, SMTP, hosting en Render/Vercel y proveedor de MySQL a definir en PA-14, cron externo para recordatorios).

## Preguntas abiertas (priorizadas)

| ID | Prioridad | Pregunta | Bloquea | Decisor |
|----|-----------|----------|---------|---------|
| PA-04 | Media | ¿Cuál es la anticipación mínima de cancelación por defecto? | US-004, RN-CA-02 | Equipo |
| PA-07 | Media | ¿Cuánto antes del turno se envía el recordatorio? ¿Uno o varios? | Flujo 2 | Equipo |
| PA-08 | Baja | ¿Cuántas horas (X) se espera antes del reintento de confirmación? | US-033 | Equipo |
| PA-09 | Baja | ¿Un usuario puede tener más de un rol? | RBAC | Equipo |
| PA-11 | Media | Al crear un bloqueo sobre turnos ya existentes, ¿qué ocurre con esos turnos? | Flujo 7 | Equipo |
| PA-12 | Baja | ¿Cuál es la política de retención y baja de datos personales (Ley 25.326)? | Privacidad | Equipo |
| PA-13 | Baja | ¿El email del paciente es obligatorio en la reserva? | SU-07, recordatorios | Equipo |
| PA-14 | Media | ¿Qué proveedor gratuito de MySQL 8 se usa en producción? (reemplaza al de DD-08) | C-19 (despliegue) | Equipo, en C-19 |

Nota: PA-01 a PA-05 corresponden a las cinco preguntas abiertas de `docs/discovery/discovery.md`, sección 11; PA-06 a PA-13 las identificó esta KB al estructurar el modelo; PA-14 surge de DD-09. PA-01, PA-02, PA-03, PA-05, PA-06 y PA-10 están resueltas (ver abajo).

## Preguntas resueltas

| ID | Pregunta | Resolución | Fecha | Referencia |
|----|----------|------------|-------|------------|
| PA-02 | ¿El stack tecnológico es libre o lo fija la cátedra? Si es libre, ¿cuál? | La cátedra permite elegir libremente. Stack: Java 21 + Spring Boot 3 + Maven, MySQL 8 + Flyway (tests sobre H2 `MODE=MySQL`, sin Docker), React + Vite + TypeScript, SMTP, Render/Vercel. | 2026-11-23, base de datos actualizada el 2026-11-24 | DD-08 y DD-09 en `09_decisiones_y_supuestos.md` |
| PA-01 | ¿Cuál es el plazo de entrega del trabajo final? | Jueves 2026-11-26 a las 12:00. Corte de entrega: C-01 a C-04, C-06 a C-10, C-14 a C-17 y C-19. | 2026-11-24 | DD-10 |
| PA-03 | En el MVP, ¿WhatsApp por enlace prellenado o con la API oficial? | Solo enlace `wa.me` prellenado, sin API ni costo. | 2026-11-24 | DD-05, IN-01 |
| PA-05 | En la reserva online, ¿el paciente elige profesional o se asigna el primero disponible? | El paciente elige un profesional o la opción "cualquiera / primero disponible" (parámetro opcional del motor de disponibilidad). | 2026-11-24 | RN-DI-04, US-010, C-10, C-16 |
| PA-06 | ¿Quién se asigna primero y cómo se asigna el sillón en la reserva online? | Primero se fija el profesional; luego el sistema asigna el primer sillón libre en ese rango. El paciente nunca elige sillón. | 2026-11-24 | C-10, C-16 |
| PA-10 | ¿El turno online nace `pendiente` o `confirmado`? | Nace `pendiente` y pasa a `confirmado` con el enlace del email. | 2026-11-24 | RN-TU-05, US-011, C-09, C-17 |
