# Ensayo del plan B local (tarea 4.1)

- **Fecha**: 2026-11-26, madrugada
- **Máquina**: notebook de Bruno (Windows 11, MySQL 8.4 como servicio `MySQL84`, Temurin 21, Node 22.15.0)
- **Base de `main`**: `de47e9a` (C-02..C-08 y C-05 mergeados; todavía sin C-09)
- **Punto de partida**: base `dentaflow` y `.env` ya creados (pasos 1 y 2 del README hechos antes). No es un arranque desde una máquina limpia.

## Resultado

| Intento | Resultado | Tiempo |
|---|---|---|
| 1 | Backend `UP` en 20 s; el script no detectó el frontend aunque estaba arriba | 122 s (agotó la espera) |
| 2 (con el arreglo) | Backend `UP` en 12 s, frontend 200 en <http://localhost:5173>, `POST /api/auth/login` responde (400 con cuerpo vacío) | 15 s |
| `-Detener` | 8080 y 5173 libres; el archivo de PID se borra | — |

**Problema encontrado en el intento 1**: Vite (Node 17+) escucha solo en `::1` y el script probaba `127.0.0.1`. Se corrigió probando IPv4 e IPv6.

## Pendiente

- Recorrer el guion de `docs/demo.md` en el navegador cuando estén C-09..C-17 en `main`.
- Repetir el ensayo en la máquina con la que se va a presentar, si no es esta.
