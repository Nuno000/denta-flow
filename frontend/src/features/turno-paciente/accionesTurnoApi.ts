import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ErrorApi, apiClient } from '../../lib/apiClient'
import { leerErrorDeApi } from '../../lib/erroresApi'
import type { HuecoPublico } from '../reserva/reservaApi'

/**
 * Cliente de la gestión del turno por el paciente con el token del email (C-17 design D2, D4). Sin sesión: el
 * token no adivinable es la única credencial, y nunca se manda `Authorization`.
 */

export type SituacionDelTurno = 'VIGENTE' | 'CANCELADO' | 'VENCIDO'

/** Resumen público del turno: sin datos personales ni sillón (RN-PR-03). Instantes ISO con offset `-03:00`. */
export interface TurnoPublico {
  situacion: SituacionDelTurno
  inicio: string
  fin: string
  prestacion: string
  profesional: string
  estado: string
  puedeModificar: boolean
}

const SIN_TOKEN = { autenticar: false } as const

export const CLAVES_TURNO = {
  turno: (token: string) => ['publico', 'turno', token] as const,
  huecos: (token: string) => ['publico', 'turno', token, 'huecos'] as const,
  huecosDelDia: (token: string, fecha: string | null) => ['publico', 'turno', token, 'huecos', fecha] as const,
}

function rutaDelTurno(token: string): string {
  return `/api/publico/turnos/${encodeURIComponent(token)}`
}

export function useTurnoPublico(token: string) {
  return useQuery({
    queryKey: CLAVES_TURNO.turno(token),
    queryFn: () => apiClient.get<TurnoPublico>(rutaDelTurno(token), SIN_TOKEN),
    staleTime: 0,
  })
}

/** Confirmación de un toque (idempotente en el backend). Deja el turno actualizado en caché. */
export function useConfirmarTurno(token: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => apiClient.post<TurnoPublico>(`${rutaDelTurno(token)}/confirmar`, undefined, SIN_TOKEN),
    onSuccess: (turno) => queryClient.setQueryData(CLAVES_TURNO.turno(token), turno),
  })
}

export function useCancelarTurno(token: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => apiClient.post<TurnoPublico>(`${rutaDelTurno(token)}/cancelar`, undefined, SIN_TOKEN),
    onSuccess: (turno) => queryClient.setQueryData(CLAVES_TURNO.turno(token), turno),
  })
}

/** Huecos de un día (`fecha` = AAAA-MM-DD) para reprogramar; el motor excluye el propio turno. Siempre frescos. */
export function useHuecosReprogramacion(token: string, fecha: string | null) {
  return useQuery({
    queryKey: CLAVES_TURNO.huecosDelDia(token, fecha),
    queryFn: () => {
      const parametros = new URLSearchParams({ desde: String(fecha), hasta: String(fecha) })
      return apiClient.get<HuecoPublico[]>(`${rutaDelTurno(token)}/huecos?${parametros.toString()}`, SIN_TOKEN)
    },
    enabled: fecha !== null,
    staleTime: 0,
  })
}

/** Reprograma al nuevo inicio. Ante un 409 (horario tomado o fuera de término) vuelve a pedir los huecos. */
export function useReprogramarTurno(token: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (inicio: string) => apiClient.post<TurnoPublico>(`${rutaDelTurno(token)}/reprogramar`, { inicio }, SIN_TOKEN),
    onSuccess: (turno) => queryClient.setQueryData(CLAVES_TURNO.turno(token), turno),
    onError: (error) => {
      if (error instanceof ErrorApi && error.status === 409) {
        void queryClient.invalidateQueries({ queryKey: CLAVES_TURNO.huecos(token) })
      }
    },
  })
}

export type MotivoDelError = 'no-encontrado' | 'conflicto' | 'demasiados-pedidos' | 'otro'

/** Traduce un error de la API a lo que la pantalla tiene que hacer y el texto (en español) a mostrar. */
export function motivoDelError(error: unknown): { motivo: MotivoDelError; mensaje: string } {
  const { general } = leerErrorDeApi(error)
  if (!(error instanceof ErrorApi)) {
    return { motivo: 'otro', mensaje: general }
  }
  const motivo: MotivoDelError =
    error.status === 404
      ? 'no-encontrado'
      : error.status === 409
        ? 'conflicto'
        : error.status === 429
          ? 'demasiados-pedidos'
          : 'otro'
  return { motivo, mensaje: general }
}
