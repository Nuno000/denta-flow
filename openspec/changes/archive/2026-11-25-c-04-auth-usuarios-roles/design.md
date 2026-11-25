# Design — C-04 `auth-usuarios-roles`

> Governance **CRÍTICO**: este diseño requiere la aprobación explícita de Bruno antes de `/opsx:apply`.
> **Aprobación: aprobado (2026-11-25, por Bruno)**, con la tabla `rol` y el ABM simple de usuarios incluidos. Motivación y alcance: ver `proposal.md`. Comportamiento exigido: ver `specs/autenticacion-personal`, `specs/autorizacion-por-rol` y `specs/gestion-usuarios`.

## Context

- Backend Spring Boot 3.5 (C-02) con capas `presentacion`, `aplicacion`, `dominio` e `infraestructura`. Todavía no hay Spring Security, ningún controlador ni manejador global de errores. `GET /actuator/health` es público por spec (`configuracion-entorno`).
- C-03 fijó los patrones que este change replica: dominio en Java puro (verificado por `DominioSinFrameworksTest`), mapeo JPA en XML por dominio (`META-INF/orm/<dominio>.xml`), repositorios Spring Data que extienden `Repository` (sin `delete*`) e implementan el puerto del dominio, restricciones con nombre único en todo el esquema, tests de restricciones duplicados en H2 y MySQL (`Restricciones…Base` con subclases H2 y `@Tag("mysql")`) y seeds como `ApplicationRunner` condicionados por propiedad.
- Frontend React 19 + React Router 7 + TanStack Query + RHF + Zod (C-02). `apiClient` es el único cliente HTTP y hoy no envía credenciales. `features/auth/` está vacío.
- Frontend en Vercel y API en Render: sitios distintos (DD-08). Render free corre una sola instancia y puede reiniciarse.
- Política de diseño visual del proyecto: `off` (ver `design-system/README.md`).

## Goals / Non-Goals

**Goals:**
- Una sola cadena de seguridad que deniega por defecto, para que los changes siguientes solo declaren el rol de cada endpoint.
- Que desactivar o cambiar el rol de un usuario tenga efecto inmediato sin infraestructura de revocación.
- Dejar listos para los changes siguientes el formato de errores, las guardas por rol, el acceso al usuario actual (C-08) y el prefijo público (C-16).

**Non-Goals:**
- Refresh tokens, revocación de tokens, recuperación de contraseña por email, cambio de la propia contraseña, multirol (PA-09) y bloqueo de cuentas persistente.
- Límite de intentos distribuido entre instancias.
- Edición del email de un usuario y paginación o búsqueda en el listado (con pocos usuarios no hacen falta).

## Decisions

### D1. JWT Bearer en lugar de cookie de sesión

Token JWT firmado con HMAC-SHA256 (`HS256`) usando `AUTH_SECRET`, enviado en `Authorization: Bearer`. Claims mínimos: `sub` (id del usuario), `iat`, `exp` (8 h) e `iss` (`dentaflow`). El rol **no** va en el token como fuente de verdad (ver D3).

- **Por qué**: front y API en sitios distintos. Una cookie de sesión sería de terceros: exige `SameSite=None; Secure`, Safari y Firefox la bloquean por defecto y además habría que proteger contra CSRF. Con Bearer no hay CSRF ni cookies, y el mismo esquema funciona en local (`localhost:5173` → `localhost:8080`) y en producción.
- **Alternativas**: (a) cookie de sesión con proxy de Vercel (`/api/*` reescrito a Render) para que sea first-party; logout inmediato, pero suma CSRF, configuración de rewrites y sesiones perdidas cuando Render reinicia, o una tabla de sesiones. (b) Tokens opacos guardados en la base; revocables, pero es más código que no aporta en el MVP. Elegida por el usuario el 2026-11-25.
- **Implementación**: `spring-boot-starter-oauth2-resource-server` (trae Nimbus JOSE + JWT): `NimbusJwtDecoder.withSecretKey(...)` valida firma, `exp` e `iss`; `NimbusJwtEncoder` con `ImmutableSecret` emite. Se descarta JJWT, que sugiere el registro de skills, porque el resource server ya resuelve la validación, el 401 y el `WWW-Authenticate` sin una dependencia extra.

