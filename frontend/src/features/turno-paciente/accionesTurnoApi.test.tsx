import { QueryClientProvider, type QueryClient } from '@tanstack/react-query'
import { act, renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ErrorApi } from '../../lib/apiClient'
import { crearQueryClient } from '../../lib/queryClient'
import { simularApi } from '../../test/apiSimulada'
import {
  CLAVES_TURNO,
  motivoDelError,
  useCancelarTurno,
  useConfirmarTurno,
  useHuecosReprogramacion,
  useReprogramarTurno,
  useTurnoPublico,
  type TurnoPublico,
} from './accionesTurnoApi'

function envoltorio(queryClient: QueryClient) {
  return function Envoltorio({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  }
}

function llamadas(api: ReturnType<typeof simularApi>) {
  return api.mock.calls.map(([url, init]) => ({
    url: String(url),
    metodo: init?.method ?? 'GET',
    headers: new Headers(init?.headers),
    cuerpo: typeof init?.body === 'string' ? (JSON.parse(init.body) as unknown) : undefined,
  }))
}

const TURNO: TurnoPublico = {
  situacion: 'VIGENTE',
  inicio: '2026-12-05T10:00:00-03:00',
  fin: '2026-12-05T10:30:00-03:00',
  prestacion: 'Limpieza',
  profesional: 'Ana Pérez',
  estado: 'pendiente',
  puedeModificar: true,
}

describe('cliente de API de la gestión del turno por el paciente (C-17 design D2, D4)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('consulta el turno por token sin mandar Authorization', async () => {
    const api = simularApi({ 'GET /api/publico/turnos/tok-1': { status: 200, cuerpo: TURNO } })
    const { result } = renderHook(() => useTurnoPublico('tok-1'), { wrapper: envoltorio(crearQueryClient()) })

    await waitFor(() => expect(result.current.data).toEqual(TURNO))
    expect(llamadas(api)[0].url).toBe('/api/publico/turnos/tok-1')
    expect(llamadas(api)[0].headers.has('Authorization')).toBe(false)
  })

  it('un token inexistente queda como error 404 y no se reintenta', async () => {
    const api = simularApi({})
    const { result } = renderHook(() => useTurnoPublico('nada'), { wrapper: envoltorio(crearQueryClient()) })

    await waitFor(() => expect(result.current.isError).toBe(true))
    expect(motivoDelError(result.current.error).motivo).toBe('no-encontrado')
    expect(api).toHaveBeenCalledTimes(1)
  })

  it('confirmar y cancelar hacen POST al endpoint del token y actualizan el turno en caché', async () => {
    const api = simularApi({
      'POST /api/publico/turnos/tok-1/confirmar': { status: 200, cuerpo: { ...TURNO, estado: 'confirmado' } },
      'POST /api/publico/turnos/tok-1/cancelar': {
        status: 200,
        cuerpo: { ...TURNO, situacion: 'CANCELADO', estado: 'cancelado', puedeModificar: false },
      },
    })
    const queryClient = crearQueryClient()
    const wrapper = envoltorio(queryClient)
    const confirmar = renderHook(() => useConfirmarTurno('tok-1'), { wrapper })
    const cancelar = renderHook(() => useCancelarTurno('tok-1'), { wrapper })

    await act(() => confirmar.result.current.mutateAsync())
    expect(queryClient.getQueryData<TurnoPublico>(CLAVES_TURNO.turno('tok-1'))?.estado).toBe('confirmado')
    await act(() => cancelar.result.current.mutateAsync())
    expect(queryClient.getQueryData<TurnoPublico>(CLAVES_TURNO.turno('tok-1'))?.estado).toBe('cancelado')
    expect(llamadas(api).map((l) => `${l.metodo} ${l.url}`)).toEqual([
      'POST /api/publico/turnos/tok-1/confirmar',
      'POST /api/publico/turnos/tok-1/cancelar',
    ])
  })

  it('consulta huecos de un día para reprogramar con desde = hasta = fecha', async () => {
    const api = simularApi({
      'GET /api/publico/turnos/tok-1/huecos': {
        status: 200,
        cuerpo: [{ inicio: '2026-12-07T10:00:00-03:00', fin: '2026-12-07T10:30:00-03:00', profesionalId: null }],
      },
    })
    const { result } = renderHook(() => useHuecosReprogramacion('tok-1', '2026-12-07'), {
      wrapper: envoltorio(crearQueryClient()),
    })

    await waitFor(() => expect(result.current.data).toHaveLength(1))
    expect(llamadas(api)[0].url).toBe('/api/publico/turnos/tok-1/huecos?desde=2026-12-07&hasta=2026-12-07')
  })

  it('no consulta huecos sin fecha elegida', () => {
    const api = simularApi({})
    renderHook(() => useHuecosReprogramacion('tok-1', null), { wrapper: envoltorio(crearQueryClient()) })

    expect(api).not.toHaveBeenCalled()
  })

  it('reprogramar manda el nuevo inicio y ante 409 vuelve a pedir los huecos', async () => {
    const api = simularApi({
      'POST /api/publico/turnos/tok-1/reprogramar': {
        status: 409,
        cuerpo: { detail: 'El horario elegido ya no está disponible; elegí otro' },
      },
    })
    const queryClient = crearQueryClient()
    const invalidar = vi.spyOn(queryClient, 'invalidateQueries')
    const { result } = renderHook(() => useReprogramarTurno('tok-1'), { wrapper: envoltorio(queryClient) })

    const error = await act(() =>
      result.current.mutateAsync('2026-12-07T10:00:00-03:00').then(
        () => null,
        (e: unknown) => e,
      ),
    )

    expect(error).toBeInstanceOf(ErrorApi)
    expect(motivoDelError(error)).toEqual({
      motivo: 'conflicto',
      mensaje: 'El horario elegido ya no está disponible; elegí otro',
    })
    expect(llamadas(api)[0].cuerpo).toEqual({ inicio: '2026-12-07T10:00:00-03:00' })
    expect(invalidar).toHaveBeenCalledWith({ queryKey: CLAVES_TURNO.huecos('tok-1') })
  })

  it('traduce 429 y errores de red', () => {
    expect(motivoDelError(new ErrorApi(429, { detail: 'Demasiados pedidos' })).motivo).toBe('demasiados-pedidos')
    expect(motivoDelError(new TypeError('red')).motivo).toBe('otro')
  })
})
