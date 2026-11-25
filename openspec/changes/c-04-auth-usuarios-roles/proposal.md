# Proposal — C-04 `auth-usuarios-roles`

## Why

Hasta C-03 la API no tiene autenticación: cualquier endpoint que se agregue quedaría abierto. Los paneles internos de recepción, profesionales y administración (C-05, C-06, C-07, C-08, C-11 y siguientes) necesitan saber quién está operando y con qué rol, y la regla dura del proyecto exige que la autorización se verifique en el backend, no solo en la UI (RN-AU-02, `03_actores_y_roles.md` §RBAC). C-04 es el cuarto eslabón del camino crítico (`C-01 → C-02 → C-03 → C-04 → C-08 → ...`) y abre el primer fork de la oleada 3: C-05, C-06, C-07 y C-08 dependen de él. Además la auditoría de C-08 (RN-AU-01, "quién y cuándo") necesita un usuario autenticado al que atribuir cada cambio.

## What Changes

- **Decisión de esquema de autenticación** (pedida explícitamente por `CHANGES.md` para este change): **JWT firmado en el header `Authorization: Bearer`**, sin cookies. El frontend (Vercel) y la API (Render) quedan en sitios distintos, y una cookie de sesión sería de terceros (Safari y Firefox la bloquean). El token se firma con `AUTH_SECRET`, vence a las 8 horas y en cada request se vuelve a leer el usuario, así que desactivar a alguien o cambiarle el rol tiene efecto en el request siguiente.
- **Modelo de dominio** (`ar.dentaflow.dominio.usuarios`, Java puro):
  - `Usuario`: email (único, normalizado), hash de credencial, un rol, `profesional_id` (obligatorio si el rol es `profesional`, prohibido en los otros) y activo/inactivo sin borrado.
  - `Rol`: `administrador`, `recepcion`, `profesional` (RN-AU-02), con un nombre visible para la UI. **Un solo rol por usuario** mientras PA-09 siga abierta, como indica `CHANGES.md`.
  - Puertos: repositorio de usuarios, catálogo de roles y codificador de credenciales.
- **Persistencia**: migración Flyway `V004_1__crear_usuarios.sql` con:
  - la tabla `rol` (catálogo con los tres roles, cargados en la misma migración, como pide el seed de `04_modelo_de_datos.md`);
  - la tabla `usuario`, con unicidad de email y de `profesional_id`, FK del rol hacia `rol`, `CHECK` de coherencia rol–profesional y FK `usuario.profesional_id → profesional.id` (prevista por C-03).

  Mapeo JPA en `META-INF/orm/usuarios.xml`.
- **Autenticación** (US-003, `08_arquitectura_propuesta.md` §Seguridad):
  - `POST /api/auth/login` (público) devuelve el token y los datos del usuario; un mensaje genérico ante email inexistente, contraseña incorrecta o usuario desactivado.
  - `GET /api/auth/yo` devuelve el usuario autenticado.
  - Hash BCrypt; límite de intentos fallidos de login por email (429 al superarlo).
  - Logout del lado del cliente (descarta el token; no hay revocación en el servidor).
- **Autorización RBAC** en el backend: todo endpoint exige autenticación salvo una lista explícita de rutas públicas (`/actuator/health`, login, documentación OpenAPI y el prefijo `/api/publico/**` reservado para la reserva online de C-10/C-16); guardas reutilizables por rol para los controladores de los changes siguientes; respuestas 401/403 en JSON.
- **Gestión de usuarios por el administrador** (US-003), solo con rol `administrador`:
  - listar, crear, cambiar rol (y profesional vinculado), desactivar, reactivar y blanquear la contraseña de otro usuario;
  - consultar el catálogo de roles y los profesionales activos que todavía no tienen usuario, para completar los formularios.

  Sin recuperación de contraseña por email en el MVP, el blanqueo es la única salida si alguien olvida la suya. El administrador no puede desactivarse, cambiarse el rol ni blanquearse la contraseña a sí mismo, para que siempre quede uno activo.
- **Administrador inicial**: se crea al arrancar si no existe ningún administrador activo, con email y contraseña tomados de variables de entorno (nunca del repositorio, Ley 25.326).
- **Manejo de errores común**: formato `ProblemDetail` (RFC 9457) para errores de validación (400), no encontrado (404), conflicto (409), no autenticado (401), sin permiso (403) y demasiados intentos (429). Es la base que reutilizan los changes siguientes.
- **CORS**: solo el origen del frontend (`APP_BASE_URL`), con el header `Authorization`.
- **Frontend** (React Hook Form + Zod, mobile-first, WCAG 2.1 AA):
  - `frontend/src/features/auth/`: pantalla de ingreso, contexto de sesión, envío del token desde el `apiClient`, cierre de sesión, rutas protegidas por rol y un panel interno mínimo de destino tras el ingreso.
  - `frontend/src/features/usuarios/`: **ABM simple de usuarios** para el administrador (decisión del 2026-11-25). Una pantalla con el listado y acciones para crear, cambiar rol, desactivar o reactivar y blanquear la contraseña, con un acceso desde el panel visible solo para administradores.
