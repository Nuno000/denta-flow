# Tasks — C-20 `lista-de-espera`

> **Gobernanza MEDIA** (`CHANGES.md`). TDD estricto: para cada comportamiento, escribir primero el test que falla (RED), luego la implementación mínima (GREEN) y finalmente refactorizar (REFACTOR). Sin Docker ni Testcontainers (`DD-09`).

## 1. Migración y parámetro configurable

- [x] 1.1 RED/GREEN: Agregar `LISTA_ESPERA_PLAZO_OFERTA_HORAS` (`lista-espera-plazo-oferta-horas`, entero `1..72`) en `ClaveParametro` y actualizar los tests unitarios/integración de `parametros-consultorio` (catálogo con 6 claves y validación de rango). Verificar con `./mvnw test -Dtest=*Parametro*`.
- [x] 1.2 Crear migración `V020_1__crear_lista_de_espera.sql` (tabla `lista_espera`, constraints, índices e `INSERT` del parámetro `lista-espera-plazo-oferta-horas` con valor provisorio `4`). Verificar arranque de Flyway sobre H2 (`./mvnw test -Dtest=DentaFlowApplicationTests`).

## 2. Dominio y persistencia `ListaEspera`

- [x] 2.1 RED/GREEN: Crear `EstadoListaEspera`, `ConversorEstadoListaEspera` y la entidad `ListaEspera` en `dominio/listaespera/` con validaciones de rango de fechas/horas (`America/Argentina/Buenos_Aires`) y transiciones de estado (`activa -> ofrecida | vencida`, `ofrecida -> atendida | vencida`). Verificar con test unitario de dominio.
- [x] 2.2 RED/GREEN: Crear `ListaEsperaRepositorio` y `ListaEsperaRepositorioJpa` (`infraestructura/persistencia/listaespera/`) incluyendo búsqueda de compatibles activas, `buscarPorTokenParaActualizar` (`PESSIMISTIC_WRITE`), `vencerOtrasOfertasDelHueco` y `vencerExpiradas`. Verificar con `@DataJpaTest`.

## 3. Gestión interna (Recepción / Administrador)

- [x] 3.1 RED/GREEN: Implementar `ServicioDeListaDeEspera` (`registrar`, `listar`, `darDeBaja`, `vencerOfertasExpiradas`) validando paciente no anonimizado, prestación activa, profesional habilitado y estado `activa` al dar de baja. Verificar con tests unitarios/integración.
- [x] 3.2 RED/GREEN: Implementar `ListaEsperaController` (`/api/lista-espera`, `GET`, `POST`, `DELETE /{id}`) protegido con `@AdministradorORecepcion` y verificar RBAC (`401` sin sesión, `403` rol `profesional`, `200`/`201` para `recepcion` y `administrador`) con MockMvc.

## 4. Oferta automática al cancelar un turno (`RN-CA-04`)

- [x] 4.1 RED/GREEN: Publicar `TurnoCanceladoEvent` desde `ServicioDeTurnos.cambiarEstado` cuando el nuevo estado es `CANCELADO` (sin alterar la transacción si no hay lista de espera). Verificar que cancelar un turno sigue pasando los tests existentes de `ServicioDeTurnos`.
- [x] 4.2 RED/GREEN: Implementar `OferenteDeListaDeEspera` (`@TransactionalEventListener(AFTER_COMMIT)` + `REQUIRES_NEW`) que verifica disponibilidad real con `MotorDeDisponibilidad`, marca entradas compatibles (no anonimizadas) como `ofrecida` con token seguro y `ofrecidaHasta`, y programa el email. Verificar con tests de integración (compatibles, incompatible por profesional/rango, hueco bloqueado no ofrecido, paciente anonimizado excluido).
- [x] 4.3 RED/GREEN: Implementar `PlantillaOfertaListaEspera` y `ServicioDeNotificacionesListaEspera` usando `EjecutorDeEmails` y `EnviadorDeEmail` (enlace `{APP_BASE_URL}/lista-espera/aceptar?token={token}` y horario en `America/Argentina/Buenos_Aires`), asegurando que un fallo de envío no revierte la cancelación. Verificar con tests unitarios.

## 5. Aceptación pública, concurrencia y vencimiento

- [x] 5.1 RED/GREEN: Implementar `AceptadorDeOfertaListaEspera` (`consultarPorToken`, `aceptar`) con bloqueo `FOR UPDATE` sobre la entrada, validación de vencimiento (`ofrecidaHasta > ahora`), selección de sillón vía `MotorDeDisponibilidad` y creación transaccional vía `ServicioDeTurnos.crear` (bloqueo `profesional -> sillón`), marcando la ganadora como `atendida` y el resto de ofertas del hueco como `vencida`. Verificar casos: aceptación exitosa, oferta vencida, hueco ya tomado (`409`).
- [x] 5.2 RED/GREEN: Implementar `ListaEsperaPublicoController` (`GET /api/publico/lista-espera/{token}` y `POST /api/publico/lista-espera/{token}/aceptar`) y habilitar `/api/publico/lista-espera/**` en `ConfiguracionSeguridad`. Verificar con MockMvc.
- [x] 5.3 RED/GREEN: Agregar test de concurrencia `@Tag("mysql")` (`AceptacionListaEsperaConcurrenciaMySqlTest`) verificando que dos hilos aceptando simultáneamente la misma oferta o dos ofertas del mismo hueco producen exactamente 1 turno creado y 1 `409 Conflict`, sin solapamiento. Verificar con `./mvnw verify -Pit-mysql`.

## 6. Frontend (`frontend/src/features/lista-espera/`)

- [x] 6.1 RED/GREEN: Crear cliente API (`api.ts`), pantalla interna `ListaEsperaPage.tsx` (`/panel/lista-de-espera`: listado filtrable por estado, alta con búsqueda de paciente por DNI y baja de activas) y enlace en `BarraDelPanel.tsx` visible para `administrador` y `recepcion`. Verificar con Vitest + Testing Library.
- [x] 6.2 RED/GREEN: Crear pantalla pública `AceptarOfertaPage.tsx` (`/lista-espera/aceptar` leyendo `?token=`) con estados vigente, confirmada, ya tomada (`409`), vencida y enlace inválido, y registrar la ruta en `rutas.tsx`. Verificar con Vitest + Testing Library y `rutas.test.tsx`.

## 7. Verificación integral

- [x] 7.1 Ejecutar suite completa backend (`./mvnw verify`), concurrencia MySQL (`./mvnw verify -Pit-mysql`), frontend (`npm test` y `npm run build`), y `openspec validate c-20-lista-de-espera --strict`.

## Workflow follow-up

- Actualizar el estado de `C-20` en `CHANGES.md` al completar la implementación y archivar con `openspec-archive-change` tras la revisión del PR.
