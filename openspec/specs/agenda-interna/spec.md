# agenda-interna Specification

## Purpose

Permite que el personal interno vea los turnos del consultorio por día o por semana, filtrados por profesional, sillón y estado, y que cada profesional vea solo su propia agenda (US-020, US-023, Flujo 5).

## Requirements

### Requirement: Consulta de turnos por rango de fechas
El sistema SHALL devolver a los roles `administrador`, `recepcion` y `profesional` los turnos cuyo intervalo [inicio, fin) se cruce con el rango pedido. El rango MUST indicarse con una fecha desde y una fecha hasta inclusivas, interpretadas como días completos en `America/Argentina/Buenos_Aires`. Los instantes de la respuesta MUST estar en UTC en formato ISO-8601.

#### Scenario: Turnos de un día
- **WHEN** recepción consulta el día "2026-12-01" y hay un turno de 09:00 a 09:30 hora del consultorio
- **THEN** recibe 200 con ese turno, con inicio `2026-12-01T12:00:00Z` y fin `2026-12-01T12:30:00Z`

#### Scenario: Turno de la noche anterior en UTC
- **WHEN** se consulta el día "2026-11-30" y existe un turno que empieza en `2026-12-01T02:30:00Z`
- **THEN** el turno se incluye, porque en el consultorio empieza el 30/11 a las 23:30

#### Scenario: Turno fuera del rango
- **WHEN** se consulta el día "2026-12-01" y un turno empieza el 2026-12-02 a las 00:00 hora del consultorio
- **THEN** el turno no se incluye

#### Scenario: Sin sesión
- **WHEN** se consulta la agenda sin token
- **THEN** el sistema responde 401 y no devuelve turnos

### Requirement: Rango acotado y válido
El rango MUST tener la fecha desde igual o anterior a la fecha hasta y abarcar como máximo 7 días. Un rango inválido, una fecha mal formada o un filtro con un valor desconocido MUST responderse con 400 en formato de problema, sin devolver turnos.

#### Scenario: Rango invertido
- **WHEN** se consulta con desde "2026-12-05" y hasta "2026-12-01"
- **THEN** el sistema responde 400 indicando el rango inválido

#### Scenario: Rango demasiado largo
- **WHEN** se consulta del "2026-12-01" al "2026-12-08", que son 8 días
- **THEN** el sistema responde 400

#### Scenario: Estado desconocido
- **WHEN** se filtra por el estado "borrado"
- **THEN** el sistema responde 400

### Requirement: Datos de cada turno en la agenda
Cada turno SHALL incluir su identificador, inicio, fin, estado, origen, marca de sobreturno, el nombre y apellido del paciente, el identificador y nombre de la prestación, del profesional y del sillón. La respuesta MUST NOT incluir el DNI, el teléfono, el email del paciente ni el token de acceso del turno.

#### Scenario: Turno con todos sus datos
- **WHEN** se consulta un día con un turno confirmado de "Ana Pérez" para "Limpieza" con el profesional "Juan Gómez" en "Sillón 1"
- **THEN** el turno trae esos nombres, el estado `confirmado`, el origen y `esSobreturno` en falso

#### Scenario: Sin datos sensibles del paciente
- **WHEN** se consulta cualquier rango con turnos
- **THEN** ningún turno de la respuesta contiene DNI, teléfono, email ni token

#### Scenario: Paciente anonimizado
- **WHEN** el paciente de un turno fue dado de baja por anonimización (C-15)
- **THEN** el turno sigue en la agenda con el nombre "Paciente" y el apellido "Anonimizado", sin ningún dato sustituto de contacto

### Requirement: Orden de los turnos
Los turnos SHALL devolverse ordenados por inicio ascendente; a igual inicio, por nombre del profesional y después por nombre del sillón.

#### Scenario: Dos turnos a la misma hora
- **WHEN** hay turnos a las 10:00 con "Laura Díaz" y con "Juan Gómez"
- **THEN** el de "Díaz" aparece antes que el de "Gómez", y ambos después de un turno de las 09:30

### Requirement: Filtros por profesional, sillón y estado
La consulta SHALL aceptar filtros opcionales por profesional, por sillón y por estado, combinables entre sí. Sin filtro de estado, MUST devolver todos los turnos excepto los `cancelado`; los cancelados MUST verse solo filtrando por ese estado.

#### Scenario: Filtro por sillón
- **WHEN** recepción filtra por "Sillón 2"
- **THEN** recibe solo los turnos asignados a "Sillón 2"

#### Scenario: Filtros combinados
- **WHEN** el administrador filtra por un profesional y por el estado `pendiente`
- **THEN** recibe solo los turnos pendientes de ese profesional

#### Scenario: Cancelados ocultos por defecto
- **WHEN** se consulta un día con un turno confirmado y uno cancelado, sin filtro de estado
- **THEN** recibe solo el confirmado

#### Scenario: Ver los cancelados
- **WHEN** se filtra por el estado `cancelado`
- **THEN** recibe solo los turnos cancelados del rango

#### Scenario: Recurso inexistente en el filtro
- **WHEN** se filtra por un profesional o un sillón que no existe
- **THEN** recibe 200 con una lista vacía

### Requirement: El profesional solo ve sus turnos
Para el rol `profesional`, el backend MUST limitar la consulta a los turnos del profesional vinculado a su usuario, aunque no envíe el filtro. Si filtra por otro profesional, MUST responder 403 sin datos. Un usuario `profesional` sin profesional vinculado MUST recibir 403.

#### Scenario: Profesional sin filtro
- **WHEN** un profesional consulta el día sin filtro de profesional y ese día hay turnos suyos y de otro profesional
- **THEN** recibe solo los suyos

