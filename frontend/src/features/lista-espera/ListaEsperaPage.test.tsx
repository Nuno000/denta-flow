import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { apiClient } from '../../lib/apiClient'
import { ListaEsperaPage } from './ListaEsperaPage'

vi.mock('../auth/BarraDelPanel', () => ({
  BarraDelPanel: () => <div data-testid="barra-panel" />,
}))

describe('ListaEsperaPage', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('muestra el listado de entradas de lista de espera', async () => {
    vi.spyOn(apiClient, 'get').mockImplementation(async (url: string) => {
      if (url.startsWith('/api/lista-espera')) {
        return [
          {
            id: 1,
            pacienteId: 10,
            pacienteNombre: 'Ana Gomez',
            pacienteDni: '30123456',
            prestacionId: 2,
            prestacionNombre: 'Limpieza',
            profesionalId: null,
            profesionalNombre: null,
            fechaDesde: '2026-12-01',
            fechaHasta: '2026-12-05',
            horaDesde: null,
            horaHasta: null,
            estado: 'activa',
            inicioOfrecido: null,
            ofrecidaHasta: null,
            creadoEn: '2026-12-01T12:00:00Z',
          },
        ]
      }
      return []
    })

    const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(
      <QueryClientProvider client={qc}>
        <MemoryRouter>
          <ListaEsperaPage />
        </MemoryRouter>
      </QueryClientProvider>,
    )

    expect(await screen.findByText(/Ana Gomez — Limpieza/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Dar de baja' })).toBeInTheDocument()
  })
})
