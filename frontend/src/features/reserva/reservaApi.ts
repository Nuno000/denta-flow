import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ErrorApi, apiClient } from '../../lib/apiClient'
import { leerErrorDeApi } from '../../lib/erroresApi'

/**
 * Cliente de la reserva pública (C-16 design D3; huecos de C-10 design D7). Todo sin token: la reserva no
 * requiere cuenta (RN-DI-03).
 */

export interface PrestacionPublica {
  id: number
  nombre: string
  duracionMinutos: number
}

export interface ProfesionalPublico {
  id: number
  nombre: string
  apellido: string
}

/** Hueco ofrecido por el motor: instantes ISO con el offset del consultorio (`-03:00`). */
export interface HuecoPublico {
  inicio: string
  fin: string
  profesionalId: number | null
}

export interface ContactoConsultorio {
  contacto: string
}

export interface SolicitudReserva {
  prestacionId: number
  /** `null` = cualquier profesional (PA-05). */
  profesionalId: number | null
  inicio: string
  paciente: { dni: string; nombre: string; apellido: string; telefono: string; email: string }
  aceptaAvisoPrivacidad: boolean
}

export interface ReservaConfirmada {
  inicio: string
  fin: string
  prestacion: string
  profesional: string
  estado: string
}

const SIN_TOKEN = { autenticar: false } as const

export const CLAVES_RESERVA = {
  todo: ['publico'] as const,
  prestaciones: ['publico', 'prestaciones'] as const,
  profesionales: (prestacionId: number | null) => ['publico', 'profesionales', prestacionId] as const,
  huecos: ['publico', 'huecos'] as const,
  huecosDelDia: (prestacionId: number | null, profesionalId: number | null, fecha: string | null) =>
    ['publico', 'huecos', prestacionId, profesionalId, fecha] as const,
  contacto: ['publico', 'contacto'] as const,
}

export function usePrestacionesPublicas() {
  return useQuery({
    queryKey: CLAVES_RESERVA.prestaciones,
    queryFn: () => apiClient.get<PrestacionPublica[]>('/api/publico/prestaciones', SIN_TOKEN),
  })
}

export function useProfesionalesDe(prestacionId: number | null) {
  return useQuery({
    queryKey: CLAVES_RESERVA.profesionales(prestacionId),
    queryFn: () =>
      apiClient.get<ProfesionalPublico[]>(`/api/publico/profesionales?prestacionId=${prestacionId}`, SIN_TOKEN),
    enabled: prestacionId !== null,
  })
}

/** Huecos de un solo día del consultorio (`fecha` = AAAA-MM-DD). Siempre frescos: la agenda cambia. */
export function useHuecosDelDia(prestacionId: number | null, profesionalId: number | null, fecha: string | null) {
  return useQuery({
    queryKey: CLAVES_RESERVA.huecosDelDia(prestacionId, profesionalId, fecha),
    queryFn: () => {
      const parametros = new URLSearchParams({ prestacionId: String(prestacionId), desde: String(fecha), hasta: String(fecha) })
      if (profesionalId !== null) {
        parametros.set('profesionalId', String(profesionalId))
      }
      return apiClient.get<HuecoPublico[]>(`/api/publico/huecos?${parametros.toString()}`, SIN_TOKEN)
    },
    enabled: prestacionId !== null && fecha !== null,
    staleTime: 0,
  })
}

export function useContacto(habilitado = true) {
  return useQuery({
    queryKey: CLAVES_RESERVA.contacto,
    queryFn: () => apiClient.get<ContactoConsultorio>('/api/publico/contacto', SIN_TOKEN),
    enabled: habilitado,
  })
}

/** Crea la reserva. Ante un 409 (horario tomado) vuelve a pedir los huecos (spec "Horario tomado..."). */
export function useReservar() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (solicitud: SolicitudReserva) =>
      apiClient.post<ReservaConfirmada>('/api/publico/reservas', solicitud, SIN_TOKEN),
    onError: (error) => {
      if (error instanceof ErrorApi && error.status === 409) {
        void queryClient.invalidateQueries({ queryKey: CLAVES_RESERVA.huecos })
      }
    },
  })
}

export type MotivoDelRechazo = 'horario-tomado' | 'demasiados-pedidos' | 'datos' | 'otro'

/** Traduce el error de una reserva a lo que la pantalla tiene que hacer y el texto a mostrar. */
export function motivoDelRechazo(error: unknown): { motivo: MotivoDelRechazo; mensaje: string; campos: Record<string, string> } {
  const { campos, general } = leerErrorDeApi(error)
  if (!(error instanceof ErrorApi)) {
    return { motivo: 'otro', mensaje: general, campos }
  }
  const motivo: MotivoDelRechazo =
    error.status === 409 ? 'horario-tomado' : error.status === 429 ? 'demasiados-pedidos' : error.status === 400 ? 'datos' : 'otro'
  return { motivo, mensaje: general, campos }
}