#### Scenario: Profesional pide la agenda de otro
- **WHEN** un profesional consulta filtrando por el identificador de otro profesional
- **THEN** el sistema responde 403 en formato de problema y no devuelve turnos

#### Scenario: Recepción ve a todos
- **WHEN** recepción consulta el día sin filtros
- **THEN** recibe los turnos de todos los profesionales

### Requirement: Sección Agenda del panel
El panel SHALL ofrecer la sección "Agenda" en `/panel/agenda` a los tres roles internos, con la vista diaria por defecto en el día de hoy del consultorio. La fecha, la vista y los filtros MUST reflejarse en la URL, de modo que recargar o volver atrás muestre la misma agenda.

#### Scenario: Abrir la agenda
- **WHEN** recepción abre `/panel/agenda` cuando en el consultorio es 2026-11-26
- **THEN** ve la vista diaria del jueves 26 de noviembre de 2026

#### Scenario: Agenda desde un enlace con filtros
- **WHEN** se abre `/panel/agenda?vista=semana&fecha=2026-12-02&sillonId=2`
- **THEN** ve la semana del lunes 30/11 al domingo 6/12 filtrada por el sillón 2

#### Scenario: Hoy en otra zona horaria
- **WHEN** la computadora del usuario marca `2026-11-27T02:00:00Z`
- **THEN** la agenda abre en el 26/11, que es la fecha del consultorio

### Requirement: Vista diaria
La vista diaria SHALL listar los turnos del día en orden cronológico, mostrando horario de inicio y fin en hora del consultorio, paciente, prestación, profesional, sillón y estado. MUST permitir ir al día anterior, al siguiente y a hoy.

#### Scenario: Turnos del día
- **WHEN** el día tiene dos turnos
- **THEN** se ven en orden, cada uno con "09:00–09:30", el paciente, la prestación, el profesional, el sillón y el estado

#### Scenario: Día siguiente
- **WHEN** el usuario elige "Día siguiente" estando en el 26/11
- **THEN** la agenda pasa al 27/11, la URL cambia y se piden los turnos de ese día

#### Scenario: Día sin turnos
- **WHEN** el día no tiene turnos
- **THEN** la pantalla dice "No hay turnos para este día"

### Requirement: Vista semanal
La vista semanal SHALL mostrar de lunes a domingo la semana que contiene la fecha elegida, con los turnos de cada día en orden y el día de hoy identificado. MUST pedir los turnos de la semana en una sola consulta y permitir ir a la semana anterior, a la siguiente y a la actual.

#### Scenario: Semana completa
- **WHEN** el usuario cambia a la vista semanal en el miércoles 2/12
- **THEN** ve los días del lunes 30/11 al domingo 6/12, cada uno con sus turnos o con "Sin turnos"

#### Scenario: Una consulta por semana
- **WHEN** se abre la vista semanal
- **THEN** la pantalla hace una sola consulta con desde el lunes y hasta el domingo

### Requirement: Filtros en la pantalla
Recepción y el administrador SHALL poder filtrar por profesional, sillón y estado con selectores rotulados, que listan los profesionales y sillones del consultorio. El profesional MUST ver el título "Mi agenda" y MUST NOT ver el filtro de profesional. Cambiar un filtro MUST actualizar la URL y la lista.

#### Scenario: Recepción filtra por profesional
- **WHEN** recepción elige un profesional en el filtro
- **THEN** la lista muestra solo sus turnos y la URL incluye su identificador

#### Scenario: Profesional en su agenda
- **WHEN** un profesional abre la agenda
- **THEN** ve el título "Mi agenda", los filtros de sillón y estado, y no el de profesional

#### Scenario: Limpiar los filtros
- **WHEN** el usuario elige "Limpiar filtros"
- **THEN** vuelven los turnos de todos los profesionales y sillones, sin cancelados

### Requirement: Estado y sobreturno visibles sin depender del color
Cada turno SHALL mostrar su estado con texto en castellano: Pendiente, Confirmado, Cancelado, Asistió, Ausente y Ausente con aviso. Un sobreturno MUST llevar la marca "Sobreturno" en texto. El color MUST ser solo un refuerzo y cumplir contraste AA.

#### Scenario: Estado legible
- **WHEN** un turno está en estado `ausente_con_aviso`
- **THEN** se lee "Ausente con aviso"

#### Scenario: Sobreturno marcado
- **WHEN** un turno es sobreturno
- **THEN** se lee "Sobreturno" junto al turno

### Requirement: Pantalla usable y accesible
La agenda MUST poder usarse en un celular de 390 px de ancho sin desplazamiento horizontal y cumplir WCAG 2.1 AA: navegación por teclado, controles rotulados, la fecha mostrada como encabezado y el resultado de cada cambio anunciado a lectores de pantalla.

#### Scenario: Celular
- **WHEN** la agenda semanal se abre en 390 px de ancho
- **THEN** los días se apilan uno debajo de otro y no hay desplazamiento horizontal

#### Scenario: Cambio anunciado
- **WHEN** el usuario pasa al día siguiente
- **THEN** una región de estado anuncia la nueva fecha y la cantidad de turnos

### Requirement: Carga y errores en la pantalla
Mientras se cargan los turnos, la pantalla SHALL indicarlo. Si la API falla, MUST mostrar un aviso de que no se pudo cargar la agenda, con la opción de reintentar, sin romper la pantalla. Un 403 MUST mostrarse como falta de permiso.

#### Scenario: API caída
- **WHEN** la consulta de turnos responde 500
- **THEN** la pantalla muestra "No se pudo cargar la agenda" y un botón "Reintentar"

#### Scenario: Sin permiso
- **WHEN** la consulta responde 403
- **THEN** la pantalla dice que no tiene permiso para ver esa agenda
