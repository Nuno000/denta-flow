import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { apiClient, ErrorApi } from '../../lib/apiClient'
import { AceptarOfertaPage } from './AceptarOfertaPage'

function renderizar(ruta: string) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter initialEntries={[ruta]}>
        <AceptarOfertaPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('AceptarOfertaPage', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('muestra la oferta vigente y permite aceptarla', async () => {
    vi.spyOn(apiClient, 'get').mockResolvedValue({
      estado: 'ofrecida',
      prestacionNombre: 'Limpieza dental',
      profesionalNombre: 'Dra. Maria Lopez',
      inicioOfrecido: '2026-12-02T13:00:00Z',
      ofrecidaHasta: '2026-12-02T12:00:00Z',
      turnoCreadoId: null,
    })
    vi.spyOn(apiClient, 'post').mockResolvedValue({
      estado: 'atendida',
      prestacionNombre: 'Limpieza dental',
      profesionalNombre: 'Dra. Maria Lopez',
      inicioOfrecido: '2026-12-02T13:00:00Z',
      ofrecidaHasta: '2026-12-02T12:00:00Z',
      turnoCreadoId: 42,
    })

    renderizar('/lista-espera/aceptar?token=tok-123')

    expect(await screen.findByText('Limpieza dental')).toBeInTheDocument()
    const boton = screen.getByRole('button', { name: 'Aceptar turno' })
    await userEvent.click(boton)

    await waitFor(() => {
      expect(screen.getByText(/¡Turno confirmado!/i)).toBeInTheDocument()
    })
  })

  it('muestra mensaje claro cuando la oferta ya fue tomada (409)', async () => {
    vi.spyOn(apiClient, 'get').mockResolvedValue({
      estado: 'ofrecida',
      prestacionNombre: 'Limpieza dental',
      profesionalNombre: 'Dra. Maria Lopez',
      inicioOfrecido: '2026-12-02T13:00:00Z',
      ofrecidaHasta: '2026-12-02T12:00:00Z',
      turnoCreadoId: null,
    })
    vi.spyOn(apiClient, 'post').mockRejectedValue(
      new ErrorApi(409, 'El horario ofrecido ya fue tomado por otro paciente'),
    )

    renderizar('/lista-espera/aceptar?token=tok-123')

    const boton = await screen.findByRole('button', { name: 'Aceptar turno' })
    await userEvent.click(boton)

    expect(await screen.findByRole('alert')).toHaveTextContent(/ya fue tomado/i)
  })
})
