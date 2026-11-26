# pacientes Specification

## Purpose

Registra a los pacientes del consultorio con los datos mínimos para gestionar sus turnos (DNI, nombre, apellido, teléfono y email), identificados y deduplicados por DNI sin cuenta propia (DD-04, SU-03, SU-07, PA-13, RN-PR-03).

## Requirements

### Requirement: Datos obligatorios del paciente
Todo paciente SHALL tener DNI, nombre, apellido, teléfono y email (PA-13, resuelta el 2026-11-25). El sistema MUST rechazar con un error de validación, sin guardar nada, un paciente al que le falte cualquiera de esos datos o que supere el largo máximo (nombre y apellido 100, email 254).

#### Scenario: Alta con todos los datos
- **WHEN** se registra un paciente con DNI `30123456`, nombre `Ana`, apellido `Ficticia`, teléfono `+54 11 5555-0000` y email `ana@example.com`
- **THEN** el paciente queda guardado con esos datos y un id propio

#### Scenario: Falta el email
- **WHEN** se registra un paciente sin email o con email en blanco
- **THEN** se rechaza con un error de validación y no se guarda ningún paciente

#### Scenario: Falta el DNI
- **WHEN** se registra un paciente sin DNI o con DNI en blanco
- **THEN** se rechaza con un error de validación y no se guarda ningún paciente

#### Scenario: Falta el teléfono o el nombre
- **WHEN** se registra un paciente sin teléfono, sin nombre o sin apellido
- **THEN** se rechaza con un error de validación y no se guarda ningún paciente

### Requirement: Formato del email del paciente
El email del paciente SHALL tener formato `algo@dominio.ext`, sin espacios ni segunda arroba, y MUST guardarse en minúsculas.

#### Scenario: Email con mayúsculas
- **WHEN** se registra un paciente con email `Ana@Example.COM`
- **THEN** se guarda como `ana@example.com`

#### Scenario: Email inválido
- **WHEN** se registra un paciente con email `ana@` o `ana example.com`
- **THEN** se rechaza con un error de validación

### Requirement: DNI normalizado
El DNI SHALL guardarse solo con dígitos: el sistema MUST quitar puntos, espacios y guiones y MUST rechazar el DNI que, normalizado, no tenga 7 u 8 dígitos o contenga otros caracteres.

#### Scenario: DNI con puntos
- **WHEN** se registra un paciente con DNI `30.123.456`
- **THEN** el DNI se guarda como `30123456`

#### Scenario: DNI con letras o largo inválido
- **WHEN** se registra un paciente con DNI `30A23456` o `123456`
- **THEN** se rechaza con un error de validación

### Requirement: Teléfono de contacto válido
El teléfono SHALL guardarse con sus dígitos y un `+` inicial opcional, sin espacios ni guiones, y MUST tener entre 8 y 15 dígitos.

#### Scenario: Teléfono con espacios y guiones
- **WHEN** se registra el teléfono `+54 11 5555-0000`
- **THEN** se guarda como `+541155550000`

#### Scenario: Teléfono demasiado corto
- **WHEN** se registra el teléfono `5555`
- **THEN** se rechaza con un error de validación

### Requirement: DNI único con deduplicación
El DNI SHALL identificar a un único paciente (SU-03): registrar un paciente con un DNI ya existente MUST reutilizar ese paciente y MUST NOT crear una segunda fila, también cuando el DNI llega con otro formato.

#### Scenario: Segunda reserva con el mismo DNI
- **GIVEN** existe el paciente 5 con DNI `30123456`
- **WHEN** se registra un paciente con DNI `30.123.456`
- **THEN** se devuelve el paciente 5 y sigue habiendo un solo paciente con ese DNI

#### Scenario: Altas simultáneas del mismo DNI nuevo
- **GIVEN** no existe ningún paciente con DNI `30999888`
- **WHEN** dos operaciones registran a la vez un paciente con DNI `30999888` (perfil `it-mysql`)
- **THEN** ambas devuelven el mismo paciente y queda una sola fila con ese DNI

### Requirement: Actualización de datos según el origen
Al reutilizar un paciente por DNI, el origen `online` (sin autenticar) MUST NOT modificar nombre, apellido, teléfono ni email ya guardados; el origen `recepcion` (personal autenticado) SHALL reemplazarlos por los datos recibidos.

#### Scenario: Reserva online con otro teléfono
- **GIVEN** el paciente 5 con DNI `30123456` tiene teléfono `+541155550000`
- **WHEN** llega una reserva online con DNI `30123456` y teléfono `+541166660000`
- **THEN** el paciente 5 conserva el teléfono `+541155550000`

#### Scenario: Recepción corrige el email
- **GIVEN** el paciente 5 tiene email `ana@example.com`
- **WHEN** recepción registra al DNI `30123456` con email `ana.nueva@example.com`
- **THEN** el paciente 5 queda con email `ana.nueva@example.com`

### Requirement: Fecha de consentimiento del aviso de privacidad
El paciente SHALL guardar la fecha y hora en que aceptó el aviso de privacidad como instante UTC (RN-PR-01); una aceptación posterior MUST reemplazar la fecha anterior. Un paciente sin aceptación registrada MUST tener la fecha vacía.

#### Scenario: Aceptación registrada
- **WHEN** el paciente acepta el aviso el 2026-12-01 a las 09:00 en `America/Argentina/Buenos_Aires`
- **THEN** la fecha de consentimiento se guarda como `2026-12-01T12:00:00Z`

#### Scenario: Paciente cargado sin aceptación
- **WHEN** recepción registra un paciente sin aceptación del aviso
- **THEN** la fecha de consentimiento queda vacía

### Requirement: Integridad de pacientes en la base
La base SHALL rechazar, aunque se escriba directo con SQL, un paciente sin DNI, nombre, apellido, teléfono o email, y un segundo paciente con el mismo DNI, cada caso por una restricción con nombre propio, tanto en H2 `MODE=MySQL` como en MySQL 8.

#### Scenario: DNI duplicado escrito directo en la base
- **WHEN** se inserta por SQL un paciente con un DNI que ya existe
- **THEN** la base lo rechaza por la restricción `uk_paciente_dni`

#### Scenario: Email nulo escrito directo en la base
- **WHEN** se inserta por SQL un paciente con email nulo
- **THEN** la base lo rechaza

### Requirement: Datos personales fuera de logs y auditoría
El sistema MUST NOT escribir DNI, teléfono, email ni nombre del paciente en los logs ni en los valores del historial de turnos; el historial SHALL referenciar al paciente solo por su id (Ley 25.326).

#### Scenario: Alta de turno auditada
- **WHEN** se crea un turno para el paciente 5
- **THEN** el valor nuevo del historial contiene `"paciente_id":"5"` y ningún DNI, teléfono, email ni nombre
