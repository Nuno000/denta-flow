/**
 * Único cliente HTTP de la SPA (regla dura: nada de fetch suelto en componentes).
 * Toma la URL base de VITE_API_BASE_URL y se usa desde los hooks de TanStack Query.
 */

/** Error de una respuesta HTTP no 2xx, con el cuerpo que devolvió la API (si lo hubo). */
export class ErrorApi extends Error {
  readonly status: number
  readonly cuerpo: unknown

  constructor(status: number, cuerpo: unknown) {
    super(`La API respondió ${status}`)
    this.name = 'ErrorApi'
    this.status = status
    this.cuerpo = cuerpo
  }
}

function urlBase(): string {
  return (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/+$/, '')
}

async function leerCuerpo(respuesta: Response): Promise<unknown> {
  const texto = await respuesta.text()
  if (texto === '') {
    return undefined
  }
  const tipo = respuesta.headers.get('Content-Type') ?? ''
  return tipo.includes('json') ? (JSON.parse(texto) as unknown) : texto
}

async function solicitar<T>(ruta: string, opciones: RequestInit = {}): Promise<T> {
  const cabeceras = new Headers(opciones.headers)
  cabeceras.set('Accept', 'application/json')
  if (opciones.body !== undefined) {
    cabeceras.set('Content-Type', 'application/json')
  }

  const respuesta = await fetch(`${urlBase()}${ruta}`, { ...opciones, headers: cabeceras })
  const cuerpo = await leerCuerpo(respuesta)

  if (!respuesta.ok) {
    throw new ErrorApi(respuesta.status, cuerpo)
  }
  return cuerpo as T
}

export const apiClient = {
  get: <T>(ruta: string, opciones?: RequestInit) => solicitar<T>(ruta, { ...opciones, method: 'GET' }),
  post: <T>(ruta: string, datos: unknown, opciones?: RequestInit) =>
    solicitar<T>(ruta, { ...opciones, method: 'POST', body: JSON.stringify(datos) }),
  put: <T>(ruta: string, datos: unknown, opciones?: RequestInit) =>
    solicitar<T>(ruta, { ...opciones, method: 'PUT', body: JSON.stringify(datos) }),
  patch: <T>(ruta: string, datos: unknown, opciones?: RequestInit) =>
    solicitar<T>(ruta, { ...opciones, method: 'PATCH', body: JSON.stringify(datos) }),
  delete: <T>(ruta: string, opciones?: RequestInit) => solicitar<T>(ruta, { ...opciones, method: 'DELETE' }),
}
