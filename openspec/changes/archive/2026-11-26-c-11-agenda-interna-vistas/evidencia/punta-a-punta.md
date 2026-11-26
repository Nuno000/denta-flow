# Prueba de punta a punta de C-11 contra el backend real (tarea 6.7)

- **Fecha:** 2026-11-26, rama `feat/C-11-agenda-interna-vistas` con `origin/main` mergeado (C-06, C-09 y C-15).
- **Backend real:** `./mvnw spring-boot:run` con el perfil `test` y su clave de firma de prueba (no es un secreto). Base H2 en archivo, `target/e2e-agenda`, con `MODE=MySQL` y `AUTO_SERVER`. Seed de demo activo (profesionales, sillones y prestaciones) y un administrador **ficticio** pasado por variables de entorno del proceso. No se leyó ni se usó el `.env` del equipo ni la base MySQL local.
- **Frontend real:** `vite` con `VITE_API_BASE_URL=http://localhost:8080`, **sin `simularApi` ni intercepción de red**. Ingreso real por `/ingresar`.
- **Turnos:** ficticios (Ana Prueba, Bruno Muestra, Carla Modelo y un paciente anonimizado), cargados por SQL en la base H2 desde una segunda conexión. Todavía no hay endpoint para crear turnos, porque eso llega con C-12 y C-16.
  - Los instantes se cargaron en la representación que usa Hibernate (ver design D7, gotcha de zona horaria en las cargas por JDBC).
  - El usuario profesional se creó por `POST /api/usuarios`, vinculado a Laura Ejemplo.
- Al terminar se detuvieron backend y frontend y se borraron la base H2 y la carpeta `.playwright-mcp`.

## Resultados

| Verificación | Resultado |
|---|---|
| `GET /api/agenda/turnos` como administrador, día 26/11 | 5 turnos: 09:00, 10:00 (sobreturno, Laura Ejemplo), 10:00 (Martín Prueba), 14:00 (paciente anonimizado) y 23:30. El cancelado de las 11:00 no aparece |
| Orden a igual hora | Por apellido del profesional: "Ejemplo" antes que "Prueba" |
| Turno de 02:30Z del 27/11 | Aparece el 26/11 como "23:30–00:00" |
| Paciente anonimizado (C-15) | Solo "Paciente Anonimizado". Ni el email sustituto ni el teléfono llegan al JSON |
| Sin token | 401 |
| Profesional (Laura) sin filtro | Solo sus 3 turnos |
| Profesional con `profesionalId=2` en la API | 403 en formato de problema, sin turnos |
| Profesional con `?profesionalId=2` en la URL de la pantalla | La UI no lo envía y sigue mostrando solo sus 3 turnos |
| "Mi agenda" a 390 px | Sin scroll horizontal (`scrollWidth` 375 = ancho visible) |
| Pedidos fallidos y errores de consola durante el recorrido | 0 y 0 |
| Cambios necesarios en `frontend/src/features/agenda/api.ts` | **Ninguno**: el contrato simulado coincidió con el real |

## Capturas

- `capturas/e2e-real-admin-dia-1440.png`: vista diaria del administrador con datos reales.
- `capturas/e2e-real-admin-semana-1440.png`: vista semanal del administrador.
- `capturas/e2e-real-profesional-390.png`: "Mi agenda" de la profesional en el celular.
