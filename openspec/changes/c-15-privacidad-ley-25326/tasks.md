# Tasks — C-15 `privacidad-ley-25326`

> **Gobernanza ALTA:** no empezar hasta que Bruno apruebe `design.md` y Renzo confirme el contrato de `paciente` (design D1).
>
> TDD estricto en cada tarea: **RED** (test que falla y se ve fallar), **GREEN** (código mínimo), **REFACTOR** (con tests en verde). Nada requiere Docker ni Testcontainers. Backend desde `backend/` con `./mvnw verify` (H2) y `./mvnw verify -Pit-mysql` (MySQL 8 local); frontend desde `frontend/` con `npm test` y `npm run build`. Evidencia en `openspec/changes/c-15-privacidad-ley-25326/evidencia/`.
>
> **Bloque A** se puede hacer ya, sin C-09. **Bloque B** espera el merge de C-09 a `main` y el rebase de la rama.

## 1. Línea base

- [ ] 1.1 Correr `./mvnw verify`, `./mvnw verify -Pit-mysql`, `npm test` y `npm run build` antes de tocar código y anotar los resultados en `evidencia/linea-base.txt`. Si algo falla, frenar y reportarlo como falla previa

## 2. Bloque A — Aviso público (spec "Aviso de privacidad público")

- [ ] 2.1 RED: `AvisoPrivacidadControllerTest` (`@WebMvcTest`): sin token, `GET /api/publico/aviso-privacidad` → 200 con solo `texto` y `leyenda` y `Cache-Control: no-cache`; el cuerpo no tiene `provisorio`, autor ni fecha. GREEN: `AvisoPrivacidadDto` y controller sobre la lectura tipada de C-07 (design D5)
- [ ] 2.2 RED: `AvisoPrivacidadIntegracionTest` (`@SpringBootTest`): el administrador guarda un texto nuevo por `PUT /api/parametros/aviso-privacidad-texto` y el `GET` público devuelve el nuevo. GREEN: ajustar si hiciera falta

## 3. Bloque A — Logs, errores y HTTPS (spec "Sin datos personales en logs ni en errores", "Transporte cifrado en el despliegue")

- [ ] 3.1 RED: `ErroresSinDatosDelInputTest`: un 400 de Bean Validation con un campo inválido `30111222` no contiene ese valor en el cuerpo. GREEN: quitar `rejectedValue` del `GlobalExceptionHandler` si aparece (design D7)
- [ ] 3.2 RED: `HstsDetrasDelProxyTest` (MockMvc): con `X-Forwarded-Proto: https` la respuesta trae `Strict-Transport-Security`; sin él, no. GREEN: `server.forward-headers-strategy: framework` y HSTS en la configuración de seguridad (design D8)
- [ ] 3.3 Confirmar `spring.jpa.show-sql=false` en todos los perfiles y anotarlo en `evidencia/`

## 4. Bloque A — Frontend (spec "Aceptación obligatoria del aviso al reservar" — UI)

- [ ] 4.1 RED: `avisoPrivacidadEsquema.test.ts`: `true` válido; `false` y ausente inválidos con el mensaje en español. GREEN: esquema Zod (design D9)
- [ ] 4.2 RED: `useAvisoPrivacidad.test.tsx`: con el cliente API simulado devuelve `texto` y `leyenda`; ante error expone el estado de error. GREEN: hook con TanStack Query sobre `lib/apiClient.ts`
- [ ] 4.3 RED: `AvisoPrivacidad.test.tsx` (Testing Library): muestra la leyenda como etiqueta de la casilla; el texto se despliega; al enviar sin marcar muestra el error asociado con `aria-describedby` y mueve el foco; marcada, el formulario de prueba envía `aceptaAvisoPrivacidad: true`. GREEN: componente `AvisoPrivacidad.tsx`
- [ ] 4.4 Revisar el componente a 390 px de ancho y con teclado (orden de foco, contraste); anotar el resultado en `evidencia/accesibilidad.md`

## 5. Bloque B — Esquema (requiere C-09) (spec "Datos mínimos del paciente")

