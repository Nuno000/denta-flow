import { QueryClientProvider, useQuery } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { apiClient } from '../lib/apiClient'
import { crearQueryClient } from '../lib/queryClient'
import { AvisoDeArranque, MENSAJE_ARRANQUE } from './AvisoDeArranque'

function Consulta() {
  const consulta = useQuery({ queryKey: ['algo'], queryFn: () => apiClient.get<{ ok: boolean }>('/api/algo') })
  return <p>{consulta.isSuccess ? 'Listo' : 'Cargando'}</p>
}

function renderizar() {
  render(
    <QueryClientProvider client={crearQueryClient()}>
      <AvisoDeArranque demoraMs={50} />
      <Consulta />
    </QueryClientProvider>,
  )
}

describe('AvisoDeArranque (spec despliegue-publico, "Arranque en frío tolerado")', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('si la API tarda, avisa que el sistema está despertando', async () => {
    vi.stubGlobal('fetch', vi.fn(() => new Promise<Response>(() => undefined)))
    renderizar()

    expect(await screen.findByRole('status')).toHaveTextContent(MENSAJE_ARRANQUE)
  })

  it('si la API responde rápido, no muestra nada', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => new Response('{"ok":true}', { headers: { 'Content-Type': 'application/json' } })))
    renderizar()

    expect(await screen.findByText('Listo')).toBeInTheDocument()
    await new Promise((resolver) => setTimeout(resolver, 100))
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })
})