### D2. Clave de firma y vencimiento por configuración

- `app.auth.secreto: ${AUTH_SECRET:}`, `app.auth.duracion-token: PT8H` y `app.auth.emisor: dentaflow` en un `@ConfigurationProperties` validado al arrancar. Si el secreto falta o tiene menos de 32 bytes (mínimo para HS256), el arranque falla con un mensaje que indica cómo generarlo (`openssl rand -base64 48`).
- Los perfiles `test` e `it-mysql` definen un secreto fijo **solo de pruebas**, con un nombre que lo declara así. No es un secreto real y no habilita nada fuera de los tests.
- 8 h cubre una jornada del consultorio sin refresh token. Al vencer, el usuario vuelve a ingresar.

### D3. Usuario y rol releídos en cada request

Un `Converter<Jwt, AbstractAuthenticationToken>` propio lee el usuario por `sub` en cada request. Si no existe o está inactivo, lanza `BadCredentialsException` y el resource server responde 401. Si está activo, construye un `UsuarioAutenticado` (id, email, rol, profesionalId) como principal, con la autoridad `ROLE_<ROL>`.

- **Por qué**: cumple "usuario desactivado no sigue operando" y "el cambio de rol aplica en el request siguiente" sin revocación. El costo es una consulta por PK por request, despreciable para un consultorio chico.
- **Alternativa**: rol en el claim y lista de tokens revocados. Más estado y más casos borde.
- C-08 obtiene el actor con un componente `UsuarioActual` (en `infraestructura/seguridad`) que lee el `SecurityContext`, así la auditoría no depende de Spring Security directamente.

### D4. Dominio `usuarios` en Java puro

- `Rol` (enum): `ADMINISTRADOR`, `RECEPCION`, `PROFESIONAL`, con su código en minúsculas (`administrador`, `recepcion`, `profesional`, igual que la KB). Es la referencia en el código para las guardas y las reglas: la autorización no puede depender de filas editables. Se persiste como ese código con un `AttributeConverter` en infraestructura y la base lo valida contra el catálogo `rol` (D5).
- `RolDisponible(Rol rol, String nombre)` (record) y el puerto de solo lectura `CatalogoDeRoles.listar()`, para mostrar el nombre visible en la UI.
- `Usuario`: `nuevo(email, hashCredencial, rol, profesionalId)` valida el email (obligatorio, normalizado con `strip().toLowerCase(Locale.ROOT)`, máximo 254 y formato con `@`) y la coherencia rol–profesional. Además expone `cambiarRol(rol, profesionalId)`, `desactivar()` y `activar()`. Las contraseñas en claro **nunca** entran al dominio: `Usuario` solo conoce el hash.
- `Usuario.cambiarCredencial(hash)` para el blanqueo de contraseña.
- Puertos: `UsuarioRepositorio` (`guardar`, `buscarPorId`, `buscarPorEmail`, `listarTodos`, `existeAdministradorActivo`, `existeUsuarioDelProfesional`, `idsDeProfesionalesConUsuario`), `CatalogoDeRoles` y `CodificadorDeCredenciales` (`codificar`, `coincide`), este último implementado en infraestructura con `BCryptPasswordEncoder`.
- La longitud de la contraseña (8 a 72) se valida en el caso de uso, antes de codificar, igual en el alta y en el blanqueo. 72 es el límite de bytes que BCrypt procesa: aceptar más daría una falsa sensación de seguridad.
- **Tabla `rol`** (pedida por el usuario el 2026-11-25, coincide con la KB §Usuario/Rol): catálogo con los tres roles y su nombre visible. El enum de Java y la tabla deben coincidir. Un test (`CatalogoDeRolesConsistenciaTest`) verifica que las filas de `rol` son exactamente los códigos del enum, así un desfasaje falla en CI y no en producción.
- **Alternativa descartada**: enum más `CHECK (rol IN (...))` sin tabla. Es más simple, pero no deja el catálogo con nombres visibles para la UI ni la base preparada para el multirol. Si PA-09 se resuelve a favor del multirol, una migración nueva crea `usuario_rol (usuario_id, rol_codigo)` y migra la columna.

