# panel-ausentismo Specification

## Purpose

Permite que el personal interno consulte indicadores agregados de ausentismo a partir de los estados de asistencia registrados (`asistio`, `ausente`, `ausente_con_aviso`), desglosados por profesional, franja horaria, día de la semana y pacientes con ausencias reiteradas, con permisos por rol y sin exponer datos sensibles (US-031, C-23).

## ADDED Requirements

### Requirement: Cálculo de métricas de ausentismo
El sistema SHALL calcular los indicadores de ausentismo solo sobre turnos en estado `asistio`, `ausente` o `ausente_con_aviso` iniciados en `[desde, hasta]` según `America/Argentina/Buenos_Aires`. Los turnos `pendiente`, `confirmado` y `cancelado` MUST excluirse. La tasa MUST calcularse como `(totalAusentes / totalEvaluados) * 100` redondeada a un decimal (`0.0` si `totalEvaluados` es `0`).

#### Scenario: Rango con turnos de distintos estados
- **WHEN** en el rango consultado hay 2 turnos `asistio`, 1 `ausente`, 1 `ausente_con_aviso`, 3 `confirmado` y 2 `cancelado`
- **THEN** `totalEvaluados` es `4`, `asistidos` es `2`, `ausentesSinAviso` es `1`, `ausentesConAviso` es `1`, `totalAusentes` es `2` y `tasaAusentismo` es `50.0`

#### Scenario: Rango sin turnos con asistencia registrada
- **WHEN** en el rango consultado solo existen turnos `pendiente` o `cancelado` (o ningún turno)
- **THEN** `totalEvaluados` es `0`, `totalAusentes` es `0` y `tasaAusentismo` es `0.0`

#### Scenario: Borde de medianoche del consultorio (UTC-3)
- **WHEN** existe un turno `ausente` a las `2031-03-05T02:30:00Z` (23:30 del 4/3 en `America/Argentina/Buenos_Aires`) y otro `asistio` a las `2031-03-05T03:00:00Z` (00:00 del 5/3 en el consultorio)
- **THEN** al consultar `desde=2031-03-04&hasta=2031-03-04` solo se contabiliza el turno de las 23:30 del 4/3

### Requirement: Desgloses por profesional, franja horaria y día de la semana
El reporte SHALL incluir `porProfesional` (ordenado por tasa y ausencias descendentes), `porFranja` en hora del consultorio (`manana` < 12:00, `tarde` 12:00–17:59, `noche` >= 18:00, en orden cronológico) y `porDiaSemana` (lunes a domingo para días con evaluados).

#### Scenario: Clasificación en franjas horarias según hora argentina
- **WHEN** hay un turno `ausente` a las 10:00 (`13:00Z`), uno `asistio` a las 14:30 (`17:30Z`) y uno `ausente_con_aviso` a las 19:00 (`22:00Z`)
- **THEN** `manana` registra 1 evaluado / 1 ausente (100.0 %), `tarde` registra 1 evaluado / 0 ausentes (0.0 %) y `noche` registra 1 evaluado / 1 ausente (100.0 %)

#### Scenario: Orden de profesionales por ausentismo
- **WHEN** el Dr. A tiene 1 ausente de 2 evaluados (50.0 %) y la Dra. B tiene 1 ausente de 4 evaluados (25.0 %)
- **THEN** `porProfesional` lista primero al Dr. A y luego a la Dra. B

### Requirement: Pacientes con ausencias reiteradas
El reporte SHALL devolver `pacientesReiterados` con los pacientes cuya suma de ausencias (`ausente + ausente_con_aviso`) sea mayor o igual a `minimoAusencias` (por defecto `2`), ordenados por cantidad de ausencias y tasa descendentes.

#### Scenario: Filtro por umbral de ausencias reiteradas
- **WHEN** el paciente A acumula 2 ausencias (`1 ausente` y `1 ausente_con_aviso`) sobre 3 evaluados y el paciente B acumula 1 ausencia sobre 2 evaluados
- **THEN** con el umbral por defecto (`2`), `pacientesReiterados` incluye únicamente al paciente A con `totalAusentes = 2` y `tasaAusentismo = 66.7`

