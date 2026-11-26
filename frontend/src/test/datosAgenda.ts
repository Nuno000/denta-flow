import { RUTA_AGENDA, type TurnoDeAgenda } from '../features/agenda/api'
import type { UsuarioSesion } from '../features/auth/sesion'
import { sesionDe, type RespuestaSimulada } from './apiSimulada'
import { apiDeRecursos } from './datosRecursos'
import { ADMIN } from './datosUsuarios'

/**
 * Turnos ficticios para los tests de la agenda (Ley 25.326: nombres genéricos, sin DNI ni contacto).
 * Profesionales y sillones son los de `datosRecursos`: Laura Ejemplo (10), Martín Prueba (11), Sillón 1 y 2.
 */
const LAURA = { id: 10, nombre: 'Laura', apellido: 'Ejemplo' }
const MARTIN = { id: 11, nombre: 'Martín', apellido: 'Prueba' }
const SILLON_1 = { id: 1, nombre: 'Sillón 1' }
const SILLON_2 = { id: 2, nombre: 'Sillón 2' }
const CONSULTA = { id: 2, nombre: 'Consulta' }
const LIMPIEZA = { id: 4, nombre: 'Limpieza' }

function turno(datos: Partial<TurnoDeAgenda> & Pick<TurnoDeAgenda, 'id' | 'inicio' | 'fin'>): TurnoDeAgenda {
  return {
    estado: 'confirmado',
    origen: 'online',
    esSobreturno: false,
    paciente: { nombre: 'Paciente', apellido: `Ficticio ${datos.id}` },
    prestacion: CONSULTA,
    profesional: LAURA,
    sillon: SILLON_1,
    ...datos,
  }
}

/** Martes 1/12/2026, hora del consultorio (UTC-3), en el orden en que los devuelve el backend. */
export const TURNOS_DEL_1_12: TurnoDeAgenda[] = [
  turno({
    id: 1,
    inicio: '2026-12-01T12:00:00Z', // 09:00
    fin: '2026-12-01T12:30:00Z',
    estado: 'confirmado',
    paciente: { nombre: 'Ana', apellido: 'Ejemplar' },
  }),
  turno({
    id: 2,
    inicio: '2026-12-01T13:00:00Z', // 10:00
    fin: '2026-12-01T13:40:00Z',
    estado: 'pendiente',
    prestacion: LIMPIEZA,
    profesional: MARTIN,
    sillon: SILLON_2,
    paciente: { nombre: 'Bruno', apellido: 'Muestra' },
  }),
  turno({
    id: 3,
    inicio: '2026-12-01T13:00:00Z', // 10:00, sobreturno
    fin: '2026-12-01T13:30:00Z',
    estado: 'asistio',
    esSobreturno: true,
    origen: 'recepcion',
    paciente: { nombre: 'Carla', apellido: 'Modelo' },
  }),
  turno({ id: 4, inicio: '2026-12-01T17:00:00Z', fin: '2026-12-01T17:30:00Z', estado: 'ausente' }),
  turno({ id: 5, inicio: '2026-12-01T18:00:00Z', fin: '2026-12-01T18:30:00Z', estado: 'ausente_con_aviso' }),
  // 23:30 del 1/12 en el consultorio, ya 2/12 en UTC.
  turno({ id: 6, inicio: '2026-12-02T02:30:00Z', fin: '2026-12-02T03:00:00Z', estado: 'confirmado' }),
]

export const TURNO_CANCELADO = turno({
  id: 7,
  inicio: '2026-12-01T14:00:00Z',
  fin: '2026-12-01T14:30:00Z',
  estado: 'cancelado',
})

/** Turno del jueves 3/12 para la vista semanal. */
export const TURNO_DEL_3_12 = turno({
  id: 8,
  inicio: '2026-12-03T11:00:00Z', // 08:00
  fin: '2026-12-03T11:30:00Z',
  profesional: MARTIN,
  sillon: SILLON_2,
})

type Manejador = RespuestaSimulada | ((cuerpo: unknown) => RespuestaSimulada)

/** API simulada con la sesión, los recursos y la agenda (por defecto, los turnos del 1/12). */
export function apiDeAgenda(
  usuario: UsuarioSesion = ADMIN,
  agenda: Manejador = { status: 200, cuerpo: TURNOS_DEL_1_12 },
): Record<string, Manejador> {
  return apiDeRecursos(usuario, { [`GET ${RUTA_AGENDA}`]: agenda })
}

export const sesionRecepcion = sesionDe('recepcion')
export const sesionProfesional = sesionDe('profesional')

/** Parámetros de consulta de cada pedido a la agenda que recibió la API simulada. */
export function consultasDeAgenda(fetchSimulado: { mock: { calls: unknown[][] } }): URLSearchParams[] {
  return fetchSimulado.mock.calls
    .map(([url]) => new URL(String(url), 'http://localhost'))
    .filter((url) => url.pathname === RUTA_AGENDA)
    .map((url) => url.searchParams)
}
