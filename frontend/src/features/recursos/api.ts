import { useMutation, useQuery, useQueryClient, type QueryKey } from '@tanstack/react-query'
import { apiClient } from '../../lib/apiClient'

/** Recursos del consultorio tal como los devuelve la API (C-05, design D3). */
export interface Sillon {
  id: number
  nombre: string
  activo: boolean
}

export interface Prestacion {
  id: number
  nombre: string
  duracionMinutos: number
  activa: boolean
}

export interface Profesional {
  id: number
  nombre: string
  apellido: string
  /** Ausente cuando el profesional no tiene matrícula. */
  matricula?: string
  email: string
  activo: boolean
  prestacionIds: number[]
}

export interface DatosSillon {
  nombre: string
}

export interface DatosPrestacion {
  nombre: string
  duracionMinutos: number
}

export interface DatosProfesional {
  nombre: string
  apellido: string
  matricula: string | null
  email: string
}

export const CLAVES_RECURSOS = {
  sillones: ['recursos', 'sillones'] as const,
  prestaciones: ['recursos', 'prestaciones'] as const,
  profesionales: ['recursos', 'profesionales'] as const,
}

export function useSillones() {
  return useQuery({ queryKey: CLAVES_RECURSOS.sillones, queryFn: () => apiClient.get<Sillon[]>('/api/sillones') })
}

export function usePrestaciones() {
  return useQuery({
    queryKey: CLAVES_RECURSOS.prestaciones,
    queryFn: () => apiClient.get<Prestacion[]>('/api/prestaciones'),
  })
}

export function useProfesionales() {
  return useQuery({
    queryKey: CLAVES_RECURSOS.profesionales,
    queryFn: () => apiClient.get<Profesional[]>('/api/profesionales'),
  })
}

/** Mutación que, al terminar bien, invalida solo los listados afectados (design D6). */
function useMutacionDeRecursos<T, R>(accion: (datos: T) => Promise<R>, afectados: readonly QueryKey[]) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: accion,
    onSuccess: async () => {
      await Promise.all(afectados.map((queryKey) => queryClient.invalidateQueries({ queryKey })))
    },
  })
}

// --- Sillones

export function useCrearSillon() {
  return useMutacionDeRecursos((datos: DatosSillon) => apiClient.post<Sillon>('/api/sillones', datos), [
    CLAVES_RECURSOS.sillones,
  ])
}

export function useEditarSillon() {
  return useMutacionDeRecursos(
    ({ id, ...datos }: DatosSillon & { id: number }) => apiClient.put<Sillon>(`/api/sillones/${id}`, datos),
    [CLAVES_RECURSOS.sillones],
  )
}

export function useDesactivarSillon() {
  return useMutacionDeRecursos((id: number) => apiClient.post<Sillon>(`/api/sillones/${id}/desactivar`, {}), [
    CLAVES_RECURSOS.sillones,
  ])
}

export function useActivarSillon() {
  return useMutacionDeRecursos((id: number) => apiClient.post<Sillon>(`/api/sillones/${id}/activar`, {}), [
    CLAVES_RECURSOS.sillones,
  ])
}

// --- Prestaciones (el estado de una prestación cambia cómo se ven los profesionales que la realizan)

export function useCrearPrestacion() {
  return useMutacionDeRecursos(
    (datos: DatosPrestacion) => apiClient.post<Prestacion>('/api/prestaciones', datos),
    [CLAVES_RECURSOS.prestaciones],
  )
}

export function useEditarPrestacion() {
  return useMutacionDeRecursos(
    ({ id, ...datos }: DatosPrestacion & { id: number }) => apiClient.put<Prestacion>(`/api/prestaciones/${id}`, datos),
    [CLAVES_RECURSOS.prestaciones, CLAVES_RECURSOS.profesionales],
  )
}

export function useDesactivarPrestacion() {
  return useMutacionDeRecursos(
    (id: number) => apiClient.post<Prestacion>(`/api/prestaciones/${id}/desactivar`, {}),
    [CLAVES_RECURSOS.prestaciones, CLAVES_RECURSOS.profesionales],
  )
}

export function useActivarPrestacion() {
  return useMutacionDeRecursos(
    (id: number) => apiClient.post<Prestacion>(`/api/prestaciones/${id}/activar`, {}),
    [CLAVES_RECURSOS.prestaciones, CLAVES_RECURSOS.profesionales],
  )
}

// --- Profesionales

export function useCrearProfesional() {
  return useMutacionDeRecursos(
    (datos: DatosProfesional) => apiClient.post<Profesional>('/api/profesionales', datos),
    [CLAVES_RECURSOS.profesionales],
  )
}

export function useEditarProfesional() {
  return useMutacionDeRecursos(
    ({ id, ...datos }: DatosProfesional & { id: number }) =>
      apiClient.put<Profesional>(`/api/profesionales/${id}`, datos),
    [CLAVES_RECURSOS.profesionales],
  )
}

export function useDesactivarProfesional() {
  return useMutacionDeRecursos(
    (id: number) => apiClient.post<Profesional>(`/api/profesionales/${id}/desactivar`, {}),
    [CLAVES_RECURSOS.profesionales],
  )
}

export function useActivarProfesional() {
  return useMutacionDeRecursos(
    (id: number) => apiClient.post<Profesional>(`/api/profesionales/${id}/activar`, {}),
    [CLAVES_RECURSOS.profesionales],
  )
}

/** Reemplaza el conjunto de prestaciones que realiza el profesional (design D5). */
export function useAsignarPrestaciones() {
  return useMutacionDeRecursos(
    ({ id, prestacionIds }: { id: number; prestacionIds: number[] }) =>
      apiClient.put<Profesional>(`/api/profesionales/${id}/prestaciones`, { prestacionIds }),
    [CLAVES_RECURSOS.profesionales],
  )
}