### D5. Esquema `V004_1__crear_usuarios.sql`

```sql
CREATE TABLE rol (
    codigo VARCHAR(20) NOT NULL,
    nombre VARCHAR(50) NOT NULL,
    CONSTRAINT pk_rol PRIMARY KEY (codigo),
    CONSTRAINT uk_rol_nombre UNIQUE (nombre)
) ENGINE=InnoDB;

INSERT INTO rol (codigo, nombre) VALUES
    ('administrador', 'Administrador'),
    ('recepcion', 'Recepción'),
    ('profesional', 'Profesional');

CREATE TABLE usuario (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    email            VARCHAR(254) NOT NULL,
    credencial_hash  VARCHAR(100) NOT NULL,
    rol              VARCHAR(20)  NOT NULL,
    profesional_id   BIGINT       NULL,
    activo           BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT uk_usuario_profesional UNIQUE (profesional_id),
    CONSTRAINT fk_usuario_rol FOREIGN KEY (rol) REFERENCES rol (codigo),
    CONSTRAINT ck_usuario_rol_profesional CHECK (
        (rol = 'profesional' AND profesional_id IS NOT NULL)
        OR (rol <> 'profesional' AND profesional_id IS NULL)),
    CONSTRAINT fk_usuario_profesional FOREIGN KEY (profesional_id) REFERENCES profesional (id)
) ENGINE=InnoDB;
```

- **Clave natural en `rol`** (elegida por el usuario el 2026-11-25): `codigo` es la PK y `usuario.rol` la referencia directamente. Así el `CHECK` de coherencia rol–profesional sigue en la base, porque compara el código sin consultar otra tabla, y las filas de `usuario` se leen sin join. Con un id numérico (`usuario.rol_id`), esa regla quedaría solo en el backend.
- Los roles son datos de referencia fijos: se cargan en la misma migración, no en un seed condicionado como los recursos de demo de C-03, porque deben existir en todo entorno, incluido `test`. Cambiar un nombre visible requiere una migración nueva. La migración se guarda en UTF-8 (default de Flyway) por la "ó" de "Recepción".
- El email se guarda ya normalizado. La unicidad simple equivale a "sin distinguir mayúsculas" y evita depender de la collation, que no se comporta igual en MySQL y en H2.
- `UNIQUE (profesional_id)` admite varios `NULL` en MySQL 8 y en H2, que es lo que necesitan los usuarios no profesionales.
- Sin columnas de fecha: la auditoría de quién y cuándo es de C-08. Así este change no maneja instantes ni zona horaria; el único instante es el `exp` del token, en UTC.
- La versión `V004_1` sigue la convención de DD-09 (C-03 usó `V003_1`).

### D6. Casos de uso en `aplicacion/usuarios`

- `ServicioDeIngreso.ingresar(email, contrasena)`: consulta el limitador (D7), busca por email normalizado y verifica activo y hash. Ante email inexistente igual ejecuta un `coincide` contra un hash ficticio, para que el tiempo de respuesta no revele si el email existe. Si todo está bien, emite el token con un puerto `EmisorDeTokens` implementado en `infraestructura/seguridad`. Cualquier fallo lanza `CredencialesInvalidasException`, con un único mensaje.
- `ServicioDeUsuarios` (`@Transactional`): `crear`, `listar`, `cambiarRol`, `desactivar`, `activar`, `blanquearContrasena`, `listarRoles` y `listarProfesionalesDisponibles`. Valida que el profesional exista y esté activo con `ProfesionalRepositorio.buscarPorId` (C-03) y que no tenga usuario. Los profesionales disponibles salen de `ProfesionalRepositorio.listarActivos()` menos `idsDeProfesionalesConUsuario()`. Rechaza el cambio de rol, la desactivación y el blanqueo sobre uno mismo (`OperacionNoPermitidaException`, 409). Traduce la violación de unicidad de la base (`DataIntegrityViolationException`) a `ConflictoException`, que cubre la carrera de dos altas simultáneas con el mismo email.
- Excepciones nuevas en `dominio/comun`: `RecursoNoEncontradoException` (404), `ConflictoException` (409) y `OperacionNoPermitidaException` (409). Las reutilizan los changes siguientes.

