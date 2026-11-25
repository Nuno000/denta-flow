import { afterEach, describe, expect, it } from 'vitest'
import { borrarSesion, guardarSesion, leerSesion, type Sesion } from './sesion'

const sesion: Sesion = {
  token: 'token-firmado',
  venceEn: '2026-11-25T21:00:00Z',
  usuario: {
    id: 1,
    email: 'admin@example.com',
    rol: 'administrador',
    profesionalId: null,
    profesionalNombre: null,
    activo: true,
  },
}

describe('sesion', () => {
  afterEach(() => {
    sessionStorage.clear()
  })

  it('guarda y lee la sesión en sessionStorage', () => {
    guardarSesion(sesion)

    expect(leerSesion()).toEqual(sesion)
    expect(localStorage.length).toBe(0)
  })

  it('sin sesión guardada devuelve null', () => {
    expect(leerSesion()).toBeNull()
  })

  it('borrar la sesión la descarta', () => {
    guardarSesion(sesion)
    borrarSesion()

    expect(leerSesion()).toBeNull()
  })

  it('un valor corrupto se descarta en lugar de romper la app', () => {
    sessionStorage.setItem('dentaflow.sesion', '{no es json')

    expect(leerSesion()).toBeNull()
    expect(sessionStorage.getItem('dentaflow.sesion')).toBeNull()
  })
})
