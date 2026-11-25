# Proposal

## Why

RN-AU-01 exige que cada cambio en un turno registre quién lo hizo y cuándo, y RN-SO-02 lo pide en particular para el sobreturno. C-09 (`modelo-turno-paciente`) es la única vía de escritura de turnos y depende de C-08 (`CHANGES.md`, oleada 3, camino crítico): necesita un mecanismo de auditoría listo para invocar dentro de su misma transacción. Sin él, C-09, C-11, C-12 y C-13 no pueden cumplir los criterios "el cambio queda auditado" de US-012, US-022 y US-024.

## What Changes

- Tabla `turno_historial` (migración Flyway `V008_1__crear_turno_historial.sql`, SQL válido en MySQL 8 y H2 `MODE=MySQL`): turno, actor (usuario del personal, paciente o sistema), fecha y hora en UTC, acción y valores anterior y nuevo como texto JSON.
  - `turno_id` es `BIGINT NOT NULL` con índice y **sin FK**: la tabla `turno` nace en C-09, que agrega la FK en su propia migración.
  - `CHANGES.md` habla de "Migración 003"; se usa la convención vigente de DD-09 (`V0NN_<n>`), por eso es `V008_1`.
- Modelo de dominio puro `dominio.auditoria` (sin Spring ni JPA): `CambioDeTurno`, `Actor`, `AccionDeTurno` y el puerto `HistorialDeTurnoRepositorio`, solo con alta y consulta.
- Servicio de aplicación `ServicioDeAuditoria.registrarCambio(...)` que **exige** una transacción abierta (`Propagation.MANDATORY`): el registro se confirma o se revierte junto con el cambio auditado y no puede escribirse suelto.
- Solo inserción garantizado en la aplicación: el puerto no ofrece edición ni borrado y el mapeo JPA marca todas las columnas como no actualizables. Los permisos de la base (usuario sin `UPDATE`/`DELETE` sobre la tabla) quedan para C-19.
- Consulta del historial de un turno en orden cronológico (puerto y servicio de solo lectura), para que C-13 solo agregue el endpoint y la pantalla de US-025.
- Sin endpoints REST ni pantallas: C-08 entrega el mecanismo; la consulta para el administrador (US-025) es de C-13.

## Capabilities

### New Capabilities
- `auditoria-turnos`: registro solo-inserción de los cambios de cada turno (quién, cuándo, qué acción, valores anterior y nuevo), atómico con el cambio auditado, y su consulta cronológica por turno.

### Modified Capabilities
<!-- Ninguna: C-08 no cambia requisitos de capacidades existentes. -->

## Impact

- **Backend**: paquetes nuevos `dominio/auditoria/`, `aplicacion/auditoria/`, `infraestructura/persistencia/auditoria/`; mapeo nuevo `META-INF/orm/auditoria.xml`; migración `V008_1__crear_turno_historial.sql`.
- **Reutiliza** de C-04: la tabla `usuario` (FK del actor usuario), el `Clock` de `RelojConfig` y `UsuarioActual` (quien llama arma el actor). No cambia endpoints, seguridad ni frontend.
- **Contrato para C-09, C-11, C-12 y C-13**: invocar `registrarCambio` dentro de la transacción del cambio; C-09 agrega `fk_turno_historial_turno`.
- **Governance**: CRÍTICO (audit trail). Revisa y aprueba la PR Florencia.
- **Tests**: dominio puro (JUnit), integración con H2 `MODE=MySQL` (atomicidad, rechazo sin transacción, inmutabilidad) y restricciones de la tabla en el perfil `it-mysql` contra MySQL 8 real. Sin Docker ni Testcontainers.
