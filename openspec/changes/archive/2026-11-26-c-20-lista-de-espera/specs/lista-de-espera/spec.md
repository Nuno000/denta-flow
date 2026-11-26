# Spec — `lista-de-espera`

## Purpose

Gestionar la lista de espera de pacientes para prestaciones odontológicas, ofrecer automáticamente por email los huecos liberados por cancelación a pacientes compatibles, permitir la aceptación por enlace con garantía transaccional anti-solapamiento y vencer las ofertas no aceptadas en el plazo configurado (`US-030`, `RN-CA-04`, Flujo 6).

## ADDED Requirements

### Requirement: Alta y gestión de entradas en lista de espera por personal autorizado
El sistema SHALL permitir únicamente a `administrador` y `recepcion` (`RN-AU-02`) registrar, listar por estado y dar de baja (pasar de `activa` a `vencida`) entradas de lista de espera con `paciente_id`, `prestacion_id`, `profesional_id` opcional, rango de fechas en `America/Argentina/Buenos_Aires` y rango horario opcional. El rol `profesional` MUST recibir `403` y las llamadas sin sesión `401`.

#### Scenario: Alta válida con profesional opcional
- **WHEN** recepción da de alta una entrada para un paciente activo, prestación activa, `profesionalId = null` y rango `2026-12-01` a `2026-12-05`
- **THEN** el sistema responde `201` con la entrada en estado `activa`

#### Scenario: Alta rechazada por rol profesional o sin sesión
- **WHEN** un usuario con rol `profesional` intenta crear o listar entradas en `/api/lista-espera`
- **THEN** el backend responde `403` sin modificar la lista de espera

#### Scenario: Baja de entrada activa por recepción
- **WHEN** recepción cancela una entrada que está en estado `activa`
- **THEN** el sistema la marca como `vencida` y ya no recibe futuras ofertas

### Requirement: Oferta automática por email al cancelarse un turno
Al pasar un turno a `cancelado` (`RN-CA-04`, Flujo 6), el sistema SHALL buscar entradas `activa` compatibles (misma prestación, profesional coincidente o sin preferencia, horario dentro del rango en `America/Argentina/Buenos_Aires`, paciente no anonimizado) si el hueco sigue disponible en `MotorDeDisponibilidad`. Cada entrada compatible SHALL pasar a `ofrecida` con token único y vencimiento `lista-espera-plazo-oferta-horas`, enviando el email tras el commit.

#### Scenario: Cancelación dispara oferta a compatibles
- **WHEN** se cancela un turno de Limpieza el `2026-12-02 10:00` (Buenos Aires) con Dra. Gómez, y existen dos entradas `activa` para Limpieza en ese rango (una con Dra. Gómez y otra sin preferencia) y una tercera con Dr. Pérez
- **THEN** las dos primeras pasan a `ofrecida` con enlace único de aceptación y reciben el email de oferta, y la tercera permanece `activa`

#### Scenario: Hueco bloqueado no se ofrece
- **WHEN** se cancela un turno cuyo horario ahora cae dentro de un bloqueo vigente
- **THEN** el sistema no pasa ninguna entrada a `ofrecida` ni envía emails de oferta

### Requirement: Aceptación pública por enlace con garantía anti-solapamiento
El sistema SHALL exponer la consulta y aceptación pública por `token_aceptacion`. Al aceptar una oferta vigente, MUST crear el turno vía `ServicioDeTurnos.crear` (bloqueo pesimista profesional luego sillón), pasar la entrada aceptada a `atendida` y marcar como `vencida` las demás ofertas del mismo hueco. Si el hueco ya fue tomado, MUST responder `409 Conflict` sin crear turnos solapados.

#### Scenario: Dos pacientes aceptan el mismo hueco simultáneamente
- **WHEN** dos pacientes en estado `ofrecida` para el mismo hueco envían `POST /api/publico/lista-espera/{token}/aceptar` al mismo tiempo
- **THEN** solo uno crea el turno y pasa a `atendida`, el otro recibe `409 Conflict` sin solapamiento en la base de datos y las ofertas restantes del hueco quedan en `vencida`

#### Scenario: Hueco ya tomado por reserva online
- **WHEN** un paciente intenta aceptar una oferta vigente cuyo hueco ya fue reservado por otro paciente en `/reservar`
- **THEN** el sistema rechaza la aceptación con `409 Conflict` y marca la oferta como `vencida`

### Requirement: Vencimiento de ofertas y liberación del hueco
Las ofertas en estado `ofrecida` no bloquean la disponibilidad en `MotorDeDisponibilidad` (`Flujo 6`). Si `ahora >= ofrecida_hasta`, la oferta SHALL pasar a `vencida` al consultarse, intentar aceptarse o ejecutarse la expiración, rechazando la aceptación con `409 Conflict` mientras el hueco permanece libre en la reserva online.

#### Scenario: Intento de aceptar oferta vencida
- **WHEN** un paciente abre o intenta aceptar un enlace cuya fecha `ofrecida_hasta` ya pasó
- **THEN** la entrada pasa a estado `vencida`, no se crea ningún turno y el sistema informa que el plazo de la oferta expiró

#### Scenario: Hueco sigue libre si nadie acepta
- **WHEN** se ofrece un hueco a lista de espera y nadie lo acepta antes de `ofrecida_hasta`
- **THEN** el hueco sigue apareciendo disponible en la reserva online pública (`C-10`/`C-16`) sin intervención manual
