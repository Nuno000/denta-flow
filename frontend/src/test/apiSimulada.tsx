import { render } from '@testing-library/react'
import { createMemoryRouter } from 'react-router-dom'
import { vi } from 'vitest'
import { guardarSesion, type Rol, type Sesion } from '../features/auth/sesion'
import { Proveedores } from '../Proveedores'
import { rutas } from '../routes/rutas'

/** Respuesta simulada de la API: estado y cuerpo JSON (opcional). */
export interface RespuestaSimulada {
  status: number
  cuerpo?: unknown
  headers?: Record<string, string>
}

type Manejador = RespuestaSimulada | ((cuerpo: unknown) => RespuestaSimulada)

/**
 * Simula la API en el borde HTTP (`fetch`), nunca los hooks. Las claves son "MÉTODO /ruta" y los
 * pedidos no previstos responden 404, así un llamado inesperado se nota en el test.
 */
export function simularApi(manejadores: Record<string, Manejador>) {
  const fetchSimulado = vi.fn(async (url: string, init?: RequestInit) => {
    const ruta = new URL(url, 'http://localhost').pathname
    const clave = `${init?.method ?? 'GET'} ${ruta}`
    const manejador = manejadores[clave]
    const cuerpoPedido: unknown = typeof init?.body === 'string' ? JSON.parse(init.body) : undefined
    const respuesta: RespuestaSimulada =
      manejador === undefined
        ? { status: 404, cuerpo: { detail: `Sin simular: ${clave}` } }
        : typeof manejador === 'function'
          ? manejador(cuerpoPedido)
          : manejador
    const contenido = respuesta.cuerpo === undefined ? null : JSON.stringify(respuesta.cuerpo)
    return new Response(contenido, {
      status: respuesta.status,
      headers: { 'Content-Type': 'application/json', ...respuesta.headers },
    })
  })
  vi.stubGlobal('fetch', fetchSimulado)
  return fetchSimulado
}

/** Pedidos que recibió la API simulada, como "MÉTODO /ruta". */
export function pedidos(fetchSimulado: ReturnType<typeof simularApi>): string[] {
  return fetchSimulado.mock.calls.map(([url, init]) => `${init?.method ?? 'GET'} ${new URL(url, 'http://localhost').pathname}`)
}

export function sesionDe(rol: Rol, id = 1, email = `${rol}@example.com`): Sesion {
  return {
    token: `token-${rol}`,
    venceEn: '2026-11-25T21:00:00Z',
    usuario: { id, email, rol, profesionalId: rol === 'profesional' ? 10 : null, profesionalNombre: null, activo: true },
  }
}

/** Renderiza la SPA completa en `ruta`, con una sesión guardada si se indica. */
export function renderizarEn(ruta: string, sesion?: Sesion) {
  if (sesion !== undefined) {
    guardarSesion(sesion)
  }
  const router = createMemoryRouter(rutas, { initialEntries: [ruta] })
  const resultado = render(<Proveedores router={router} />)
  return { ...resultado, router }
}
