# DentaFlow — Base de Conocimiento

Base de conocimiento generada a partir de los documentos de discovery del proyecto (`docs/discovery/discovery.md` e `informe-discovery.md`).

## Índice de Archivos

| Archivo | Contenido |
|---------|-----------|
| [01_vision_y_objetivos.md](01_vision_y_objetivos.md) | Propósito, objetivos por actor, alcance MVP, fuera de alcance |
| [02_descripcion_general.md](02_descripcion_general.md) | Stack (decidido, DD-08 y DD-09), arquitectura lógica, integraciones |
| [03_actores_y_roles.md](03_actores_y_roles.md) | Actores, matriz RBAC, rutas públicas |
| [04_modelo_de_datos.md](04_modelo_de_datos.md) | Entidades conceptuales, relaciones, seed |
| [05_reglas_de_negocio.md](05_reglas_de_negocio.md) | Reglas RN-XX por dominio |
| [06_funcionalidades.md](06_funcionalidades.md) | Historias de usuario por épica |
| [07_flujos_principales.md](07_flujos_principales.md) | Flujos extremo a extremo |
| [08_arquitectura_propuesta.md](08_arquitectura_propuesta.md) | Patrones, seguridad, estructura del monorepo, variables de entorno, migraciones y pruebas (stack DD-08 y DD-09) |
| [09_decisiones_y_supuestos.md](09_decisiones_y_supuestos.md) | Decisiones DD y supuestos SU |
| [10_preguntas_abiertas.md](10_preguntas_abiertas.md) | Inconsistencias y preguntas abiertas priorizadas |

## Quick Start para Desarrolladores

1. Entender el dominio -> [01](01_vision_y_objetivos.md), [03](03_actores_y_roles.md)
2. Entender los datos -> [04](04_modelo_de_datos.md)
3. Entender las reglas -> [05](05_reglas_de_negocio.md)
4. Entender la arquitectura -> [02](02_descripcion_general.md), [08](08_arquitectura_propuesta.md)
5. Implementar -> [07](07_flujos_principales.md), [06](06_funcionalidades.md)
6. Antes de codificar -> [10](10_preguntas_abiertas.md)

## Resumen Ejecutivo

DentaFlow es un sistema web de turnos y agenda para consultorios odontológicos chicos de Argentina (una sede, 1 a 5 profesionales). Su núcleo es la prevención real de solapamientos por profesional y por sillón, la reserva online sin registro, y el ciclo recordatorio-confirmación. Es un trabajo final de UTN (equipo de 4). El stack quedó decidido el 2026-11-23 y se actualizó el 2026-11-24 (DD-08 y DD-09: Java 21 + Spring Boot 3, MySQL 8 sin Docker, React + Vite + TypeScript). La entrega es el 2026-11-26 a las 12:00 con un corte de alcance (DD-10). WhatsApp, elección de profesional, asignación de sillón y estado inicial del turno ya están resueltos. Siguen abiertas, entre otras, la anticipación mínima (PA-04), los recordatorios (PA-07) y el hosting de MySQL (PA-14); ver `10_preguntas_abiertas.md`.
