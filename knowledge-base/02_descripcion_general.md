# Descripción General

## Stack tecnológico

**DECIDIDO el 2026-11-23 y actualizado el 2026-11-24** (ver `09_decisiones_y_supuestos.md`, DD-08 y DD-09). La cátedra permite elegir libremente el stack (PA-02 resuelta).

| Capa | Tecnología | Estado |
|---|---|---|
| Frontend (web, mobile-first) | React + Vite + TypeScript (SPA única para reserva pública y panel interno), React Router, TanStack Query, React Hook Form + Zod, Tailwind CSS; tests con Vitest + Testing Library y E2E con Playwright | Decidido |
| Backend / API | Java 21 + Spring Boot 3 (Web, Security, Data JPA, Validation, Mail), Maven; REST + OpenAPI (springdoc-openapi) | Decidido |
| Base de datos | MySQL 8 (InnoDB) + Flyway; anti-solapamiento con bloqueo pesimista del profesional y luego del sillón dentro de la transacción (ver `04_modelo_de_datos.md` y DD-09); tests sobre H2 `MODE=MySQL` y concurrencia contra MySQL real (perfil `it-mysql`) | Decidido |
| Envío de email transaccional | SMTP genérico vía Spring Mail; proveedor gratuito (Brevo o Resend) a elegir en C-14 | Decidido (proveedor pendiente) |
| Tareas programadas (recordatorios, reintentos) | Operación idempotente disparada por cron externo contra endpoint protegido; `@Scheduled` en local (a decidir en C-18) | Propuesto |
| Despliegue / hosting | Render (API) y Vercel (frontend), planes gratuitos; proveedor de MySQL a elegir en C-19 (PA-14); desarrollo local con MySQL nativo, sin contenedores; CI con GitHub Actions | API y frontend decididos; base pendiente |

El esquema de autenticación (cookie de sesión o JWT) se decide en C-04 por el despliegue en dominios distintos.

Requisitos no funcionales que el stack debe cumplir, derivados de discovery:

- Interfaz web adaptable al celular (mobile-first para el paciente); sin app nativa en el MVP.
- Control de concurrencia en la reserva para impedir solapamientos aun con reservas simultáneas.
- Programación de tareas en segundo plano (recordatorios por email).
- Registro de auditoría (quién y cuándo) en cada cambio de turno.
- Cumplimiento de Ley 25.326 (datos personales).

## Tipo de sistema

Aplicación web (SPA React) con backend propio (API REST Spring Boot), de un solo consultorio (single-site). Una instancia atiende a un consultorio; la multisede y el multi-tenant están fuera del MVP.

## Arquitectura general

Descripción lógica (implementada con el stack de DD-08 y DD-09):

```
Paciente (celular/PC)          Recepción / Profesional / Admin
        |                                   |
        v                                   v
  Reserva pública (link)             Panel interno (login, roles)
        \                                  /
         \                                /
          +--------> Backend / API <------+
                       |   |   |
                       |   |   +--> Motor de disponibilidad y anti-solapamiento
                       |   +------> Servicio de notificaciones (email; wa.me)
                       v
                 Base de datos (turnos, recursos, auditoría)
                       ^
                       |
              Tareas programadas (recordatorios, reintentos)
```

Componentes lógicos:

- **Reserva pública**: calcula y muestra solo huecos reales (la duración completa de la prestación debe entrar) y registra turnos sin cuenta de paciente.
- **Panel interno**: agenda diaria/semanal por profesional y por sillón; carga manual; sobreturno; estados de asistencia; configuración (admin).
- **Motor de disponibilidad**: única fuente de verdad para validar horarios, bloqueos y solapamientos de profesional y sillón. Debe usarse tanto en la reserva online como en la carga manual.
- **Notificaciones**: email transaccional con enlace de confirmación de un toque; enlaces wa.me prellenados.
- **Auditoría**: registro de cada cambio de turno (quién, cuándo, qué).

## Integraciones externas

| Servicio | Propósito | Tipo | Etapa |
|---|---|---|---|
| Email transaccional (SMTP; Brevo o Resend, a elegir en C-14) | Recordatorios y confirmaciones | SMTP | MVP |
| WhatsApp (enlaces wa.me prellenados) | Recordar/confirmar sin costo por mensaje | Enlace (sin API) | MVP (diferenciador) |
| WhatsApp Business API oficial | Envío automatizado (costo por mensaje de Meta, p. ej. USD 0,026 según DentalSoft) | API | Posterior; **abierto si entra en MVP** |
| Mercado Pago | Señas y cobros | API | Posterior |
| ARCA | Facturación electrónica | Web service | Posterior |
| Google Calendar | Sincronización de agenda | API | Posterior |

## API REST (si aplica)

REST con contrato OpenAPI generado por springdoc-openapi (DD-08); los endpoints concretos se definirán en cada change. Recursos previstos (a nivel conceptual): profesionales, sillones/boxes, prestaciones, horarios y bloqueos, disponibilidad, turnos (con transiciones de estado), lista de espera, usuarios/roles, auditoría.
