# Prueba visual de C-05 (2026-11-25)

Verificación automatizada con Playwright (navegador Chromium) en 390×844 (celular) y 1440×900 (escritorio).

## Entorno

- **Backend**: `./mvnw spring-boot:run` con el classpath de pruebas y el perfil `test` (H2 en memoria, `MODE=MySQL`), con seed de demostración (3 profesionales, 2 sillones, 5 prestaciones ficticios) y un administrador ficticio creado solo para esta sesión (`admin.demo@example.com`).
  - Se eligió H2 para no tocar la base MySQL local ni usar credenciales reales.
  - La persistencia en MySQL 8 está cubierta por `./mvnw verify -Pit-mysql` (ver `verificacion.txt`).
- **Frontend**: `npx vite` con `VITE_API_BASE_URL=http://localhost:8080`.
- **Usuarios**: un administrador y un usuario de recepción ficticio (`recepcion.demo@example.com`), creado por la API.
- Al terminar se detuvieron ambos servidores. La base en memoria se descartó con ellos.

## Flujos recorridos

| Rol | Flujo | Resultado | Captura |
|---|---|---|---|
| Administrador | Pestaña Profesionales (redirección desde `/panel/recursos`) | Tarjetas apiladas en 390 px; tabla en 1440 px; ordenados por apellido; "—" sin matrícula | `capturas/admin-profesionales-390.png`, `capturas/admin-profesionales-1440.png` |
| Administrador | Diálogo "Prestaciones de Laura Ejemplo": se quitó "Obturación (arreglo)" y se guardó | El listado se actualizó sin recargar | `capturas/admin-dialogo-prestaciones-390.png` |
| Administrador | Desactivar "Blanqueamiento" con confirmación | Diálogo con el texto de conservación; estado "Inactiva"; en Profesionales figura "Blanqueamiento (inactiva)" | `capturas/admin-confirmar-desactivacion-390.png`, `capturas/admin-profesionales-1440.png` |
| Administrador | Alta de prestación con duración 0 y después 20 | Error "La duración debe ser mayor que cero" junto al campo, sin llamar a la API; después "Prestación creada" | `capturas/admin-validacion-duracion-390.png`, `capturas/admin-prestaciones-1440.png` |
| Administrador | Editar a Martín Prueba con el email de Laura | 409 de la API mostrado junto al campo Email; el formulario queda abierto | `capturas/admin-conflicto-email-1440.png` |
| Recepción | Sillones y Profesionales | Solo lectura: sin "Nuevo…", sin columna Acciones; el menú muestra Panel y Recursos | `capturas/recepcion-sillones-390.png`, `capturas/recepcion-sillones-1440.png`, `capturas/recepcion-profesionales-1440.png` |

## Consola y red

- No hubo errores de la aplicación en la consola.
- La única entrada de error es el registro del navegador del 409 esperado en el caso de conflicto de email.

## Ajuste hecho a partir de la prueba

- En 1440 px el email del profesional se cortaba a mitad de palabra y los botones de acción ocupaban dos líneas.
- Se agregó `md:whitespace-nowrap` al nombre y al email, y `md:flex-nowrap` a las acciones de las tres pestañas. El celular no cambia.
- Se volvió a capturar `admin-profesionales-1440.png` con el ajuste aplicado.
