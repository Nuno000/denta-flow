import type { UsuarioSesion } from '../features/auth/sesion'

/** Datos ficticios para los tests del ABM de usuarios (Ley 25.326: nada real). */
export const ADMIN: UsuarioSesion = {
  id: 1,
  email: 'administrador@example.com',
  rol: 'administrador',
  profesionalId: null,
  profesionalNombre: null,
  activo: true,
}

export const RECEPCION: UsuarioSesion = {
  id: 2,
  email: 'recepcion@example.com',
  rol: 'recepcion',
  profesionalId: null,
  profesionalNombre: null,
  activo: true,
}

export const LAURA: UsuarioSesion = {
  id: 3,
  email: 'laura@example.com',
  rol: 'profesional',
  profesionalId: 10,
  profesionalNombre: 'Laura Ejemplo',
  activo: false,
}

export const ROLES = [
  { codigo: 'administrador', nombre: 'Administrador' },
  { codigo: 'recepcion', nombre: 'Recepción' },
  { codigo: 'profesional', nombre: 'Profesional' },
]

export const DISPONIBLES = [{ id: 11, nombre: 'Martín', apellido: 'Prueba' }]

/** Rutas base de la pantalla de usuarios con el administrador ingresado. */
export function apiDeUsuarios(usuarios: UsuarioSesion[] = [ADMIN, RECEPCION, LAURA]) {
  return {
    'GET /api/auth/yo': { status: 200, cuerpo: ADMIN },
    'GET /api/usuarios': { status: 200, cuerpo: usuarios },
    'GET /api/roles': { status: 200, cuerpo: ROLES },
    'GET /api/usuarios/profesionales-disponibles': { status: 200, cuerpo: DISPONIBLES },
  }
}
