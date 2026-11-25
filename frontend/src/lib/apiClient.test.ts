import { afterEach, describe, expect, it, vi } from 'vitest'
import { ErrorApi, alNoAutorizado, apiClient, configurarToken } from './apiClient'

interface Saludo {
  mensaje: string
}

function respuestaJson(cuerpo: unknown, status = 200): Response {
  return new Response(JSON.stringify(cuerpo), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('apiClient', () => {
  afterEach(() => {
    vi.unstubAllEnvs()
    vi.unstubAllGlobals()
  })

  it('arma la URL sobre VITE_API_BASE_URL', async () => {
    vi.stubEnv('VITE_API_BASE_URL', 'https://api.ejemplo')
    const fetchSimulado = vi.fn().mockResolvedValue(respuestaJson({ mensaje: 'hola' }))
    vi.stubGlobal('fetch', fetchSimulado)

    await apiClient.get<Saludo>('/api/saludo')

    expect(fetchSimulado).toHaveBeenCalledWith('https://api.ejemplo/api/saludo', expect.any(Object))
  })

  it('no duplica la barra entre la base y la ruta', async () => {
    vi.stubEnv('VITE_API_BASE_URL', 'https://api.ejemplo/')
    const fetchSimulado = vi.fn().mockResolvedValue(respuestaJson({ mensaje: 'hola' }))
    vi.stubGlobal('fetch', fetchSimulado)

    await apiClient.get<Saludo>('/api/saludo')

    expect(fetchSimulado).toHaveBeenCalledWith('https://api.ejemplo/api/saludo', expect.any(Object))
  })

  it('devuelve el JSON tipado', async () => {
    vi.stubEnv('VITE_API_BASE_URL', 'https://api.ejemplo')
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(respuestaJson({ mensaje: 'hola' })))

    const saludo = await apiClient.get<Saludo>('/api/saludo')

    expect(saludo.mensaje).toBe('hola')
  })

  it('envía el cuerpo como JSON en un POST', async () => {
    vi.stubEnv('VITE_API_BASE_URL', 'https://api.ejemplo')
    const fetchSimulado = vi.fn().mockResolvedValue(respuestaJson({ mensaje: 'creado' }, 201))
    vi.stubGlobal('fetch', fetchSimulado)

    await apiClient.post<Saludo>('/api/saludo', { nombre: 'Ana' })

    const [, opciones] = fetchSimulado.mock.calls[0] as [string, RequestInit]
    expect(opciones.method).toBe('POST')
    expect(opciones.body).toBe(JSON.stringify({ nombre: 'Ana' }))
    expect(new Headers(opciones.headers).get('Content-Type')).toBe('application/json')
  })

  describe('autenticación (C-04)', () => {
    afterEach(() => {
      configurarToken(() => null)
    })

    it('con un token agrega Authorization: Bearer', async () => {
      vi.stubEnv('VITE_API_BASE_URL', 'https://api.ejemplo')
      const fetchSimulado = vi.fn().mockResolvedValue(respuestaJson({ mensaje: 'hola' }))
      vi.stubGlobal('fetch', fetchSimulado)
      configurarToken(() => 'token-firmado')

      await apiClient.get<Saludo>('/api/auth/yo')

      const [, opciones] = fetchSimulado.mock.calls[0] as [string, RequestInit]
      expect(new Headers(opciones.headers).get('Authorization')).toBe('Bearer token-firmado')
    })

    it('sin token no agrega Authorization', async () => {
      vi.stubEnv('VITE_API_BASE_URL', 'https://api.ejemplo')
      const fetchSimulado = vi.fn().mockResolvedValue(respuestaJson({ mensaje: 'hola' }))
      vi.stubGlobal('fetch', fetchSimulado)

      await apiClient.get<Saludo>('/api/publico/algo')

      const [, opciones] = fetchSimulado.mock.calls[0] as [string, RequestInit]
      expect(new Headers(opciones.headers).has('Authorization')).toBe(false)
    })

    it('un pedido marcado como no autenticado no manda el token', async () => {
      vi.stubEnv('VITE_API_BASE_URL', 'https://api.ejemplo')
      const fetchSimulado = vi.fn().mockResolvedValue(respuestaJson({ mensaje: 'hola' }))
      vi.stubGlobal('fetch', fetchSimulado)
      configurarToken(() => 'token-viejo')

      await apiClient.post<Saludo>('/api/auth/login', { email: 'a@example.com' }, { autenticar: false })

      const [, opciones] = fetchSimulado.mock.calls[0] as [string, RequestInit]
      expect(new Headers(opciones.headers).has('Authorization')).toBe(false)
    })

    it('ante un 401 de un pedido autenticado avisa a los suscriptores', async () => {
      vi.stubEnv('VITE_API_BASE_URL', 'https://api.ejemplo')
      vi.stubGlobal('fetch', vi.fn().mockResolvedValue(respuestaJson({ detail: 'Ingresá' }, 401)))
      configurarToken(() => 'token-vencido')
      const suscriptor = vi.fn()
      const desuscribir = alNoAutorizado(suscriptor)

      const error = await apiClient.get<Saludo>('/api/auth/yo').catch((e: unknown) => e)

      expect((error as ErrorApi).status).toBe(401)
      expect(suscriptor).toHaveBeenCalledTimes(1)
      desuscribir()
    })

    it('un 401 del login (sin token) no avisa a los suscriptores', async () => {
      vi.stubEnv('VITE_API_BASE_URL', 'https://api.ejemplo')
      vi.stubGlobal('fetch', vi.fn().mockResolvedValue(respuestaJson({ detail: 'Email o contraseña incorrectos' }, 401)))
      const suscriptor = vi.fn()
      const desuscribir = alNoAutorizado(suscriptor)

      await apiClient.post<Saludo>('/api/auth/login', {}, { autenticar: false }).catch(() => undefined)

      expect(suscriptor).not.toHaveBeenCalled()
      desuscribir()
    })

    it('una respuesta 204 sin cuerpo se resuelve sin error', async () => {
      vi.stubEnv('VITE_API_BASE_URL', 'https://api.ejemplo')
      vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, { status: 204 })))

      await expect(apiClient.put<void>('/api/usuarios/2/contrasena', { contrasena: 'x' })).resolves.toBeUndefined()
    })
  })

  it('lanza un ErrorApi tipado ante una respuesta no 2xx', async () => {
    vi.stubEnv('VITE_API_BASE_URL', 'https://api.ejemplo')
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(respuestaJson({ mensaje: 'Turno no disponible' }, 409)))

    const error = await apiClient.get<Saludo>('/api/turnos/1').catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ErrorApi)
    expect((error as ErrorApi).status).toBe(409)
    expect((error as ErrorApi).cuerpo).toEqual({ mensaje: 'Turno no disponible' })
  })
})