### D7. Límite de intentos en memoria, por email

`LimitadorDeIntentosDeIngreso` en `aplicacion`, con un `ConcurrentHashMap<email, ventana>` y el `Clock` de C-02 (`RelojConfig`). Con 5 fallos en una ventana fija de 15 minutos desde el primero, bloquea hasta que la ventana vence y lanza `DemasiadosIntentosException`, con los segundos restantes, que se traduce a 429 + `Retry-After`. Un ingreso correcto limpia la entrada. Los valores se configuran con `app.auth.intentos-maximos` y `app.auth.ventana-intentos`.

- **Por qué por email y no por IP**: detrás del proxy de Render la IP real llega en `X-Forwarded-For` y es falsificable si no se configura bien. Por email protege la cuenta atacada, que es lo que importa con pocos usuarios.
- **Trade-off aceptado**: alguien puede bloquear 15 minutos el ingreso de un usuario conociendo su email. El estado se pierde si Render reinicia. Las dos cosas son aceptables para el MVP y quedan documentadas.
- **Alternativa**: Bucket4j o una tabla de intentos. Es otra dependencia o más estado para el mismo resultado en una sola instancia.

### D8. Cadena de seguridad y guardas

`ConfiguracionSeguridad` (`infraestructura/seguridad`):
- `SessionCreationPolicy.STATELESS`, CSRF deshabilitado (no hay cookies), `httpBasic` y `formLogin` deshabilitados.
- `permitAll`: `GET /actuator/health`, `POST /api/auth/login`, `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`, `/api/publico/**` y los preflight `OPTIONS`. El resto usa `anyRequest().authenticated()`.
- `oauth2ResourceServer(jwt)` con el convertidor de D3, y un `AuthenticationEntryPoint` y un `AccessDeniedHandler` que escriben `ProblemDetail` (401 con `WWW-Authenticate: Bearer`, 403).
- `@EnableMethodSecurity` y guardas como meta-anotaciones en `infraestructura/seguridad/guardas`: `@SoloAdministrador` (`hasRole('ADMINISTRADOR')`), `@PersonalInterno` (cualquiera de los tres roles) y `@AdministradorORecepcion`. Los controladores declaran una por endpoint. La matriz de `03_actores_y_roles.md` se traduce endpoint por endpoint en cada change.
- **Documentación OpenAPI pública**: describe la API pero no expone datos. Se acepta en el MVP para facilitar la corrección del TP, y C-19 puede cerrarla en producción con `springdoc.api-docs.enabled=false`. Se registra un `SecurityScheme` `bearerAuth` para probar los endpoints desde Swagger UI.

### D9. Formato de errores común (`presentacion/errores`)

`ManejadorDeErrores` (`@RestControllerAdvice`) devuelve `ProblemDetail` (`application/problem+json`) con `title` y `detail` en español:
- 400: `ValidacionDeDominioException`, `MethodArgumentNotValidException` (agrega `errores` por campo) y JSON ilegible.
- 401: `CredencialesInvalidasException`. 404: `RecursoNoEncontradoException`. 409: `ConflictoException` y `OperacionNoPermitidaException`. 429: `DemasiadosIntentosException` + `Retry-After`.
- 500: mensaje genérico sin traza. Se loguea el error sin datos personales.

Los 401 y 403 que produce el filtro de seguridad (antes de llegar al controlador) usan el mismo formato desde el entry point y el access denied handler de D8.

### D10. API REST

