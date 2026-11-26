# Evidencia del bloque A de C-15 (sin C-09)

- **Fecha**: 2026-11-26
- **Base**: `main` en `de47e9a`, rama `feat/C-15-privacidad-ley-25326`
- **Máquina**: Windows 11, Temurin 21, Node 22.15.0

## Línea base (tarea 1.1)

| Verificación | Resultado |
|---|---|
| `./mvnw verify` (H2 `MODE=MySQL`) | 442 tests, 0 fallas, 0 errores |

`./mvnw verify -Pit-mysql` no se corrió: el bloque A no toca la base ni migraciones; se corre en el bloque B.

## Después del bloque A

| Verificación | Resultado |
|---|---|
| `./mvnw verify` | 449 tests (442 + 7 nuevos), 0 fallas, 0 errores |
| `npm test` | 17 archivos, 103 tests en verde |
| `npm run lint` | sin advertencias |
| `npm run build` | OK |

## Ciclo TDD

| Tarea | Test | RED | GREEN |
|---|---|---|---|
| 2.1, 2.2 | `AvisoPrivacidadControllerTest` (2) | 404, el endpoint no existía | `AvisoPrivacidadController` + `AvisoPrivacidadRespuesta` |
| 3.1 | `ErroresSinDatosDelInputTest` (2) | No hubo RED: pasó de entrada (test de caracterización); `ManejadorDeErrores` ya no repetía el input | — |
| 3.2 | `HstsDetrasDelProxyTest` (2) | Sin `Strict-Transport-Security` con `X-Forwarded-Proto: https` | `server.forward-headers-strategy: framework` |
| 3.3 | — | `show-sql` no está activado en ningún perfil | — |
| 3.4 (nueva) | `SinValoresDeLaBaseEnLogsTest` (1) | Hibernate (`SqlExceptionHelper`) logueaba el valor repetido de una clave única | Nivel `OFF` para ese logger en `application.yml` |
| 4.1 | `avisoPrivacidadApi.test.ts` (3) | El módulo no existía | `esquemaAvisoPrivacidad` |
| 4.2, 4.3 | `AvisoPrivacidad.test.tsx` (4) | El componente no existía | `AvisoPrivacidad.tsx` + `useAvisoPrivacidad` |

## Desvíos del diseño

- **Nombres del frontend**: el esquema y el hook viven juntos en `features/reserva/avisoPrivacidadApi.ts`. `avisoPrivacidad.ts` choca con `AvisoPrivacidad.tsx` en sistemas de archivos que no distinguen mayúsculas (Windows), y el import resolvía al componente.
- **Hallazgo nuevo (3.4)**: los valores de una clave única duplicada (DNI, email) aparecían en el log de Hibernate. No estaba en el diseño; se agregó la tarea.
- **Tests de backend**: `@SpringBootTest` + MockMvc en vez de `@WebMvcTest`, como el resto del proyecto.

## Pendiente

- 4.4 (revisión a 390 px y con teclado) queda para cuando C-16 incruste el componente en una pantalla real.
