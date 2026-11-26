import type { Prestacion, Profesional, Sillon } from '../features/recursos/api'
import type { UsuarioSesion } from '../features/auth/sesion'
import { sesionDe, type RespuestaSimulada } from './apiSimulada'
import { ADMIN } from './datosUsuarios'

/** Datos ficticios para los tests de recursos (Ley 25.326: nada real). */
export const SILLONES: Sillon[] = [
  { id: 1, nombre: 'Sillón 1', activo: true },
  { id: 2, nombre: 'Sillón 2', activo: false },
]

export const PRESTACIONES: Prestacion[] = [
  { id: 1, nombre: 'Blanqueamiento', duracionMinutos: 90, activa: false },
  { id: 2, nombre: 'Consulta', duracionMinutos: 30, activa: true },
  { id: 3, nombre: 'Extracción', duracionMinutos: 60, activa: true },
  { id: 4, nombre: 'Limpieza', duracionMinutos: 40, activa: true },
]

export const PROFESIONALES: Profesional[] = [
  {
    id: 10,
    nombre: 'Laura',
    apellido: 'Ejemplo',
    matricula: 'MP-0001',
    email: 'laura@example.com',
    activo: true,
    prestacionIds: [1, 4],
  },
  { id: 11, nombre: 'Martín', apellido: 'Prueba', email: 'martin@example.com', activo: false, prestacionIds: [] },
]

type Manejadores = Record<string, RespuestaSimulada | ((cuerpo: unknown) => RespuestaSimulada)>

/** API simulada con la sesión del usuario indicado y los tres listados de recursos. */
export function apiDeRecursos(usuario: UsuarioSesion = ADMIN, extra: Manejadores = {}): Manejadores {
  return {
    'GET /api/auth/yo': { status: 200, cuerpo: usuario },
    'GET /api/sillones': { status: 200, cuerpo: SILLONES },
    'GET /api/prestaciones': { status: 200, cuerpo: PRESTACIONES },
    'GET /api/profesionales': { status: 200, cuerpo: PROFESIONALES },
    ...extra,
  }
}

export const sesionAdmin = { ...sesionDe('administrador'), usuario: ADMIN }
