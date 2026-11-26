import { QueryClientProvider, type QueryClient } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ErrorApi } from '../../lib/apiClient'
import { crearQueryClient } from '../../lib/queryClient'
import { simularApi } from '../../test/apiSimulada'
import { TURNOS_DEL_1_12, consultasDeAgenda } from '../../test/datosAgenda'
import { RUTA_AGENDA, construirConsultaAgenda, useTurnosDeAgenda } from './api'

function envoltorio(queryClient: QueryClient) {
  return function Envoltorio({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  }
}

const UN_DIA = { desde: '2026-12-01', hasta: '2026-12-01' }

describe('cliente de la agenda (design D1 y D3)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  describe('construirConsultaAgenda', () => {
    it('incluye siempre el rango', () => {
      expect(construirConsultaAgenda(UN_DIA, {})).toBe(`${RUTA_AGENDA}?desde=2026-12-01&hasta=2026-12-01`)
    })

    it('agrega los filtros presentes y omite los vacíos', () => {
      expect(
        construirConsultaAgenda(
          { desde: '2026-11-30', hasta: '2026-12-06' },
          { profesionalId: 10, sillonId: undefined, estado: 'pendiente' },
        ),
      ).toBe(`${RUTA_AGENDA}?desde=2026-11-30&hasta=2026-12-06&profesionalId=10&estado=pendiente`)
    })
  })

  describe('useTurnosDeAgenda', () => {
    it('devuelve los turnos del rango pedido', async () => {
      simularApi({ [`GET ${RUTA_AGENDA}`]: { status: 200, cuerpo: TURNOS_DEL_1_12 } })
      const { result } = renderHook(() => useTurnosDeAgenda(UN_DIA, {}), { wrapper: envoltorio(crearQueryClient()) })

      await waitFor(() => expect(result.current.data).toHaveLength(TURNOS_DEL_1_12.length))
      expect(result.current.data?.[0]?.paciente).toEqual({ nombre: 'Ana', apellido: 'Ejemplar' })
    })

    it('envía el rango y los filtros en la URL', async () => {
      const api = simularApi({ [`GET ${RUTA_AGENDA}`]: { status: 200, cuerpo: [] } })
      const { result } = renderHook(() => useTurnosDeAgenda(UN_DIA, { sillonId: 2, estado: 'cancelado' }), {
        wrapper: envoltorio(crearQueryClient()),
      })

      await waitFor(() => expect(result.current.isSuccess).toBe(true))
      const [consulta] = consultasDeAgenda(api)
      expect(Object.fromEntries(consulta)).toEqual({
        desde: '2026-12-01',
        hasta: '2026-12-01',
        sillonId: '2',
        estado: 'cancelado',
      })
    })

    it('un 403 queda como error sin reintentar', async () => {
      const api = simularApi({ [`GET ${RUTA_AGENDA}`]: { status: 403, cuerpo: { detail: 'Sin permiso' } } })
      const { result } = renderHook(() => useTurnosDeAgenda(UN_DIA, { profesionalId: 11 }), {
        wrapper: envoltorio(crearQueryClient()),
      })

      await waitFor(() => expect(result.current.isError).toBe(true))
      expect(result.current.error).toBeInstanceOf(ErrorApi)
      expect((result.current.error as ErrorApi).status).toBe(403)
      expect(consultasDeAgenda(api)).toHaveLength(1)
    })

    it('un 500 deja el hook en error', async () => {
      simularApi({ [`GET ${RUTA_AGENDA}`]: { status: 500 } })
      const { result } = renderHook(() => useTurnosDeAgenda(UN_DIA, {}), { wrapper: envoltorio(crearQueryClient()) })

      await waitFor(() => expect(result.current.isError).toBe(true), { timeout: 4000 })
    })
  })
})
