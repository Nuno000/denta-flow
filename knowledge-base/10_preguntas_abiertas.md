# Preguntas Abiertas

## Inconsistencias detectadas

### IN-01 — WhatsApp en el MVP: ¿enlace o API?
**discovery.md dice**: MVP con wa.me; API oficial en etapas posteriores, pero lista la decisión como pregunta abierta.
**informe-discovery.md dice**: recomienda wa.me primero y la API cuando el costo esté validado.
**Impacto**: define si el WhatsApp del MVP es automático o manual.
**Resolución propuesta**: asumir wa.me (sin costo) hasta que el equipo decida lo contrario. No resuelto aquí.

### IN-02 — Informe sin verificación completa
**discovery.md dice**: la sección "Verificación de fuentes" aún debe completarla el equipo.
**informe-discovery.md dice**: la tabla de verificación tiene correcciones ya aplicadas, y no se hicieron demos.
**Impacto**: las afirmaciones competitivas (SU-02) no están validadas.
**Resolución propuesta**: completar la verificación y las demos.

### IN-03 — [DISCOVERY] Campos de descubrimiento no inferibles con confianza
**RESUELTA el 2026-11-23.** Antes, `system_type`, `stack` y `needs_infra` no podían inferirse con confianza porque el stack no estaba decidido. Con DD-08 (ver `09_decisiones_y_supuestos.md`) quedan confirmados: `system_type` = aplicación web (SPA React + API REST Spring Boot), `stack` = Java 21 + Spring Boot 3 / PostgreSQL 16 / React + Vite + TypeScript, `needs_infra` = sí (PostgreSQL, SMTP, hosting en Render/Vercel/Neon, cron externo para recordatorios).

## Preguntas abiertas (priorizadas)

| ID | Prioridad | Pregunta | Bloquea | Decisor |
|----|-----------|----------|---------|---------|
| PA-01 | Alta | ¿Cuál es el plazo de entrega del trabajo final? | Alcance y roadmap | Equipo / cátedra |
| PA-05 | Alta | En la reserva online, ¿el paciente elige profesional o se asigna el primero disponible? | US-010, motor de disponibilidad, RN-DI-04 | Equipo |
| PA-04 | Media | ¿Cuál es la anticipación mínima de cancelación por defecto? | US-004, RN-CA-02 | Equipo |
| PA-03 | Media | En el MVP, ¿los recordatorios de WhatsApp son solo por enlace prellenado o se automatizan con la API oficial (con costo)? | US-032, RN-NO-04, integraciones | Equipo |
| PA-06 | Media | ¿Quién (sillón vs profesional) se asigna primero y cómo se asigna el sillón en la reserva online? ¿Lo elige el paciente o el sistema? | Motor de disponibilidad | Equipo |
| PA-07 | Media | ¿Cuánto antes del turno se envía el recordatorio? ¿Uno o varios? | Flujo 2 | Equipo |
| PA-08 | Baja | ¿Cuántas horas (X) se espera antes del reintento de confirmación? | US-033 | Equipo |
| PA-09 | Baja | ¿Un usuario puede tener más de un rol? | RBAC | Equipo |
| PA-10 | Media | ¿El turno online nace `pendiente` o `confirmado`? | Máquina de estados | Equipo |
| PA-11 | Media | Al crear un bloqueo sobre turnos ya existentes, ¿qué ocurre con esos turnos? | Flujo 7 | Equipo |
| PA-12 | Baja | ¿Cuál es la política de retención y baja de datos personales (Ley 25.326)? | Privacidad | Equipo |
| PA-13 | Baja | ¿El email del paciente es obligatorio en la reserva? | SU-07, recordatorios | Equipo |

Nota: PA-01 a PA-05 corresponden a las cinco preguntas abiertas de `docs/discovery/discovery.md`, sección 11; PA-06 a PA-13 las identificó esta KB al estructurar el modelo. Solo PA-02 está resuelta (ver abajo).

## Preguntas resueltas

| ID | Pregunta | Resolución | Fecha | Referencia |
|----|----------|------------|-------|------------|
| PA-02 | ¿El stack tecnológico es libre o lo fija la cátedra? Si es libre, ¿cuál? | La cátedra permite elegir libremente. Stack: Java 21 + Spring Boot 3 + Maven, PostgreSQL 16 + Flyway, React + Vite + TypeScript, SMTP, Render/Vercel/Neon. | 2026-11-23 | DD-08 en `09_decisiones_y_supuestos.md` |