| Método y ruta | Guarda | Respuesta |
|---|---|---|
| `POST /api/auth/login` `{email, contrasena}` | pública | 200 `{token, venceEn, usuario}` · 400 · 401 · 429 |
| `GET /api/auth/yo` | `@PersonalInterno` | 200 `UsuarioRespuesta` |
| `GET /api/usuarios` | `@SoloAdministrador` | 200 `UsuarioRespuesta[]` |
| `POST /api/usuarios` `{email, contrasena, rol, profesionalId?}` | `@SoloAdministrador` | 201 + `Location` · 400 · 409 |
| `PUT /api/usuarios/{id}/rol` `{rol, profesionalId?}` | `@SoloAdministrador` | 200 · 400 · 404 · 409 |
| `POST /api/usuarios/{id}/desactivar` | `@SoloAdministrador` | 200 · 404 · 409 |
| `POST /api/usuarios/{id}/activar` | `@SoloAdministrador` | 200 · 404 |
| `PUT /api/usuarios/{id}/contrasena` `{contrasena}` | `@SoloAdministrador` | 204 · 400 · 404 · 409 |
| `GET /api/usuarios/profesionales-disponibles` | `@SoloAdministrador` | 200 `ProfesionalDisponible[]` (id, nombre, apellido) |
| `GET /api/roles` | `@SoloAdministrador` | 200 `RolRespuesta[]` (codigo, nombre) |

- DTOs `record` con Bean Validation (`@NotBlank`, `@Email`, `@Size`, `@NotNull`). `UsuarioRespuesta(id, email, rol, profesionalId, profesionalNombre, activo)` nunca incluye la credencial. `profesionalNombre` evita que la pantalla tenga que consultar los profesionales para mostrar el listado. El rol viaja como código en minúsculas, igual que en la base y la KB.
- Los profesionales disponibles cuelgan de `/api/usuarios/…` y no de `/api/profesionales`, que es la API de recursos de C-05 (Florencia): este change no invade esa zona.
- Desactivar y activar son acciones explícitas (`POST …/desactivar`) en lugar de `PATCH {activo}`: dejan clara la intención y facilitan la guarda de "no a uno mismo".
- Sin `DELETE` (spec: sin borrado).

### D11. Administrador inicial

`SeedAdministradorInicial` (`infraestructura/seed`, `ApplicationRunner`) se habilita con `app.seed.admin=true`, que vale `true` en `application.yml` y `false` en `test`. Lee `app.admin.email` (`${APP_ADMIN_EMAIL:}`) y `app.admin.password` (`${APP_ADMIN_PASSWORD:}`). Si `existeAdministradorActivo()` da verdadero, no hace nada. Si faltan las variables, deja un `WARN` sin valores. Si no, crea el usuario con el mismo `ServicioDeUsuarios`, que aplica las mismas validaciones que el alta por API. Corre en todos los perfiles salvo `test`, incluida producción, porque es la única forma de tener el primer administrador. Es independiente del seed de recursos de C-03.

### D12. Frontend `features/auth`

- **Almacenamiento del token**: `sessionStorage` (sobrevive a recargar la página y se borra al cerrar la pestaña). La alternativa, solo en memoria, obliga a reingresar en cada recarga. `localStorage` deja el token vivo indefinidamente. Riesgo aceptado: un XSS podría leerlo. Se mitiga porque React escapa la salida, no hay HTML crudo y el vencimiento es de 8 h.
- `sesion.ts`: leer, guardar y borrar el token y el usuario. `apiClient` incorpora un proveedor de token: agrega `Authorization: Bearer` si hay sesión y, ante una respuesta 401 de un request autenticado, borra la sesión y avisa a un suscriptor (sin `window.location`), así el `ProveedorDeSesion` navega a `/ingresar`. La regla "nada de fetch suelto" se mantiene.
- `ProveedorDeSesion` y `useSesion()`: usuario actual, `ingresar` (mutación de TanStack Query sobre `POST /api/auth/login`) y `cerrarSesion`. Al iniciar con un token guardado, valida con `GET /api/auth/yo`.
- `IngresarPage` (`/ingresar`): React Hook Form + Zod (email válido, contraseña obligatoria), `<label>` en cada campo, errores con `aria-describedby`, mensaje general en `role="alert"`, foco en el primer campo inválido, botón deshabilitado mientras envía, layout mobile-first con Tailwind y contraste AA.
- `RutaProtegida` (`roles?: Rol[]`): sin sesión, `<Navigate to="/ingresar" state={{ desde }}>`. Con un rol no permitido, muestra el aviso "No tenés permiso para esta sección".
- `PanelPage` (`/panel`): destino mínimo tras el ingreso, con email, rol, "Cerrar sesión" y, solo para administradores, el enlace "Usuarios". C-11 lo reemplaza por la agenda.
- Tipos: `Rol = 'administrador' | 'recepcion' | 'profesional'` y `UsuarioSesion`, sin `any`.

