/**
 * Único punto de contacto de la agenda con la API (C-11, design D1 y D3). Los componentes usan solo
 * `useTurnosDeAgenda`; si el backend real cambiara un nombre de campo, el ajuste queda en este archivo.
 */
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ErrorApi, apiClient } from '../../lib/apiClient'
import type { RangoAgenda } from './fechas'

export const RUTA_AGENDA = '/api/agenda/turnos'

/** Estados del turno (RN-TU-05), en el mismo orden en que se ofrecen en el filtro. */
export const ESTADOS_TURNO = ['pendiente', 'confirmado', 'asistio', 'ausente', 'ausente_con_aviso', 'cancelado'] as const

export type EstadoTurno = (typeof ESTADOS_TURNO)[number]

export const NOMBRES_DE_ESTADO: Record<EstadoTurno, string> = {
  pendiente: 'Pendiente',
  confirmado: 'Confirmado',
  asistio: 'Asistió',
  ausente: 'Ausente',
  ausente_con_aviso: 'Ausente con aviso',
  cancelado: 'Cancelado',
}

export function esEstadoTurno(valor: string): valor is EstadoTurno {
  return (ESTADOS_TURNO as readonly string[]).includes(valor)
}

export type OrigenTurno = 'online' | 'recepcion'

/** Un turno tal como lo devuelve `GET /api/agenda/turnos`: sin DNI, teléfono, email ni token (Ley 25.326). */
export interface TurnoDeAgenda {
  id: number
  /** Instantes en UTC (ISO-8601). */
  inicio: string
  fin: string
  estado: EstadoTurno
  origen: OrigenTurno
  esSobreturno: boolean
  paciente: { nombre: string; apellido: string }
  prestacion: { id: number; nombre: string }
  profesional: { id: number; nombre: string; apellido: string }
  sillon: { id: number; nombre: string }
}

/** Filtros opcionales; sin `estado`, el backend excluye los cancelados. */
export interface FiltrosAgenda {
  profesionalId?: number
  sillonId?: number
  estado?: EstadoTurno
}

/** Ruta con la consulta: el rango siempre; los filtros, solo si tienen valor. */
export function construirConsultaAgenda(rango: RangoAgenda, filtros: FiltrosAgenda): string {
  const params = new URLSearchParams({ desde: rango.desde, hasta: rango.hasta })
  agregarFiltros(params, filtros)
  return `${RUTA_AGENDA}?${params.toString()}`
}

/** Escribe en `params` solo los filtros que tienen valor (lo comparten la consulta y la URL de la pantalla). */
export function agregarFiltros(params: URLSearchParams, filtros: FiltrosAgenda): void {
  if (filtros.profesionalId !== undefined) params.set('profesionalId', String(filtros.profesionalId))
  if (filtros.sillonId !== undefined) params.set('sillonId', String(filtros.sillonId))
  if (filtros.estado !== undefined) params.set('estado', filtros.estado)
}

export function obtenerTurnosDeAgenda(rango: RangoAgenda, filtros: FiltrosAgenda): Promise<TurnoDeAgenda[]> {
  return apiClient.get<TurnoDeAgenda[]>(construirConsultaAgenda(rango, filtros))
}

/** Turnos de un día o de una semana. Mientras llega el rango nuevo se sigue mostrando el anterior. */
export function useTurnosDeAgenda(rango: RangoAgenda, filtros: FiltrosAgenda) {
  return useQuery({
    queryKey: ['agenda', rango, filtros],
    queryFn: () => obtenerTurnosDeAgenda(rango, filtros),
    placeholderData: keepPreviousData,
  })
}

// --- Asistencia (C-13, US-024)

/** Estados que se registran como asistencia, en el orden en que se ofrecen. */
export const ESTADOS_DE_ASISTENCIA = ['asistio', 'ausente', 'ausente_con_aviso'] as const

export type EstadoDeAsistencia = (typeof ESTADOS_DE_ASISTENCIA)[number]

export interface AsistenciaRegistrada {
  id: number
  estado: EstadoTurno
}

/** Registra la asistencia de un turno y, si sale bien, refresca todas las vistas de la agenda. */
export function useRegistrarAsistencia() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ turnoId, estado }: { turnoId: number; estado: EstadoDeAsistencia }) =>
      apiClient.post<AsistenciaRegistrada>(`/api/turnos/${turnoId}/asistencia`, { estado }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['agenda'] })
    },
  })
}

// --- Carga manual de turnos y sobreturnos (C-12, US-021, US-022)

/** Datos de contacto del paciente tal como se envían y se reciben. */
export interface DatosDePaciente {
  dni: string
  nombre: string
  apellido: string
  telefono: string
  email: string
}

/** Un paciente encontrado por DNI (`POST /api/pacientes/busqueda`); el DNI llega normalizado. */
export interface PacienteEncontrado extends DatosDePaciente {
  id: number
}

/** Cuerpo de `POST /api/turnos` y `POST /api/turnos/sobreturnos`; `inicio` lleva el desplazamiento `-03:00`. */
export interface PedidoDeCarga {
  paciente: DatosDePaciente
  prestacionId: number
  profesionalId: number
  sillonId: number
  inicio: string
}

/** Turno creado: sin token ni datos de contacto del paciente. */
export interface TurnoCargado {
  id: number
  inicio: string
  fin: string
  estado: EstadoTurno
  esSobreturno: boolean
  pacienteId: number
  prestacionId: number
  profesionalId: number
  sillonId: number
}

/** Horario sugerido por el backend en un 409 de la carga común (hasta 3, del mismo profesional). */
export interface Alternativa {
  inicio: string
  fin: string
  profesionalId: number
  sillonId: number
}

/** Busca un paciente por DNI. El DNI viaja en el cuerpo y no en la URL (Ley 25.326); un 404 significa "paciente nuevo". */
export function useBuscarPacientePorDni() {
  return useMutation({
    mutationFn: (dni: string) => apiClient.post<PacienteEncontrado>('/api/pacientes/busqueda', { dni }),
  })
}

function useCargaDeTurno(ruta: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (pedido: PedidoDeCarga) => apiClient.post<TurnoCargado>(ruta, pedido),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['agenda'] })
    },
  })
}

/** Carga un turno común: ante un conflicto el backend responde 409 con alternativas. */
export function useCargarTurno() {
  return useCargaDeTurno('/api/turnos')
}

/** Carga un sobreturno (acción separada y explícita): puede superponerse, pero respeta horario y bloqueos. */
export function useCargarSobreturno() {
  return useCargaDeTurno('/api/turnos/sobreturnos')
}

/** Alternativas de un error 409; lista vacía si el error no las trae. */
export function alternativasDeError(error: unknown): Alternativa[] {
  if (error instanceof ErrorApi && typeof error.cuerpo === 'object' && error.cuerpo !== null) {
    const { alternativas } = error.cuerpo as { alternativas?: unknown }
    return Array.isArray(alternativas) ? (alternativas as Alternativa[]) : []
  }
  return []
}
