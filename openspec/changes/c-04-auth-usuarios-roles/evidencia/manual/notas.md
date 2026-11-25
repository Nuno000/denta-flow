# C-04 — Prueba manual de punta a punta (tarea 13.3)

Fecha: 2026-11-25. Backend perfil `dev` contra MySQL 8.4 local (base `dentaflow` de desarrollo, por decisión de Bruno) y frontend con `vite` en `localhost:5173`. Navegador: panel del escritorio de Claude (Chromium). Datos ficticios (`@example.com`).

El backend se levantó con un `AUTH_SECRET` aleatorio y un administrador de prueba (`admin-e2e@example.com`) definidos solo en el entorno del proceso. El `.env` no se modificó. La contraseña de ese administrador quedó fuera del repositorio.

## Resultado por paso

| Paso | Resultado |
|---|---|
| Arranque con el `.env` actual (`AUTH_SECRET` de ejemplo) | Falla con "AUTH_SECRET es demasiado corta: necesita al menos 32 bytes (generala con `openssl rand -base64 48`)" ✅ |
| Arranque con clave válida y `APP_ADMIN_*` | Aplica `V004_1`, crea el administrador inicial y carga el seed de demo. El log no muestra email ni contraseña ✅ |
| `/panel` sin sesión | Redirige a `/ingresar` ✅ (01) |
| Ingreso con contraseña incorrecta | "Email o contraseña incorrectos" en alerta; conserva el email ✅ |
| Ingreso correcto (390×844) | Panel con email, rol "Administrador" y enlace "Usuarios" ✅ (03) |
| ABM en el celular | Tabla apilada como tarjetas; fila propia con "(vos)" y sin acciones ✅ (02) |
| Alta de recepción | "Usuario creado" y aparece en el listado ✅ |
| Alta de profesional (1440×900) | El campo Profesional aparece solo con ese rol; queda vinculado a "Laura Ejemplo" ✅ (04, 05) |
| API: `/actuator/health` sin token / `/api/usuarios` sin token / con recepción / con admin | 200 / 401 / 403 / 200 ✅ |
| API: blanqueo de la profesional | 204; ingreso con la anterior 401 y con la nueva 200 ✅ |
| API: desactivar recepción | 200; su token vigente en `/api/auth/yo` da 401 y el ingreso da 401 ✅ |
| API: preflight CORS desde `localhost:5173` / desde otro origen | 200 / 403 ✅ |
| Reactivar desde la pantalla | "Usuario reactivado" y estado "Activo" ✅ |
| Cerrar sesión | Vuelve a `/ingresar` ✅ |
| Volver a ingresar como administrador | Regresa a la ruta pedida (`/panel/usuarios`) ✅ |
| Ingreso como recepción | No ve "Usuarios" en el panel; en `/panel/usuarios` ve "No tenés permiso para esta sección" ✅ |

## Defectos encontrados y corregidos durante la prueba

1. **Diálogo pegado arriba a la izquierda.** El preflight de Tailwind anula el `margin: auto` del `<dialog>` nativo. Se corrigió con `m-auto` en `Dialogo.tsx`.
2. **Página oscurecida por el navegador.** El navegador oscureció la página automáticamente y quedó texto oscuro sobre fondo oscuro, que falla el contraste AA. Esto también pasa en Chrome Android con "oscurecer contenido web". Se corrigió declarando `color-scheme: only light` en `index.html` y `index.css`, con fondo blanco explícito.

Después de los dos arreglos: 47 tests, lint y build en verde (ver `../frontend.txt`).

## Limitaciones

- Las capturas 04 y 05 están reducidas por el tamaño del panel; muestran el layout de escritorio, no el detalle a 1440 px reales.
- No se probó con lector de pantalla real. La accesibilidad se verificó con los tests (etiquetas, `role="alert"`, `aria-live`, foco) y el árbol de accesibilidad del navegador.
- En la base de desarrollo quedaron tres usuarios de prueba: `admin-e2e@example.com`, `recepcion-e2e@example.com` y `profesional-e2e@example.com`. Mientras exista `admin-e2e` activo, el seed no crea el administrador de `APP_ADMIN_EMAIL`.