- **Tests**: acceso denegado por rol, endpoint protegido sin token, token vencido o adulterado, usuario desactivado no ingresa ni sigue operando, límite de intentos, invariantes del dominio, restricciones de la tabla en H2 `MODE=MySQL` y en MySQL 8 real (`@Tag("mysql")`), y tests de componentes del frontend.
- **Fuera de alcance**: edición del email de un usuario; multirol (PA-09); recuperación de contraseña por email y cambio de la propia contraseña; revocación de tokens y refresh tokens; auditoría de cambios (C-08); autenticación del paciente (acceso por enlace con token, C-14/C-16).

## Capabilities

### New Capabilities
- `autenticacion-personal`: ingreso del personal con email y contraseña, emisión y validación del token, usuario desactivado, límite de intentos, administrador inicial y la sesión en el frontend (ingreso, cierre y vencimiento).
- `autorizacion-por-rol`: qué rutas son públicas, que todo lo demás exige autenticación y rol verificados en el backend, las respuestas 401/403 y las rutas protegidas por rol en el frontend.
- `gestion-usuarios`: alta, listado, cambio de rol, desactivación, reactivación y blanqueo de contraseña de usuarios por el administrador, el catálogo de roles, un solo rol por usuario, el vínculo obligatorio con un profesional para el rol `profesional` y la pantalla de ABM.

### Modified Capabilities
- Ninguna. `configuracion-entorno` ya exige que `GET /actuator/health` responda sin autenticación; C-04 respeta ese requisito sin cambiarlo. `recursos-consultorio` no cambia: la FK nueva vive en la tabla `usuario`.

## Impact

- **Código nuevo (backend)**: `dominio/usuarios/`, `aplicacion/usuarios/` (casos de uso de login y gestión, límite de intentos), `infraestructura/seguridad/` (configuración de Spring Security, emisión y validación de JWT, BCrypt, CORS, guardas), `infraestructura/persistencia/usuarios/`, `infraestructura/seed/SeedAdministradorInicial`, `presentacion/auth/`, `presentacion/usuarios/`, `presentacion/errores/`, `db/migration/V004_1__crear_usuarios.sql`, `META-INF/orm/usuarios.xml` y sus tests.
- **Código nuevo (frontend)**: `frontend/src/features/auth/`, `frontend/src/features/usuarios/` (carpeta nueva, fuera de la "Zona" original de `CHANGES.md`) y las rutas del panel interno; cambios en `src/lib/apiClient.ts` (header `Authorization`, 401) y en `src/routes/rutas.tsx`.
- **Archivos compartidos que se tocan**: `backend/pom.xml` (`spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server`, `spring-security-test`), `application.yml` y perfiles (`app.auth.*`, `app.cors.*`, `usuarios.xml`), `.env.example` (`APP_ADMIN_EMAIL`, `APP_ADMIN_PASSWORD` y cómo generar `AUTH_SECRET`), `db/migration/README.md`, `knowledge-base/08_arquitectura_propuesta.md` (variables nuevas y decisión JWT) y `09_decisiones_y_supuestos.md` (decisión nueva DD-11).
- **Efecto en el equipo**: al mergear, el backend no arranca sin un `AUTH_SECRET` de al menos 32 bytes en el `.env` local; el valor de ejemplo actual no alcanza a propósito. Todo endpoint nuevo de C-05 en adelante nace protegido y debe declarar su guarda de rol; los endpoints públicos van bajo `/api/publico/`.
- **Changes posteriores**: C-08 atribuye cada cambio al usuario autenticado; C-05, C-06 y C-07 usan las guardas de administrador; C-11 filtra "turnos propios" con `profesional_id`; C-16 publica bajo `/api/publico/**`; C-19 configura `AUTH_SECRET`, `APP_BASE_URL` y el administrador inicial en Render.
- **Dependencias externas**: las dos starters de Spring Security (ya previstas en el stack de DD-08) y `spring-security-test`. Ninguna en el frontend.
- **Gobernanza**: CRÍTICO. El diseño (`design.md`) requiere la aprobación explícita de Bruno antes de implementar; el PR lo aprueba Florencia.
