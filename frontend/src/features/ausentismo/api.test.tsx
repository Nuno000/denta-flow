import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { simularApi } from '../../test/apiSimulada'
import { construirConsultaAusentismo, useReporteDeAusentismo, type ReporteDeAusentismo } from './api'

const REPORTE_EJEMPLO: ReporteDeAusentismo = {
  desde: '2026-11-01',
  hasta: '2026-11-26',
  profesionalId: null,
  minimoAusencias: 2,
  general: {
    totalEvaluados: 10,
    asistidos: 7,
    ausentesSinAviso: 2,
    ausentesConAviso: 1,
    totalAusentes: 3,
    tasaAusentismo: 30.0,
  },
  porProfesional: [],
  porFranja: [],
  porDiaSemana: [],
  pacientesReiterados: [],
}

function envolver() {
  const cliente = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={cliente}>{children}</QueryClientProvider>
  )
}

describe('api de ausentismo', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('construye la URL con fechas, umbral y profesional opcional', () => {
    expect(
      construirConsultaAusentismo({
        desde: '2026-11-01',
        hasta: '2026-11-26',
        minimoAusencias: 2,
      }),
    ).toBe('/api/reportes/ausentismo?desde=2026-11-01&hasta=2026-11-26&minimoAusencias=2')

    expect(
      construirConsultaAusentismo({
        desde: '2026-11-01',
        hasta: '2026-11-26',
        profesionalId: 4,
        minimoAusencias: 3,
      }),
    ).toBe('/api/reportes/ausentismo?desde=2026-11-01&hasta=2026-11-26&minimoAusencias=3&profesionalId=4')
  })

  it('consulta GET /api/reportes/ausentismo y devuelve el reporte tipado', async () => {
    const fetchMock = simularApi({
      'GET /api/reportes/ausentismo': {
        status: 200,
        cuerpo: REPORTE_EJEMPLO,
      },
    })

    const { result } = renderHook(
      () =>
        useReporteDeAusentismo({
          desde: '2026-11-01',
          hasta: '2026-11-26',
          minimoAusencias: 2,
        }),
      { wrapper: envolver() },
    )

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(result.current.data?.general.tasaAusentismo).toBe(30.0)
    expect(String(fetchMock.mock.calls[0][0])).toContain(
      '/api/reportes/ausentismo?desde=2026-11-01&hasta=2026-11-26&minimoAusencias=2',
    )
  })
})
