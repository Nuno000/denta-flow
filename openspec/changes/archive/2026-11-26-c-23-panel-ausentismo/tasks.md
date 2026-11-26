# Tasks: Panel de ausentismo (`C-23`, `US-031`)

## 1. Dominio puro de reportes de ausentismo (TDD)

- [x] 1.1 Escribir tests unitarios (`CalculadorDeAusentismoTest`) para tasa general, exclusión de no evaluados, bordes de zona horaria `America/Argentina/Buenos_Aires`, franjas horarias (`manana`, `tarde`, `noche`), días de la semana, orden por profesional y umbral de pacientes con ausencias reiteradas.
- [x] 1.2 Implementar en `ar.dentaflow.dominio.reportes` (`FranjaHoraria`, `ResumenAusentismo`, `RegistroAsistenciaParaReporte`, `AusentismoPorProfesional`, `AusentismoPorFranja`, `AusentismoPorDiaSemana`, `AusentismoPorPaciente`, `ReporteDeAusentismo`, `CalculadorDeAusentismo`) sin importar Spring ni JPA hasta dejar los tests en verde.

## 2. Caso de uso y lectura JPA (H2 + MySQL 8, TDD)

- [x] 2.1 Escribir tests unitarios (`ConsultaDeAusentismoTest`) cubriendo validación de fechas (`desde`/`hasta` obligatorios, rango invertido, > 366 días, `minimoAusencias < 1`), conversión a instantes UTC del consultorio y permisos por rol (admin/recepción todo, profesional solo propios, 403 si pide otro o no vinculado).
- [x] 2.2 Implementar `LecturaDeAusentismo`, `PedidoDeAusentismo` y `ConsultaDeAusentismo` en `ar.dentaflow.aplicacion.reportes`.
- [x] 2.3 Escribir tests de persistencia `LecturaDeAusentismoBase`, `LecturaDeAusentismoH2Test` y `LecturaDeAusentismoMySqlTest` (`@Tag("mysql")`) verificando filtro por rango horario argentino, exclusión de `pendiente`/`confirmado`/`cancelado`, filtro por profesional, paciente anonimizado y ausencia de datos sensibles en la proyección.
- [x] 2.4 Implementar `LecturaDeAusentismoJpa` en `ar.dentaflow.infraestructura.persistencia.reportes` y verificar en H2 y MySQL 8.

## 3. Endpoint REST y documentación OpenAPI (TDD)

- [x] 3.1 Escribir tests de integración `AusentismoControllerTest` cubriendo `GET /api/reportes/ausentismo` (200 con contrato JSON y sin DNI/teléfono/email/token, 400 en fechas o umbral inválidos, 401 sin sesión, 403 en profesional consultando otro profesional o sin vínculo, paciente anonimizado y presencia en `/v3/api-docs`).
- [x] 3.2 Implementar DTOs (`ReporteDeAusentismoRespuesta` y records anidados) y `AusentismoController` con `@PersonalInterno` y anotaciones OpenAPI en `ar.dentaflow.presentacion.reportes`.

## 4. Frontend — Vista Panel de ausentismo y menú del panel (TDD)

- [x] 4.1 Escribir tests unitarios de API y parámetros (`frontend/src/features/ausentismo/api.test.tsx`, `parametros.test.ts`) y actualizar `BarraDelPanel.test.tsx` y `rutas.test.tsx` para la sección `Ausentismo` (`/panel/ausentismo`).
- [x] 4.2 Implementar cliente TanStack Query (`api.ts`), normalización de filtros/rango (`parametros.ts`), entrada `Ausentismo` en `BarraDelPanel.tsx` y ruta `/panel/ausentismo` en `rutas.tsx`.
- [x] 4.3 Escribir tests de UI (`AusentismoPage.test.tsx`) cubriendo vista de administrador/recepción (tarjetas resumen, barras por franja y día, tabla por profesional, pacientes reiterados, filtros), vista de profesional (sin selector ni comparativa de otros profesionales), estado de carga, estado vacío y error (403 y reintento).
- [x] 4.4 Implementar `AusentismoPage.tsx` y sus subcomponentes responsive (390 px y 1440 px, WCAG 2.1 AA) en `frontend/src/features/ausentismo/`.

## 5. Verificación integral y cierre

- [x] 5.1 Ejecutar en verde `./mvnw verify` (H2), `./mvnw verify -Pit-mysql` (MySQL 8 real), `npm test`, `npm run lint`, `npm run build` y `openspec validate --all --strict`.
- [x] 5.2 Archivar `c-23-panel-ausentismo` sincronizando `panel-ausentismo` y `autorizacion-por-rol`, y marcar `C-23` `[x]` en `CHANGES.md`.