- [ ] 5.1 Verificar contra la migración de C-09 el contrato de design D1 (columnas, largos, `consentimiento_privacidad_en`). Si falta algo, anotarlo y resolverlo con Renzo antes de seguir
- [ ] 5.2 RED: `RestriccionesPrivacidadPacienteBase` con subclases H2 y MySQL (`@Tag("mysql")`): `anonimizado_en` sin `anonimizado_por` (y al revés) se rechaza por `ck_paciente_anonimizado`; `anonimizado_por` inexistente se rechaza por la FK. GREEN: `V015_1__privacidad_paciente.sql` (design D2) y actualizar `db/migration/README.md`
- [ ] 5.3 RED: `PacienteMinimoTest`: las columnas de `paciente` (por `INFORMATION_SCHEMA`) y los campos de la entidad coinciden exactamente con la lista permitida. GREEN: ajustar el mapeo

## 6. Bloque B — Consentimiento (spec "Aceptación obligatoria del aviso al reservar", "Instante del consentimiento registrado")

- [ ] 6.1 RED: `ConsentimientoPrivacidadTest` y `PacienteConsentimientoTest` (dominio puro): `exigir(false)` lanza con código `aviso-privacidad-no-aceptado`; `registrarConsentimiento` guarda el instante y rechaza `null`; una segunda aceptación lo reemplaza. GREEN: dominio (design D3). Verificar `DominioSinFrameworksTest`
- [ ] 6.2 RED: `RegistroDeConsentimientoIntegracionTest` (`@SpringBootTest`, `Clock` fijo): sin transacción lanza `IllegalTransactionStateException`; dentro de una transacción que termina bien guarda `2026-12-01T13:15:00Z`; dentro de una que falla después, no cambia nada. GREEN: servicio con `MANDATORY`
- [ ] 6.3 Documentar en `design.md` y avisar a Jessica (C-16) el contrato: `aceptaAvisoPrivacidad` `@NotNull @AssertTrue` en el DTO y llamada a `registrar(...)` dentro de la transacción de la reserva. Si C-16 ya existe en `main`, agregar el test de reserva rechazada sin aceptación contra su endpoint

## 7. Bloque B — Anonimización (spec "Anonimización del paciente", "Anonimización solo por el administrador", "Sin emails a pacientes anonimizados")

- [ ] 7.1 RED: `PacienteAnonimizarTest` (dominio): reemplaza los cinco datos por los sustitutos de design D4, guarda usuario e instante, conserva el consentimiento y una segunda llamada no cambia nada. GREEN: `Paciente.anonimizar`
- [ ] 7.2 RED: `ServicioDeAnonimizacionTest` (unitario): con un turno futuro `pendiente` o `confirmado` lanza conflicto y no anonimiza; con turnos pasados o cancelados anonimiza; id inexistente → no encontrado. GREEN: servicio con bloqueo del paciente y puerto de lectura de turnos de C-09
- [ ] 7.3 RED: `PacienteAdminControllerTest` (`@WebMvcTest`): administrador → 200 con `{id, anonimizadoEn}`; repetido → 200 igual; conflicto → 409 RFC 9457; inexistente → 404; `recepcion` y `profesional` → 403; sin token → 401; en los rechazos el servicio no se invoca. GREEN: controller con `@PreAuthorize`
- [ ] 7.4 RED: `AnonimizacionIntegracionTest` (`@SpringBootTest`, H2): después de anonimizar, el DNI original reserva y crea un paciente nuevo; los turnos pasados y su `turno_historial` siguen. GREEN: ajustar
- [ ] 7.5 RED: test de la consulta de destinatarios que usen C-14/C-18 (o, si aún no existen, del método `Paciente.puedeRecibirEmails()`): un paciente anonimizado queda excluido. GREEN: implementar y avisar a Jessica (C-14) y a la tarea de C-18

## 8. Bloque B — Logs (spec "Sin datos personales en logs ni en errores")

- [ ] 8.1 RED: `SinDatosPersonalesEnLogsTest` con `ListAppender`: una reserva rechazada por falta de aceptación y una anonimización no dejan en los logs el DNI, email, teléfono, nombre ni apellido usados. GREEN: ajustar los mensajes (design D7)

## 9. Cierre

- [ ] 9.1 Correr `./mvnw verify`, `./mvnw verify -Pit-mysql`, `npm test` y `npm run build`; guardar los resultados en `evidencia/` (solo líneas de resultado, sin URL ni credenciales). Verificar exit 0 en todos
- [ ] 9.2 Actualizar `knowledge-base/08_arquitectura_propuesta.md` §Seguridad con la política de retención y anonimización (design D6) y el endpoint en la tabla de rutas si existe
- [ ] 9.3 Correr `openspec validate c-15-privacidad-ley-25326 --strict` y anotar la tabla escenario → test en `evidencia/trazabilidad.md`. Ningún escenario sin test
