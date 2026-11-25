import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ErrorApi, apiClient } from '../../lib/apiClient'
import type { Rol, UsuarioSesion } from '../auth/sesion'

/** Usuario tal como lo devuelve la gestión de usuarios (mismos datos que la sesión). */
export type UsuarioGestionado = UsuarioSesion

export interface RolDisponible {
  codigo: Rol
  nombre: string
}

export interface ProfesionalDisponible {
  id: number
  nombre: string
  apellido: string
}

export interface DatosAlta {
  email: string
  contrasena: string
  rol: Rol
  profesionalId: number | null
}

export interface DatosCambioRol {
  rol: Rol
  profesionalId: number | null
}

const CLAVES = {
  usuarios: ['usuarios'] as const,
  roles: ['usuarios', 'roles'] as const,
  profesionalesDisponibles: ['usuarios', 'profesionales-disponibles'] as const,
}

export function useUsuarios() {
  return useQuery({ queryKey: CLAVES.usuarios, queryFn: () => apiClient.get<UsuarioGestionado[]>('/api/usuarios') })
}

export function useRoles() {
  return useQuery({
    queryKey: CLAVES.roles,
    queryFn: () => apiClient.get<RolDisponible[]>('/api/roles'),
    staleTime: Infinity,
  })
}

export function useProfesionalesDisponibles() {
  return useQuery({
    queryKey: CLAVES.profesionalesDisponibles,
    queryFn: () => apiClient.get<ProfesionalDisponible[]>('/api/usuarios/profesionales-disponibles'),
  })
}

/** Toda mutación invalida el listado y los profesionales disponibles (design D13). */
function useMutacionDeUsuarios<T>(accion: (datos: T) => Promise<unknown>) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: accion,
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: CLAVES.usuarios, exact: true }),
        queryClient.invalidateQueries({ queryKey: CLAVES.profesionalesDisponibles }),
      ])
    },
  })
}

export function useCrearUsuario() {
  return useMutacionDeUsuarios((datos: DatosAlta) => apiClient.post<UsuarioGestionado>('/api/usuarios', datos))
}

export function useCambiarRol() {
  return useMutacionDeUsuarios(({ id, ...datos }: DatosCambioRol & { id: number }) =>
    apiClient.put<UsuarioGestionado>(`/api/usuarios/${id}/rol`, datos),
  )
}

export function useDesactivar() {
  return useMutacionDeUsuarios((id: number) => apiClient.post<UsuarioGestionado>(`/api/usuarios/${id}/desactivar`, {}))
}

export function useActivar() {
  return useMutacionDeUsuarios((id: number) => apiClient.post<UsuarioGestionado>(`/api/usuarios/${id}/activar`, {}))
}

export function useBlanquearContrasena() {
  return useMutacionDeUsuarios(({ id, contrasena }: { id: number; contrasena: string }) =>
    apiClient.put<void>(`/api/usuarios/${id}/contrasena`, { contrasena }),
  )
}

interface CuerpoProblema {
  detail?: string
  errores?: Record<string, string>
}

/** Traduce un error de la API a mensajes por campo y uno general, para mostrarlos en el formulario. */
export function leerErrorDeApi(error: unknown): { campos: Record<string, string>; general: string } {
  if (error instanceof ErrorApi && typeof error.cuerpo === 'object' && error.cuerpo !== null) {
    const cuerpo = error.cuerpo as CuerpoProblema
    return {
      campos: cuerpo.errores ?? {},
      general: cuerpo.detail ?? 'No se pudo completar la operación',
    }
  }
  return { campos: {}, general: 'No pudimos conectar con el servidor. Probá de nuevo.' }
}
