/**
 * Único cliente HTTP de la SPA (regla dura: nada de fetch suelto en componentes).
 * Toma la URL base de VITE_API_BASE_URL y se usa desde los hooks de TanStack Query.
 * Desde C-04 envía el token del personal en `Authorization: Bearer` y avisa cuando la API responde 401
 * a un pedido autenticado (sesión vencida o usuario desactivado).
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

/** Opciones de un pedido: las de fetch más `autenticar` (por defecto, manda el token si hay sesión). */
export interface OpcionesApi extends RequestInit {
  autenticar?: boolean
}

/**
 * Espera máxima de un pedido. La API del plan gratuito de Render se suspende sin tráfico y la primera
 * respuesta puede tardar cerca de un minuto (C-19, design D5): pasado este tiempo el pedido se corta.
 */
export const TIEMPO_MAXIMO_PEDIDO_MS = 70_000

let obtenerToken: () => string | null = () => null
const suscriptoresNoAutorizado = new Set<() => void>()

/** Define de dónde sale el token de la sesión (lo configura el proveedor de sesión). */
export function configurarToken(proveedor: () => string | null): void {
  obtenerToken = proveedor
}

/** Se suscribe a los 401 de pedidos autenticados; devuelve la función para desuscribirse. */
export function alNoAutorizado(suscriptor: () => void): () => void {
  suscriptoresNoAutorizado.add(suscriptor)
  return () => {
    suscriptoresNoAutorizado.delete(suscriptor)
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

async function solicitar<T>(ruta: string, opciones: OpcionesApi = {}): Promise<T> {
  const { autenticar = true, ...init } = opciones
  const cabeceras = new Headers(init.headers)
  cabeceras.set('Accept', 'application/json')
  if (init.body !== undefined) {
    cabeceras.set('Content-Type', 'application/json')
  }
  const token = autenticar ? obtenerToken() : null
  if (token !== null) {
    cabeceras.set('Authorization', `Bearer ${token}`)
  }

  const respuesta = await fetch(`${urlBase()}${ruta}`, {
    ...init,
    headers: cabeceras,
    signal: init.signal ?? AbortSignal.timeout(TIEMPO_MAXIMO_PEDIDO_MS),
  })
  const cuerpo = await leerCuerpo(respuesta)

  if (!respuesta.ok) {
    if (respuesta.status === 401 && token !== null) {
      suscriptoresNoAutorizado.forEach((suscriptor) => suscriptor())
    }
    throw new ErrorApi(respuesta.status, cuerpo)
  }
  return cuerpo as T
}

export const apiClient = {
  get: <T>(ruta: string, opciones?: OpcionesApi) => solicitar<T>(ruta, { ...opciones, method: 'GET' }),
  post: <T>(ruta: string, datos: unknown, opciones?: OpcionesApi) =>
    solicitar<T>(ruta, { ...opciones, method: 'POST', body: JSON.stringify(datos) }),
  put: <T>(ruta: string, datos: unknown, opciones?: OpcionesApi) =>
    solicitar<T>(ruta, { ...opciones, method: 'PUT', body: JSON.stringify(datos) }),
  patch: <T>(ruta: string, datos: unknown, opciones?: OpcionesApi) =>
    solicitar<T>(ruta, { ...opciones, method: 'PATCH', body: JSON.stringify(datos) }),
  delete: <T>(ruta: string, opciones?: OpcionesApi) => solicitar<T>(ruta, { ...opciones, method: 'DELETE' }),
}
