import { QueryClientProvider, type QueryClient } from '@tanstack/react-query'
import { act, renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ErrorApi } from '../../lib/apiClient'
import { crearQueryClient } from '../../lib/queryClient'
import { simularApi } from '../../test/apiSimulada'
import {
  motivoDelRechazo,
  useContacto,
  useHuecosDelDia,
  usePrestacionesPublicas,
  useProfesionalesDe,
  useReservar,
  type SolicitudReserva,
} from './reservaApi'

function envoltorio(queryClient: QueryClient) {
  return function Envoltorio({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  }
}

/** URL completa (con query) y cabeceras de cada pedido recibido por la API simulada. */
function llamadas(api: ReturnType<typeof simularApi>) {
  return api.mock.calls.map(([url, init]) => ({ url: String(url), headers: new Headers(init?.headers) }))
}

const SOLICITUD: SolicitudReserva = {
  prestacionId: 2,
  profesionalId: null,
  inicio: '2026-12-02T10:00:00-03:00',
  paciente: { dni: '30111222', nombre: 'Carla', apellido: 'Ejemplo', telefono: '2615550000', email: 'carla@example.com' },
  aceptaAvisoPrivacidad: true,
}

describe('cliente de API de la reserva pública (design D3 y D6)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('lee prestaciones, profesionales de una prestación y contacto sin token', async () => {
    const api = simularApi({
      'GET /api/publico/prestaciones': { status: 200, cuerpo: [{ id: 2, nombre: 'Limpieza', duracionMinutos: 30 }] },
      'GET /api/publico/profesionales': { status: 200, cuerpo: [{ id: 3, nombre: 'Ana', apellido: 'Pérez' }] },
      'GET /api/publico/contacto': { status: 200, cuerpo: { contacto: 'Llamanos al 0261 555-0000' } },
    })
    const wrapper = envoltorio(crearQueryClient())

    const prestaciones = renderHook(() => usePrestacionesPublicas(), { wrapper })
    const profesionales = renderHook(() => useProfesionalesDe(2), { wrapper })
    const contacto = renderHook(() => useContacto(), { wrapper })

    await waitFor(() => expect(prestaciones.result.current.data).toHaveLength(1))
    await waitFor(() => expect(profesionales.result.current.data?.[0]?.apellido).toBe('Pérez'))
    await waitFor(() => expect(contacto.result.current.data?.contacto).toBe('Llamanos al 0261 555-0000'))
    expect(llamadas(api).map((l) => l.url)).toContain('/api/publico/profesionales?prestacionId=2')
    expect(llamadas(api).every((l) => !l.headers.has('Authorization'))).toBe(true)
  })

  it('no pide profesionales sin prestación elegida', () => {
    const api = simularApi({})
    renderHook(() => useProfesionalesDe(null), { wrapper: envoltorio(crearQueryClient()) })

    expect(api).not.toHaveBeenCalled()
  })

  it('pide los huecos de un solo día, con el profesional solo si se eligió', async () => {
    const api = simularApi({ 'GET /api/publico/huecos': { status: 200, cuerpo: [] } })
    const wrapper = envoltorio(crearQueryClient())

    const cualquiera = renderHook(() => useHuecosDelDia(2, null, '2026-12-02'), { wrapper })
    const conProfesional = renderHook(() => useHuecosDelDia(2, 3, '2026-12-03'), { wrapper })
    const sinDia = renderHook(() => useHuecosDelDia(2, null, null), { wrapper })

    await waitFor(() => expect(cualquiera.result.current.isSuccess).toBe(true))
    await waitFor(() => expect(conProfesional.result.current.isSuccess).toBe(true))
    expect(sinDia.result.current.fetchStatus).toBe('idle')
    expect(llamadas(api).map((l) => l.url)).toEqual([
      '/api/publico/huecos?prestacionId=2&desde=2026-12-02&hasta=2026-12-02',
      '/api/publico/huecos?prestacionId=2&desde=2026-12-03&hasta=2026-12-03&profesionalId=3',
    ])
  })

  it('reserva con POST sin token y devuelve el resumen', async () => {
    const api = simularApi({
      'POST /api/publico/reservas': (cuerpo) => ({
        status: 201,
        cuerpo: {
          inicio: (cuerpo as SolicitudReserva).inicio,
          fin: '2026-12-02T10:30:00-03:00',
          prestacion: 'Limpieza',
          profesional: 'Ana Pérez',
          estado: 'pendiente',
        },
      }),
    })
    const { result } = renderHook(() => useReservar(), { wrapper: envoltorio(crearQueryClient()) })

    let respuesta: unknown
    await act(async () => {
      respuesta = await result.current.mutateAsync(SOLICITUD)
    })

    expect(respuesta).toMatchObject({ profesional: 'Ana Pérez', estado: 'pendiente' })
    const [url, init] = api.mock.calls[0]
    expect(url).toBe('/api/publico/reservas')
    expect(JSON.parse(String(init?.body))).toEqual(SOLICITUD)
    expect(new Headers(init?.headers).has('Authorization')).toBe(false)
  })

  it('un 409 invalida los huecos para que se vuelvan a pedir', async () => {
    let pedidosDeHuecos = 0
    simularApi({
      'GET /api/publico/huecos': () => {
        pedidosDeHuecos++
        return { status: 200, cuerpo: [] }
      },
      'POST /api/publico/reservas': { status: 409, cuerpo: { detail: 'El horario elegido ya no está disponible; elegí otro' } },
    })
    const wrapper = envoltorio(crearQueryClient())
    const huecos = renderHook(() => useHuecosDelDia(2, null, '2026-12-02'), { wrapper })
    await waitFor(() => expect(huecos.result.current.isSuccess).toBe(true))
    const reservar = renderHook(() => useReservar(), { wrapper })

    await act(async () => {
      await reservar.result.current.mutateAsync(SOLICITUD).catch(() => undefined)
    })

    await waitFor(() => expect(pedidosDeHuecos).toBe(2))
    expect(motivoDelRechazo(reservar.result.current.error)).toEqual({
      motivo: 'horario-tomado',
      mensaje: 'El horario elegido ya no está disponible; elegí otro',
      campos: {},
    })
  })

  it('clasifica 429, 400 con campos y fallas de conexión', () => {
    expect(motivoDelRechazo(errorCon(429, { detail: 'Hiciste demasiados pedidos de reserva.' })).motivo).toBe(
      'demasiados-pedidos',
    )
    expect(motivoDelRechazo(errorCon(400, { detail: 'Revisá los datos marcados', errores: { email: 'Email inválido' } })))
      .toEqual({ motivo: 'datos', mensaje: 'Revisá los datos marcados', campos: { email: 'Email inválido' } })
    expect(motivoDelRechazo(new TypeError('Failed to fetch')).motivo).toBe('otro')
  })
})

function errorCon(status: number, cuerpo: unknown) {
  return new ErrorApi(status, cuerpo)
}
