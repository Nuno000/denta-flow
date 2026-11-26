# Tasks — C-18 `recordatorios-programados`

> **Gobernanza ALTA:** no empezar hasta que Bruno apruebe `design.md`, el equipo confirme PA-07 (un recordatorio, 24 h, configurable) y Jessica confirme el contrato de `notificacion` (design D1).
>
> TDD estricto en cada tarea: **RED**, **GREEN**, **REFACTOR**. Nada requiere Docker ni Testcontainers. Desde `backend/`: `./mvnw verify` (H2) y `./mvnw verify -Pit-mysql` (MySQL 8 local). Evidencia en `openspec/changes/c-18-recordatorios-programados/evidencia/`.
>
> **Bloque A** se puede hacer ya. **Bloque B** espera C-09, C-14 y C-15 en `main`.

## 1. Línea base

- [ ] 1.1 Correr `./mvnw verify` y `./mvnw verify -Pit-mysql` antes de tocar código y anotar los resultados en `evidencia/linea-base.txt`

## 2. Bloque A — Parámetro (spec `parametros-consultorio` modificada)

- [ ] 2.1 RED: ampliar los tests de C-07: `ClaveParametroTest` reconoce `recordatorio-horas-antes` con rango 1..168; `GET /api/parametros` devuelve 4; `PUT` con `0` y `169` → 400; la lectura tipada devuelve `Duration.ofHours(48)` cuando vale 48; la clave desconocida de ejemplo pasa a `recordatorios-por-turno`. GREEN: constante nueva y lectura tipada (design D2)
- [ ] 2.2 RED: "Base recién migrada" y "Catálogo y base coinciden" esperan 4 filas provisorias, la nueva en 24. GREEN: `V018_1__recordatorio_parametro_y_notificacion.sql` (por ahora solo el `INSERT`) y `db/migration/README.md`

## 3. Bloque A — Credencial de tarea (spec "Disparo externo protegido por credencial de tarea", `autorizacion-por-rol` modificada)

- [ ] 3.1 RED: `CredencialDeTareaFilterTest` (unitario): token correcto autentica con `TAREA`; ausente, distinto, de otro largo o con la variable vacía o de menos de 32 caracteres → 401 sin seguir la cadena. GREEN: filtro con `MessageDigest.isEqual` (design D7)
- [ ] 3.2 RED: `TareasSeguridadTest` (`@SpringBootTest` + MockMvc, con un `EnviarRecordatorios` simulado): con el header correcto → 200 y la operación se invoca una vez; sin header, con header incorrecto o solo con el JWT de un administrador → 401 RFC 9457 y la operación no se invoca; el resto de la API sigue exigiendo JWT. GREEN: `SecurityFilterChain` de `/api/tareas/**` y `TareasController`
- [ ] 3.3 RED: `TareasSinTokenConfiguradoTest`: sin `APP_TAREAS_TOKEN` el backend arranca, el endpoint responde 401 y el log de arranque tiene un `WARN` que no contiene ningún valor secreto. GREEN: ajustar
- [ ] 3.4 Agregar `APP_TAREAS_TOKEN` a `.env.example` (valor de ejemplo no real) y a `knowledge-base/08_arquitectura_propuesta.md` §Variables de entorno

## 4. Bloque A — Programador local (spec "Disparo local programado")

- [ ] 4.1 RED: `ProgramadorDeRecordatoriosTest`: en el perfil `test` el bean no existe; con `app.recordatorios.programador-habilitado=true` existe y llama a `ejecutar()`. GREEN: programador y configuración condicionada (design D8); `true` en `application-dev.yml`

## 5. Bloque B — Esquema de notificaciones (requiere C-14)

- [ ] 5.1 Verificar la migración de C-14 contra design D1. Lo que falte (`intentos`, `actualizada_en`, estados `en_curso`/`omitida`, `UNIQUE (turno_id, tipo)`) se agrega en `V018_1` antes de mergear (todavía no está en `main`)
- [ ] 5.2 RED: `RestriccionesNotificacionRecordatorioBase` H2 + MySQL (`@Tag("mysql")`): una segunda fila `(turno, 'recordatorio')` se rechaza por la clave única. GREEN: migración

## 6. Bloque B — Dominio y selección (spec "Selección de turnos a recordar")

- [ ] 6.1 RED: `PoliticaDeRecordatorioTest` (dominio puro): ventana `(ahora, ahora + antelación]`; `puedeReintentar` verdadero para `fallida` con 1 o 2 intentos y para `en_curso` vencida, falso con 3 intentos y para `en_curso` de menos de 15 min o `enviada`. GREEN: design D5
- [ ] 6.2 RED: `SeleccionDeRecordatoriosRepositorioTest` (`@DataJpaTest`, H2): con turnos de prueba cubre cada escenario de la spec (dentro, fuera, borde exacto, ya empezado, cada estado no pendiente, paciente anonimizado, con recordatorio enviado, con fallido reintentable y con 3 intentos). GREEN: consulta de design D3

## 7. Bloque B — Envío idempotente (spec "Un solo recordatorio por turno", "Fallos de envío registrados y reintentados", "Contenido del recordatorio")

- [ ] 7.1 RED: `EnviarRecordatoriosTest` (`@SpringBootTest`, H2, `Clock` fijo, `EnviadorDeEmail` simulado): dos ejecuciones seguidas envían un solo email y dejan una notificación `enviada` con el instante del reloj; un turno confirmado entre la selección y el envío queda `omitida`; un fallo en el primer turno no impide el segundo; reintento exitoso → `enviada` con 2 intentos; con 3 intentos no se reintenta; `en_curso` de hace 16 min se reintenta. GREEN: reclamo y marcas de design D4 y D6
- [ ] 7.2 RED: `PlantillaRecordatorioTest`: un turno del `2026-12-02T13:00:00Z` muestra 02/12/2026 10:00, prestación, profesional, enlace de confirmar y de cancelar, y no contiene el DNI. GREEN: completar los datos que se pasan a la plantilla de C-14
- [ ] 7.3 RED: `EnviarRecordatoriosConcurrenciaMySqlTest` (`@Tag("mysql")`, perfil `it-mysql`): dos hilos ejecutan a la vez con 5 turnos en la ventana y un `EnviadorDeEmail` que cuenta llamadas; cada turno recibe exactamente un envío. GREEN: ajustar el reclamo si falla
- [ ] 7.4 RED: `SinDatosPersonalesEnLogsRecordatoriosTest` (`ListAppender`): ni el resumen ni los `WARN` de fallo contienen emails, nombres ni teléfonos de los pacientes de prueba. GREEN: ajustar los mensajes

## 8. Cierre

- [ ] 8.1 Documentar en `backend/README.md` cómo disparar a mano (`curl -X POST -H "X-Tarea-Token: ..."`) y la configuración del cron externo de design D9 para C-19
- [ ] 8.2 Actualizar `knowledge-base/10_preguntas_abiertas.md` (PA-07 resuelta con la decisión del equipo) y `knowledge-base/07_flujos_principales.md` §Flujo 2 (disparador: cron externo cada 15 min, antelación configurable)
- [ ] 8.3 Correr `./mvnw verify` y `./mvnw verify -Pit-mysql`; guardar los resultados en `evidencia/` sin credenciales. Verificar exit 0
- [ ] 8.4 Correr `openspec validate c-18-recordatorios-programados --strict` y anotar la tabla escenario → test en `evidencia/trazabilidad.md`
