import { apiClient } from '../../lib/apiClient'
import type { Sesion, UsuarioSesion } from './sesion'

export interface DatosIngreso {
  email: string
  contrasena: string
}

/** Ingreso del personal. Va sin token: es la única forma de obtener uno. */
export function ingresar(datos: DatosIngreso): Promise<Sesion> {
  return apiClient.post<Sesion>('/api/auth/login', datos, { autenticar: false })
}

/** Usuario autenticado, con el rol y el estado vigentes. */
export function obtenerUsuarioActual(): Promise<UsuarioSesion> {
  return apiClient.get<UsuarioSesion>('/api/auth/yo')
}

export const CLAVE_USUARIO_ACTUAL = ['auth', 'yo'] as const
