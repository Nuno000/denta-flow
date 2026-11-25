/**
 * Sesión del personal en el navegador (C-04, design D12). Se guarda en sessionStorage: sobrevive a
 * recargar la página y se borra al cerrar la pestaña. El token vence a las 8 horas del lado del backend.
 */

export type Rol = 'administrador' | 'recepcion' | 'profesional'

export const NOMBRES_DE_ROL: Record<Rol, string> = {
  administrador: 'Administrador',
  recepcion: 'Recepción',
  profesional: 'Profesional',
}

export interface UsuarioSesion {
  id: number
  email: string
  rol: Rol
  profesionalId: number | null
  profesionalNombre: string | null
  activo: boolean
}

export interface Sesion {
  token: string
  /** Vencimiento del token en UTC (ISO 8601). */
  venceEn: string
  usuario: UsuarioSesion
}

const CLAVE = 'dentaflow.sesion'

export function leerSesion(): Sesion | null {
  const guardada = sessionStorage.getItem(CLAVE)
  if (guardada === null) {
    return null
  }
  try {
    return JSON.parse(guardada) as Sesion
  } catch {
    sessionStorage.removeItem(CLAVE)
    return null
  }
}

export function guardarSesion(sesion: Sesion): void {
  sessionStorage.setItem(CLAVE, JSON.stringify(sesion))
}

export function borrarSesion(): void {
  sessionStorage.removeItem(CLAVE)
}
