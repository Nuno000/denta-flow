# Proposal — C-07 `parametros-consultorio`

## Why

US-004 pide que el administrador configure la **anticipación mínima** para que el paciente cancele o reprograme (RN-CA-01, RN-CA-02), y RN-PR-01 exige un **aviso de privacidad** visible en la reserva. Hoy no existe ningún lugar donde guardar esos valores: C-17 (`confirmar-cancelar-reprogramar`, Jessica) necesita leer la anticipación mínima y C-15 (`privacidad-ley-25326`, Bruno) necesita los textos del aviso. C-07 está en el corte de entrega (DD-10, versión mínima) y desbloquea a ambos. El valor por defecto de la anticipación sigue abierto (PA-04), así que el sistema arranca con un valor **provisorio** visible como tal hasta que el consultorio lo confirme.

## What Changes

- **Modelo de parámetros clave-valor** (`CHANGES.md` C-07):
  - Catálogo cerrado de claves conocidas por el sistema, cada una con su tipo y su validación: `anticipacion-minima-horas` (entero, en horas, rango 1 a 168), `aviso-privacidad-texto` (texto obligatorio, hasta 4000 caracteres) y `aviso-privacidad-leyenda` (texto obligatorio, hasta 200 caracteres: la frase que el paciente acepta).
  - Cada parámetro guarda su valor, la marca `provisorio` y quién y cuándo lo modificó por última vez.
- **Valores por defecto en la migración** `V007_1__crear_parametros.sql` (DD-09): tabla `parametro` más los tres valores iniciales como datos de referencia, marcados `provisorio = TRUE` (anticipación: 24 h, provisoria por PA-04; textos del aviso: borrador provisorio pendiente de revisión legal, como indica la ficha de C-15). Existen en todo entorno, incluido el perfil `test`, igual que el catálogo de roles de C-04.
- **Edición solo por el administrador** (US-004, RN-AU-02): `GET /api/parametros`, `GET /api/parametros/{clave}` y `PUT /api/parametros/{clave}` con la guarda `@SoloAdministrador`. Recepción y profesional reciben 403; sin token, 401. Un valor fuera de rango o con tipo incorrecto se rechaza con 400 y no cambia nada; una clave desconocida da 404. Guardar un valor (aunque sea el mismo) lo confirma: deja de ser provisorio.
- **Lectura tipada para otros changes**: un servicio de aplicación en `aplicacion/configuracion/` con `anticipacionMinima()` (devuelve una `Duration`) y `avisoPrivacidad()` (texto y leyenda), que C-17 y C-15 consumen sin pasar por HTTP ni parsear texto.
- **Tests** (TDD estricto, sin Docker): validación de rango y tipo en el dominio, restricciones de la tabla y valores por defecto sobre H2 `MODE=MySQL` y sobre MySQL 8 real (`@Tag("mysql")`, perfil `it-mysql`), lectura/edición y permisos por rol sobre HTTP.
- **Fuera de alcance**:
  - Pantalla de administración en el frontend: la ficha marca solo zona backend y DD-10 pide la versión mínima; el administrador puede editar desde la API (Swagger UI). Ver Q4 en `design.md`.
  - Endpoint público del aviso de privacidad para la reserva (`/api/publico/...`) y registro del consentimiento: los agrega C-15 sobre la lectura tipada de este change (ver Q5).
  - Regla de cancelación/reprogramación en sí (comparar la anticipación con el inicio del turno): C-17.
  - Historial o versionado de los textos del aviso (ver Q6).
  - Resolver PA-04: solo se deja el valor provisorio y la forma de reemplazarlo.

## Capabilities

### New Capabilities
- `parametros-consultorio`: parámetros de funcionamiento del consultorio editables por el administrador (anticipación mínima de cancelación/reprogramación y textos del aviso de privacidad), su catálogo de claves, la validación de tipo y rango, los valores por defecto provisorios y la lectura tipada que usan otros procesos.

### Modified Capabilities
- Ninguna. `autorizacion-por-rol` no cambia sus requisitos: los endpoints nuevos nacen protegidos ("Denegar por defecto") y declaran su guarda ("Rol verificado en el backend"); la verificación automática de guardas existente los cubre. `configuracion-entorno` tampoco cambia: la migración nueva se aplica con el mecanismo ya especificado.

## Impact

- **Código nuevo**: `backend/src/main/java/ar/dentaflow/dominio/configuracion/` (modelo y puerto, Java puro), `backend/src/main/java/ar/dentaflow/aplicacion/configuracion/` (servicio), `backend/src/main/java/ar/dentaflow/infraestructura/persistencia/configuracion/` (repositorio Spring Data), `backend/src/main/java/ar/dentaflow/presentacion/configuracion/` (controlador y DTOs), `backend/src/main/resources/META-INF/orm/configuracion.xml`, `backend/src/main/resources/db/migration/V007_1__crear_parametros.sql` y sus tests. La ficha nombra como zona `aplicacion/configuracion/` y `db/migration/`; el modelo va en `dominio/configuracion/` y el controlador en `presentacion/configuracion/` para respetar las capas de `08_arquitectura_propuesta.md`.
- **Archivos compartidos** (cambios mínimos y aditivos, `docs/plan-trabajo-equipo.md` §4): `application.yml` (una línea en `spring.jpa.mapping-resources`) y `db/migration/README.md` (ejemplo de la migración nueva). **No** se toca `ConfiguracionSeguridad` ni `ManejadorDeErrores`: se reutilizan la guarda `@SoloAdministrador` y las excepciones de `dominio.comun` (400, 404) de C-03/C-04.
- **Base de datos**: una tabla nueva `parametro` con FK a `usuario` (C-04). MySQL 8.0.16 o superior para que el `CHECK` se aplique.
- **API**: tres endpoints nuevos bajo `/api/parametros`, documentados en OpenAPI.
- **Changes posteriores**: C-17 lee `anticipacionMinima()`; C-15 lee `avisoPrivacidad()` y publica el endpoint de la reserva; cuando se resuelva PA-04, una migración nueva `V007_2` (o el administrador desde la API) reemplaza el valor provisorio.
- **Dependencias externas**: ninguna nueva en el `pom.xml`.
- **Gobernanza**: BAJO (`CHANGES.md`). Autonomía completa con tests en verde; las decisiones abiertas Q1–Q7 de `design.md` se informan para que Renzo las confirme. Revisor del PR: Florencia.