### Requirement: Validación de parámetros de consulta
`GET /api/reportes/ausentismo` MUST exigir `desde` y `hasta` en formato `AAAA-MM-DD` y rechazar con `400` (`application/problem+json`) si `hasta` es anterior a `desde`, si el rango supera `366` días o si `minimoAusencias` es menor que `1`.

#### Scenario: Rango invertido o excesivo
- **WHEN** se consulta `GET /api/reportes/ausentismo` con `desde=2031-03-10&hasta=2031-03-01` o con un rango de `367` días
- **THEN** el sistema responde `400` con `application/problem+json` y un mensaje claro en español

#### Scenario: Umbral de ausencias inválido
- **WHEN** se consulta con `minimoAusencias=0`
- **THEN** el sistema responde `400` con `application/problem+json`

### Requirement: Autorización por rol en indicadores de ausentismo
`GET /api/reportes/ausentismo` SHALL exigir autenticación (`401` sin token) y permitir los tres roles internos. `administrador` y `recepcion` SHALL consultar todo el consultorio o filtrar por `profesionalId`. `profesional` MUST ver solo sus propios turnos y recibir `403` si pide otro profesional o no está vinculado.

#### Scenario: Administrador o recepción consulta todo el consultorio
- **WHEN** un usuario con rol `administrador` o `recepcion` consulta `GET /api/reportes/ausentismo` sin `profesionalId`
- **THEN** recibe `200` con las métricas combinadas de todos los profesionales

#### Scenario: Profesional consulta sus propios indicadores
- **WHEN** un usuario con rol `profesional` vinculado a la Dra. Laura consulta `GET /api/reportes/ausentismo` sin `profesionalId`
- **THEN** recibe `200` contabilizando exclusivamente los turnos de la Dra. Laura

#### Scenario: Profesional intenta consultar otro profesional
- **WHEN** un profesional pide `GET /api/reportes/ausentismo?profesionalId=<otro>`
- **THEN** el sistema responde `403` con `application/problem+json` sin filtrar datos ajenos

#### Scenario: Sin sesión
- **WHEN** se consulta `GET /api/reportes/ausentismo` sin header `Authorization`
- **THEN** el sistema responde `401`

### Requirement: Minimización de datos personales en el reporte (Ley 25.326)
La respuesta del reporte MUST incluir únicamente `pacienteId`, `nombre` y `apellido` de los pacientes listados, y NEVER incluir DNI, teléfono, email ni token de acceso (RN-PR-03). Los pacientes anonimizados MUST aparecer con sus valores sustitutos (`Paciente Anonimizado`).

#### Scenario: Respuesta sin datos sensibles
- **WHEN** el reporte devuelve pacientes con ausencias reiteradas (incluido un paciente anonimizado)
- **THEN** el JSON contiene solo `pacienteId`, `nombre` y `apellido`, y no contiene `dni`, `telefono`, `email`, `token` ni dominios `.invalid`

### Requirement: Vista Panel de ausentismo en el frontend
El panel interno SHALL ofrecer la ruta protegida `/panel/ausentismo` para los tres roles internos, con filtros de período y umbral, filtro de profesional solo para `administrador` y `recepcion`, tarjetas resumen, desgloses visuales y tablas responsive (390 px y 1440 px) con estados de carga, vacío y error.

#### Scenario: Vista con datos para administrador
- **WHEN** un administrador abre `/panel/ausentismo` y la API devuelve métricas del período
- **THEN** ve el título "Panel de ausentismo", el filtro de profesional, las tarjetas resumen (tasa, evaluados, asistidos, ausentes) y los desgloses por franja, día, profesional y pacientes reiterados

#### Scenario: Vista acotada para profesional
- **WHEN** un profesional abre `/panel/ausentismo`
- **THEN** ve el título "Mi ausentismo", no se muestra el selector de profesional ni la tabla comparativa entre profesionales y la consulta no envía `profesionalId` ajeno

#### Scenario: Estado vacío y estado de error
- **WHEN** la consulta devuelve `totalEvaluados = 0`
- **THEN** la pantalla muestra un estado vacío claro con enlace a la agenda; y si la API falla, muestra una alerta con botón "Reintentar" (o aviso de permiso si es `403`)
