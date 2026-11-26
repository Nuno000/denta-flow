# Matriz de Trazabilidad — C-16 `reserva-online-publica`

Fecha: 2026-11-26

| Requisito / Escenario | Especificación | Test de cobertura |
|---|---|---|
| Parámetro `contacto-consultorio` | `parametros-consultorio` | `ParametrosControllerTest`, `ClaveParametroTest`, `ServicioDeParametrosTest` |
| `GET /api/publico/contacto` sin token | `parametros-consultorio` | `ContactoPublicoControllerTest` |
| `GET /api/publico/prestaciones` (solo activas) | `reserva-online` | `CatalogoPublicoControllerTest` |
| `GET /api/publico/profesionales?prestacionId=` | `reserva-online` | `CatalogoPublicoControllerTest` |
| Límite anti-abuso 5 pedidos / 10 min por IP | `reserva-online` | `LimitadorDeReservasPorOrigenTest`, `ManejadorDeErroresTest`, `ReservasPublicasControllerTest` |
| Orquestación de reserva online (sin `@Transactional`) | `reserva-online` | `ServicioDeReservaOnlineTest` |
| `POST /api/publico/reservas` (201 Created) | `reserva-online` | `ReservasPublicasControllerTest` |
| Validación de datos del paciente y aviso obligatorio | `reserva-online` | `ReservasPublicasControllerTest`, `esquemasReserva.test.ts` |
| Errores sin datos personales en respuestas ni logs | `reserva-online` | `ReservasPublicasControllerTest`, `LogsDeReservaPublicaTest` |
| Anti-solapamiento y concurrencia MySQL 8 real | `reserva-online` | `ReservaPublicaConcurrenciaMySqlTest` |
| Pantalla `/reservar` mobile-first accesible | `reserva-online` | `ReservaPage.test.tsx`, `reservaApi.test.tsx`, `rutas.test.tsx` |