### D13. Frontend `features/usuarios` (ABM simple)

- Ruta `/panel/usuarios` envuelta en `<RutaProtegida roles={['administrador']}>`.
- `api.ts`: consultas de TanStack Query `usuarios`, `roles` y `profesionales-disponibles`, y mutaciones `crear`, `cambiarRol`, `desactivar`, `activar` y `blanquearContrasena`. Cada mutación invalida `usuarios` y `profesionales-disponibles`. Todo pasa por `apiClient`.
- `UsuariosPage`: una sola pantalla. Encabezado con "Nuevo usuario" y listado: tabla semántica (`<table>`, `<th scope>`) desde `md`, y lista de tarjetas en el celular, con los mismos datos y acciones. Estado con texto ("Activo"/"Inactivo"), no solo color.
- Formularios como diálogos accesibles: `<dialog>` nativo con `showModal()`, foco inicial en el primer campo, `Esc` para cerrar y foco devuelto al botón que lo abrió. Sin librería de componentes nueva.
  - `FormularioUsuario` (alta): email, contraseña, rol (`<select>` del catálogo) y profesional, que solo aparece y es obligatorio con el rol `profesional`. Zod con `superRefine` para esa regla.
  - `FormularioCambioRol`: rol y profesional. Las opciones de profesional son los disponibles más el que el usuario ya tiene vinculado.
  - `FormularioBlanqueo`: contraseña nueva (8 a 72) y confirmación de que coinciden.
  - Desactivar: diálogo de confirmación que nombra el email. Reactivar no pide confirmación.
- Errores: 400 con `errores` por campo se muestran junto a cada campo; 409 junto al campo de email o como mensaje del diálogo (`role="alert"`). El diálogo queda abierto con los datos cargados.
- Sobre la fila del usuario de la sesión, las acciones de cambiar rol, desactivar y blanquear no se muestran, y se ve la marca "(vos)". El backend igual las rechaza con 409.
- Éxito: mensaje breve en una región `aria-live="polite"` ("Usuario creado", "Contraseña cambiada"), sin mostrar la contraseña.

### D14. Estrategia de tests (TDD estricto)

- **Dominio** (unitarios, sin Spring): invariantes de `Usuario` (email, coherencia rol–profesional, cambiar rol, cambiar credencial, activar y desactivar).
- **Aplicación** (Mockito y `Clock` fijo): ingreso correcto e incorrecto, mensaje único, limitador (5 fallos, ventana, reinicio), reglas de `ServicioDeUsuarios` (profesional inexistente o con usuario, acciones sobre uno mismo, conflicto de email, blanqueo, profesionales disponibles).
- **Persistencia** (`@DataJpaTest`, perfil `test`): repositorio, búsqueda por email y catálogo de roles. `RestriccionesUsuarioBase` con subclases H2 y MySQL (`@Tag("mysql")`): catálogo cargado, unicidad de email y de profesional, FK de rol, `CHECK` de coherencia y FK de profesional. `CatalogoDeRolesConsistenciaTest`: filas de `rol` = enum. Sin mocks de base.
- **Seguridad y web** (`@SpringBootTest` + MockMvc sobre H2, con `spring-security-test`): rutas públicas, 401 sin token, vencido y adulterado, 403 por rol en cada endpoint de usuarios, usuario desactivado con token vigente, cambio de rol aplicado en el request siguiente, CORS con origen permitido y desconocido, formato `ProblemDetail` y `Retry-After`.
- **Arranque**: falla sin secreto o con un secreto corto (`ApplicationContextRunner`). Seed del administrador: crea, no duplica y avisa sin variables.
- **Frontend** (Vitest + Testing Library): `IngresarPage` (validación, 401, 429, foco y anuncio), `RutaProtegida` (redirección y aviso de rol), `apiClient` (header y 401), `ProveedorDeSesion` y `UsuariosPage` con sus formularios (listado, alta con y sin profesional, 409 conservando datos, cambio de rol, confirmación de desactivación, blanqueo, fila propia sin acciones). `fetch` simulado en el borde HTTP, nunca los hooks.
- El E2E con Playwright de los flujos de ingreso y ABM queda para C-19 (E2E de cada flujo).

