import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiClient, ErrorApi } from '../../lib/apiClient'

export type EstadoListaEspera = 'activa' | 'ofrecida' | 'atendida' | 'vencida'

export interface EntradaListaEspera {
  id: number
  pacienteId: number
  pacienteNombre: string
  pacienteDni: string | null
  prestacionId: number
  prestacionNombre: string
  profesionalId: number | null
  profesionalNombre: string | null
  fechaDesde: string
  fechaHasta: string
  horaDesde: string | null
  horaHasta: string | null
  estado: EstadoListaEspera
  inicioOfrecido: string | null
  ofrecidaHasta: string | null
  creadoEn: string
}

export interface CrearListaEsperaEntrada {
  pacienteId: number
  prestacionId: number
  profesionalId: number | null
  fechaDesde: string
  fechaHasta: string
  horaDesde: string | null
  horaHasta: string | null
}

export interface OfertaListaEspera {
  estado: EstadoListaEspera
  prestacionNombre: string
  profesionalNombre: string
  inicioOfrecido: string | null
  ofrecidaHasta: string | null
  turnoCreadoId: number | null
}

export interface PacienteEncontrado {
  id: number
  dni: string
  nombre: string
  apellido: string
}

export const CLAVE_LISTA_ESPERA = ['lista-espera'] as const

export function useListaEspera(estado?: EstadoListaEspera | '') {
  const qs = estado ? `?estado=${encodeURIComponent(estado)}` : ''
  return useQuery({
    queryKey: [...CLAVE_LISTA_ESPERA, estado ?? ''],
    queryFn: () => apiClient.get<EntradaListaEspera[]>(`/api/lista-espera${qs}`),
  })
}

export function useCrearListaEspera() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (datos: CrearListaEsperaEntrada) =>
      apiClient.post<EntradaListaEspera>('/api/lista-espera', datos),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: CLAVE_LISTA_ESPERA })
    },
  })
}

export function useDarDeBajaListaEspera() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => apiClient.delete(`/api/lista-espera/${id}`),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: CLAVE_LISTA_ESPERA })
    },
  })
}

export async function buscarPacientePorDni(dni: string): Promise<PacienteEncontrado> {
  return apiClient.post<PacienteEncontrado>('/api/pacientes/busqueda', { dni })
}

export function useOfertaListaEspera(token: string) {
  return useQuery({
    queryKey: ['oferta-lista-espera', token],
    queryFn: () => apiClient.get<OfertaListaEspera>(`/api/publico/lista-espera/${encodeURIComponent(token)}`),
    enabled: token.length > 0,
    retry: false,
  })
}

export function useAceptarOfertaListaEspera(token: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: () =>
      apiClient.post<OfertaListaEspera>(`/api/publico/lista-espera/${encodeURIComponent(token)}/aceptar`, {}),
    onSuccess: (data) => {
      qc.setQueryData(['oferta-lista-espera', token], data)
    },
  })
}

export function mensajeErrorListaEspera(error: unknown): string {
  if (error instanceof ErrorApi) {
    if (typeof error.cuerpo === 'string' && error.cuerpo.trim().length > 0) {
      return error.cuerpo
    }
    if (error.cuerpo && typeof error.cuerpo === 'object') {
      const c = error.cuerpo as { detail?: unknown; message?: unknown; title?: unknown }
      if (typeof c.detail === 'string' && c.detail.trim().length > 0) return c.detail
      if (typeof c.message === 'string' && c.message.trim().length > 0) return c.message
      if (typeof c.title === 'string' && c.title.trim().length > 0) return c.title
    }
    return error.message
  }
  return 'Ocurrió un error inesperado. Intentá de nuevo.'
}
