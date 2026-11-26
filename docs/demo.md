# Demo de DentaFlow — plan B local

> **Cuándo se usa:** si el jueves 2026-11-26 a las **11:00** el entorno desplegado (Vercel + Render + Aiven) no muestra los flujos del corte funcionando, se presenta esta demo local (DD-12, C-19 design D8). No se sigue depurando la nube después de esa hora.

## 1. Antes de la presentación (una sola vez por máquina)

1. Seguir los pasos 1 y 2 del [README](../README.md): base `dentaflow`, usuario de la aplicación y archivos `.env` (raíz y `frontend/`).
2. Traer `main` actualizado: `git switch main` y `git pull`.
3. Ensayar el arranque completo con el script (sección 2) y recorrer el guion (sección 3) de punta a punta. Anotar el tiempo en `openspec/changes/c-19-cierre-mvp-e2e-y-despliegue/evidencia/ensayo-plan-b.md`.

Si la base local tiene datos de pruebas anteriores que ensucian la demo, recrearla (se pierden esos datos): borrar y crear la base `dentaflow` como en el paso 1 del README; Flyway y el seed de demo la vuelven a llenar al arrancar.

## 2. Arranque (el día de la demo)

En PowerShell, desde la raíz del repo:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\demo-local.ps1
```

El script verifica JDK 21, Node 22+, MySQL y los `.env` (sin mostrar valores), abre dos ventanas (`DentaFlow backend` y `DentaFlow frontend`), espera a que la API responda `UP` y abre <http://localhost:5173>.

Para cortar todo al terminar:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\demo-local.ps1 -Detener
```

| Si pasa esto | Hacer esto |
|---|---|
| "No encontré un JDK 21" | Instalar Temurin 21 o definir `JAVA_HOME` apuntando a un JDK 21 |
| "El servicio MySQL… está detenido" | Abrir PowerShell como administrador y correr `Start-Service MySQL84` (o el nombre que tenga el servicio) |
| "En .env faltan valores" | Completar esas variables en `.env` (ver `.env.example`) |
| "El puerto 8080/5173 está ocupado" | Correr el script con `-Detener` o cerrar la ventana que quedó abierta |
| El backend no llega a `UP` | Mirar la ventana `DentaFlow backend`: casi siempre es la contraseña de la base en `.env` |

## 3. Guion de la demo

Datos de paciente para la demo, **siempre ficticios** (Ley 25.326): Ana Prueba, DNI `30111222`, teléfono `1155550000`, email del equipo para ver los mensajes. Las credenciales del administrador son las de `APP_ADMIN_EMAIL` y `APP_ADMIN_PASSWORD` del `.env` de la máquina de la demo: no se escriben en este documento.

Cada paso indica el change que lo habilita. Antes del ensayo, tachar los que no estén en `main`.

| # | Paso | Qué se muestra | Change |
|---|------|----------------|--------|
| 1 | Abrir <http://localhost:5173> en el celular emulado (DevTools, 390 × 844) | Portada mobile-first | C-02 |
| 2 | Reservar online: prestación, profesional o "cualquiera", hueco libre, datos de Ana, aceptar el aviso de privacidad | Reserva sin cuenta, aviso obligatorio, turno `pendiente` | C-10, C-15, C-16 |
| 3 | Intentar reservar sin aceptar el aviso | La reserva se rechaza | C-15 |
| 4 | Abrir el email de confirmación y tocar "Confirmar" | El turno pasa a `confirmado` | C-14, C-17 |
| 5 | Reprogramar desde el enlace | Solo ofrece huecos libres; respeta la anticipación mínima (en 0 h para la demo, PA-04) | C-17 |
| 6 | Cancelar desde el enlace | El hueco vuelve a quedar libre | C-17 |
| 7 | Ingresar al panel como administrador (`/ingresar`) | Ingreso con JWT y menú según el rol | C-04 |
| 8 | Recursos: profesionales, sillones y prestaciones (`/panel/recursos`) | Alta y edición de recursos del consultorio | C-03, C-05 |
| 9 | Horarios de atención y un bloqueo sobre un turno existente | El bloqueo avisa y no cancela el turno; impide reservas nuevas en esa franja (PA-11) | C-06 |
| 10 | Usuarios: alta de una recepcionista (`/panel/usuarios`) y entrar con ella | La recepción no ve la gestión de usuarios: RBAC verificado en el backend | C-04 |
| 11 | Parámetros: anticipación mínima y texto del aviso de privacidad | Cambio visible al instante en la reserva | C-07 |
| 12 | Diferencial: dos reservas al mismo hueco | Solo una gana: no hay solapamiento de profesional ni de sillón | C-09, C-16 |

Para el paso 12 alcanza con abrir la reserva en dos ventanas y confirmar el mismo hueco casi a la vez; la segunda recibe el aviso de que el horario ya no está disponible.

## 4. Checklist 10 minutos antes

- [ ] `main` actualizado y backend y frontend levantados con el script
- [ ] <http://localhost:8080/actuator/health> responde `{"status":"UP"}`
- [ ] Ingreso del administrador probado
- [ ] Anticipación mínima en 0 h si se va a mostrar la reprogramación (PA-04)
- [ ] Hay huecos libres hoy y mañana para la prestación de la demo
- [ ] Bandeja del email del equipo abierta (si C-14 envía por SMTP real) o la vista de enlaces del adaptador simulado
- [ ] DevTools con la vista de celular preparada
- [ ] Notificaciones del sistema silenciadas