## Risks / Trade-offs

- [Token robado sigue siendo válido hasta 8 h] → Desactivar al usuario lo corta en el request siguiente (D3). Vencimiento acotado.
- [XSS podría leer el token de `sessionStorage`] → Sin HTML crudo, React escapa la salida, el token vence. Revisar en C-19 si conviene sumar una CSP en Vercel.
- [Bloqueo de 15 min de un usuario por un tercero que conoce su email] → Aceptado para el MVP (D7). Un administrador puede esperar la ventana o reiniciar el servicio.
- [El estado del limitador se pierde al reiniciar Render] → Aceptado: el peor caso es habilitar 5 intentos más.
- [Los compañeros no pueden arrancar el backend tras el merge si su `.env` tiene el `AUTH_SECRET` de ejemplo] → `.env.example` explica cómo generarlo, el error de arranque lo dice y se avisa en el PR.
- [Endpoints de changes paralelos sin guarda de rol quedan accesibles a cualquier rol autenticado] → La denegación por defecto evita que queden públicos. Un test de arquitectura verifica que todo método público de un `@RestController` bajo `presentacion` (salvo el login) tenga una guarda de rol.
- [`CHECK` ignorado en MySQL anterior a 8.0.16] → Ya es requisito desde C-03, y el test `@Tag("mysql")` lo verifica.
- [El enum `Rol` y la tabla `rol` se desfasan (alguien agrega una fila o un valor solo en un lado)] → `CatalogoDeRolesConsistenciaTest` falla en CI. Agregar un rol exige migración más enum más guardas, en un mismo change.
- [El ABM suma alcance con la entrega del 26/11 a las 12:00] → Va en los grupos finales de `tasks.md`, después de todo el backend. Si no llega, el backend y la gestión por Swagger ya cumplen US-003, y la pantalla puede salir en un PR aparte sin tocar las specs del backend.
- [Swagger UI público en producción] → Aceptado para el TP (D8). C-19 decide.

## Migration Plan

1. Mergear con `V004_1` nueva. Flyway la aplica al arrancar (`out-of-order: true` ya contempla changes paralelos).
2. Cada integrante agrega a su `.env` un `AUTH_SECRET` propio (≥ 32 bytes), `APP_ADMIN_EMAIL` y `APP_ADMIN_PASSWORD` ficticios, y reinicia: se crea su administrador local.
3. CI: los perfiles `test` e `it-mysql` traen su secreto de pruebas, así que el workflow no cambia.
4. Rollback: revertir el merge. La tabla `usuario` no la referencia nada de C-03, así que en una base local se puede borrar a mano junto con su fila en `flyway_schema_history`. Nunca se edita la migración mergeada.

## Open Questions

- Ninguna bloqueante. PA-09 (multirol) sigue abierta y está contemplada en D4. La exposición de Swagger en producción y la CSP se deciden en C-19 sin cambiar estas specs.

## UI Mockups

- Design system revision: — (política de proyecto `off`; ver `design-system/README.md`)
- Decision: no (project policy) — 2026-11-25
- Mockups: —
- Approval: —
- Preview: —
- Visual verify: —
