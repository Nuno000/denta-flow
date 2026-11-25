import { afterEach, describe, expect, it, vi } from 'vitest'
import { ErrorApi, apiClient } from './apiClient'

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

  it('lanza un ErrorApi tipado ante una respuesta no 2xx', async () => {
    vi.stubEnv('VITE_API_BASE_URL', 'https://api.ejemplo')
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(respuestaJson({ mensaje: 'Turno no disponible' }, 409)))

    const error = await apiClient.get<Saludo>('/api/turnos/1').catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ErrorApi)
    expect((error as ErrorApi).status).toBe(409)
    expect((error as ErrorApi).cuerpo).toEqual({ mensaje: 'Turno no disponible' })
  })
})
