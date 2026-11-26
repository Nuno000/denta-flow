import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '../../lib/apiClient'
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
