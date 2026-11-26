import { QueryClientProvider, type QueryClient } from '@tanstack/react-query'
import { act, renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { crearQueryClient } from '../../lib/queryClient'
import { pedidos, simularApi } from '../../test/apiSimulada'
import {
  CLAVES_RECURSOS,
  useAsignarPrestaciones,
  useCrearSillon,
  useDesactivarPrestacion,
  usePrestaciones,
  useProfesionales,
  useSillones,
} from './api'

function envoltorio(queryClient: QueryClient) {
  return function Envoltorio({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  }
}

describe('cliente de API de recursos (design D6)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('lista sillones, prestaciones y profesionales desde sus rutas', async () => {
    const api = simularApi({
      'GET /api/sillones': { status: 200, cuerpo: [{ id: 1, nombre: 'Sillón 1', activo: true }] },
      'GET /api/prestaciones': { status: 200, cuerpo: [{ id: 2, nombre: 'Limpieza', duracionMinutos: 40, activa: true }] },
      'GET /api/profesionales': { status: 200, cuerpo: [] },
    })
    const wrapper = envoltorio(crearQueryClient())

    const sillones = renderHook(() => useSillones(), { wrapper })
    const prestaciones = renderHook(() => usePrestaciones(), { wrapper })
    const profesionales = renderHook(() => useProfesionales(), { wrapper })

    await waitFor(() => expect(sillones.result.current.data).toHaveLength(1))
    await waitFor(() => expect(prestaciones.result.current.data?.[0]?.duracionMinutos).toBe(40))
    await waitFor(() => expect(profesionales.result.current.isSuccess).toBe(true))
    expect(pedidos(api)).toEqual(
      expect.arrayContaining(['GET /api/sillones', 'GET /api/prestaciones', 'GET /api/profesionales']),
    )
  })

  it('crear un sillón hace POST e invalida solo el listado de sillones', async () => {
    const api = simularApi({
      'POST /api/sillones': (cuerpo) => ({ status: 201, cuerpo: { id: 3, ...(cuerpo as object), activo: true } }),
    })
    const queryClient = crearQueryClient()
    const invalidar = vi.spyOn(queryClient, 'invalidateQueries')
    const { result } = renderHook(() => useCrearSillon(), { wrapper: envoltorio(queryClient) })

    await act(() => result.current.mutateAsync({ nombre: 'Sillón 3' }))

    expect(pedidos(api)).toEqual(['POST /api/sillones'])
    expect(api.mock.calls[0]?.[1]?.body).toBe(JSON.stringify({ nombre: 'Sillón 3' }))
    expect(invalidar).toHaveBeenCalledWith({ queryKey: CLAVES_RECURSOS.sillones })
    expect(invalidar).toHaveBeenCalledTimes(1)
  })

  it('asignar prestaciones hace PUT con el conjunto e invalida profesionales', async () => {
    const api = simularApi({
      'PUT /api/profesionales/7/prestaciones': { status: 200, cuerpo: { id: 7, prestacionIds: [1, 2] } },
    })
    const queryClient = crearQueryClient()
    const invalidar = vi.spyOn(queryClient, 'invalidateQueries')
    const { result } = renderHook(() => useAsignarPrestaciones(), { wrapper: envoltorio(queryClient) })

    await act(() => result.current.mutateAsync({ id: 7, prestacionIds: [1, 2] }))

    expect(pedidos(api)).toEqual(['PUT /api/profesionales/7/prestaciones'])
    expect(api.mock.calls[0]?.[1]?.body).toBe(JSON.stringify({ prestacionIds: [1, 2] }))
    expect(invalidar).toHaveBeenCalledWith({ queryKey: CLAVES_RECURSOS.profesionales })
  })

  it('desactivar una prestación invalida prestaciones y profesionales', async () => {
    simularApi({
      'POST /api/prestaciones/2/desactivar': {
        status: 200,
        cuerpo: { id: 2, nombre: 'Limpieza', duracionMinutos: 40, activa: false },
      },
    })
    const queryClient = crearQueryClient()
    const invalidar = vi.spyOn(queryClient, 'invalidateQueries')
    const { result } = renderHook(() => useDesactivarPrestacion(), { wrapper: envoltorio(queryClient) })

    await act(() => result.current.mutateAsync(2))

    expect(invalidar).toHaveBeenCalledWith({ queryKey: CLAVES_RECURSOS.prestaciones })
    expect(invalidar).toHaveBeenCalledWith({ queryKey: CLAVES_RECURSOS.profesionales })
  })
})
