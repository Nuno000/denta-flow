/**
 * Único punto de contacto de la agenda con la API (C-11, design D1 y D3). Los componentes usan solo
 * `useTurnosDeAgenda`; si el backend real cambiara un nombre de campo, el ajuste queda en este archivo.
 */
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '../../lib/apiClient'
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

// --- Registro de cambios por turno (C-13, US-025, solo administrador)

export type TipoActorHistorial = 'usuario' | 'paciente' | 'sistema'

export type AccionHistorial = 'creacion' | 'confirmacion' | 'cancelacion' | 'reprogramacion' | 'cambio_estado'

export interface CambioDeTurno {
  id: number
  /** Instante en UTC (ISO-8601). */
  ocurridoEn: string
  actorTipo: TipoActorHistorial
  actorEmail: string | null
  accion: AccionHistorial
  valorAnterior: Record<string, string>
  valorNuevo: Record<string, string>
}

/** Consulta el historial de cambios de un turno (solo administrador, solo lectura). */
export function useHistorialDeTurno(turnoId: number | null) {
  return useQuery({
    queryKey: ['turno-historial', turnoId],
    queryFn: () => apiClient.get<CambioDeTurno[]>(`/api/turnos/${turnoId}/historial`),
    enabled: turnoId !== null,
  })
}
