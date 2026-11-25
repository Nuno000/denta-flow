# Spec Delta — recursos-consultorio

## Purpose

Define los recursos del consultorio que la agenda usa para dar turnos (profesionales, sillones y prestaciones), qué datos son obligatorios, cuánto dura cada prestación, qué prestaciones realiza cada profesional y cómo se dan de baja sin perder historia.

## ADDED Requirements

### Requirement: Duración obligatoria de la prestación
Toda prestación SHALL tener una duración en minutos, entera y mayor que cero, que define la duración del turno (RN-TU-03). El sistema MUST rechazar crear o modificar una prestación sin duración o con duración cero o negativa.

#### Scenario: Prestación con duración válida
- **WHEN** se crea la prestación "Limpieza" con duración de 40 minutos
- **THEN** la prestación queda registrada, activa y con duración de 40 minutos

#### Scenario: Prestación sin duración
- **WHEN** se intenta crear una prestación sin indicar la duración
- **THEN** el sistema la rechaza indicando que la duración es obligatoria y no registra nada

#### Scenario: Duración cero o negativa
- **WHEN** se intenta crear o modificar una prestación con duración de 0 o de -15 minutos
- **THEN** el sistema la rechaza indicando que la duración debe ser mayor que cero

#### Scenario: La base rechaza una duración inválida
- **WHEN** se intenta guardar directamente en la base una prestación con duración 0, sin pasar por el modelo
- **THEN** la base rechaza la escritura por una restricción de integridad, tanto en MySQL 8 como en H2 `MODE=MySQL`

### Requirement: Datos obligatorios del profesional
Todo profesional SHALL tener nombre, apellido y email no vacíos; la matrícula MUST ser opcional. El email y, cuando se informa, la matrícula MUST ser únicos entre profesionales.

#### Scenario: Profesional sin matrícula
- **WHEN** se crea un profesional con nombre, apellido y email, sin matrícula
- **THEN** el profesional queda registrado y activo, con la matrícula vacía

#### Scenario: Dato obligatorio en blanco
- **WHEN** se intenta crear un profesional con el apellido vacío o compuesto solo por espacios
- **THEN** el sistema lo rechaza indicando el dato faltante

#### Scenario: Email repetido
- **WHEN** se intenta registrar un segundo profesional con un email que ya usa otro profesional
- **THEN** el registro se rechaza y el primer profesional no cambia

### Requirement: Identificación del sillón
Todo sillón SHALL tener un nombre no vacío que lo identifique (por ejemplo "Sillón 1") y MUST ser único entre sillones.

#### Scenario: Sillón válido
- **WHEN** se crea el sillón "Sillón 1"
- **THEN** el sillón queda registrado y activo

#### Scenario: Nombre de sillón repetido
- **WHEN** se intenta registrar un segundo sillón llamado "Sillón 1"
- **THEN** el registro se rechaza

### Requirement: Desactivar sin borrar
Profesionales, sillones y prestaciones SHALL poder desactivarse y volver a activarse. Un recurso desactivado MUST conservarse con todos sus datos y relaciones, MUST seguir siendo consultable por su identificador y MUST NOT aparecer en los listados de recursos activos. El sistema MUST NOT ofrecer una operación de borrado de recursos.

#### Scenario: Desactivar un sillón
- **WHEN** se desactiva el sillón "Sillón 2" y se listan los sillones activos
- **THEN** "Sillón 2" no aparece en el listado, pero sigue existiendo y puede consultarse por su identificador con estado inactivo

#### Scenario: Desactivar un profesional conserva sus prestaciones
- **WHEN** se desactiva un profesional que realiza "Consulta" y "Limpieza" y luego se lo reactiva
- **THEN** el profesional vuelve a figurar como activo y sigue realizando "Consulta" y "Limpieza"

#### Scenario: Desactivar dos veces
- **WHEN** se desactiva una prestación que ya estaba inactiva
- **THEN** la operación no falla y la prestación sigue inactiva

### Requirement: Prestaciones que realiza cada profesional
El sistema SHALL registrar qué prestaciones realiza cada profesional (relación muchos a muchos). Solo MUST poder asociarse una prestación existente y activa; asociar la misma prestación dos veces MUST NOT duplicar la relación.

#### Scenario: Asociar prestaciones
- **WHEN** se indica que una profesional realiza "Consulta" y "Extracción"
- **THEN** al consultarla figura con exactamente esas dos prestaciones

#### Scenario: Asociar una prestación inactiva
- **WHEN** se intenta asociar a un profesional la prestación "Blanqueamiento", que está inactiva
- **THEN** el sistema lo rechaza y el profesional no cambia

#### Scenario: Asociación repetida
- **WHEN** se asocia "Consulta" a un profesional que ya la realiza
- **THEN** el profesional sigue teniendo una sola asociación con "Consulta"

#### Scenario: Asociación a una prestación inexistente
- **WHEN** se intenta guardar en la base una asociación hacia una prestación que no existe
- **THEN** la base la rechaza por integridad referencial

### Requirement: Profesionales activos por prestación
El sistema SHALL permitir obtener los profesionales activos que realizan una prestación dada, para que la reserva con "cualquier profesional" (PA-05) y el motor de disponibilidad (C-10) elijan candidatos. Los profesionales inactivos MUST quedar excluidos.

#### Scenario: Filtrado por prestación
- **WHEN** dos profesionales activos realizan "Consulta", uno inactivo también la realiza y otro activo no la realiza, y se piden los profesionales para "Consulta"
- **THEN** el resultado contiene exactamente los dos profesionales activos que la realizan

### Requirement: Recursos bloqueables para la reserva concurrente
Las filas de profesional y de sillón SHALL poder bloquearse para escritura dentro de una transacción (`SELECT ... FOR UPDATE`), para que la creación y el cambio de turnos (C-09, DD-09) serialicen el acceso a ambos recursos en orden fijo: primero el profesional y después el sillón.

#### Scenario: Bloqueo de profesional y sillón
- **WHEN** una transacción bloquea para escritura un profesional y después un sillón existentes
- **THEN** ambos bloqueos se obtienen sin error y los datos leídos son los últimos confirmados

### Requirement: Datos de demostración
El sistema SHALL poder cargar un conjunto de datos de demostración con 3 profesionales, 2 sillones y 5 prestaciones de duraciones distintas, todos ficticios (Ley 25.326). La carga MUST ocurrir solo cuando se habilita explícitamente por configuración, MUST NOT ocurrir en el perfil `test` ni con la configuración por defecto, y MUST ser idempotente.

#### Scenario: Carga en desarrollo
- **WHEN** el backend arranca en el perfil `dev` con la carga de demostración habilitada sobre una base sin recursos
- **THEN** quedan registrados 3 profesionales, 2 sillones y 5 prestaciones activos, con duraciones distintas, y cada profesional realiza al menos una prestación

#### Scenario: Arranque repetido
- **WHEN** el backend vuelve a arrancar con la carga habilitada sobre una base que ya tiene recursos
- **THEN** no se duplica ni se modifica ningún recurso

#### Scenario: Perfil de pruebas y configuración por defecto
- **WHEN** el backend arranca en el perfil `test`, o en cualquier perfil sin habilitar la carga
- **THEN** no se carga ningún dato de demostración

#### Scenario: Datos ficticios
- **WHEN** se revisan los datos de demostración
- **THEN** los nombres son ficticios y los emails usan un dominio reservado para ejemplos, sin datos personales reales
