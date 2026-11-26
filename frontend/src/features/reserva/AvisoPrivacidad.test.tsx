import { zodResolver } from '@hookform/resolvers/zod'
import { QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useForm } from 'react-hook-form'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { z } from 'zod'
import { crearQueryClient } from '../../lib/queryClient'
import { pedidos, simularApi } from '../../test/apiSimulada'
import { AvisoPrivacidad } from './AvisoPrivacidad'
import { MENSAJE_AVISO_NO_ACEPTADO, esquemaAvisoPrivacidad } from './avisoPrivacidadApi'

const AVISO = {
  texto: 'Usamos tu DNI, nombre, teléfono y email solo para gestionar tus turnos.',
  leyenda: 'Leí y acepto el aviso de privacidad',
}

type Datos = z.infer<typeof esquemaAvisoPrivacidad>

/** Formulario mínimo que incrusta el aviso como lo hará la reserva de C-16. */
function FormularioDePrueba({ alEnviar }: { alEnviar: (datos: Datos) => void }) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<Datos>({ resolver: zodResolver(esquemaAvisoPrivacidad), defaultValues: { aceptaAvisoPrivacidad: false } })
  return (
    <form onSubmit={handleSubmit(alEnviar)} noValidate>
      <AvisoPrivacidad registro={register('aceptaAvisoPrivacidad')} error={errors.aceptaAvisoPrivacidad?.message} />
      <button type="submit">Reservar</button>
    </form>
  )
}

function renderizar(alEnviar: (datos: Datos) => void = () => undefined) {
  render(
    <QueryClientProvider client={crearQueryClient()}>
      <FormularioDePrueba alEnviar={alEnviar} />
    </QueryClientProvider>,
  )
}

describe('AvisoPrivacidad (US-040, spec "Aceptación obligatoria del aviso al reservar")', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('muestra la leyenda como etiqueta de la casilla y el texto del aviso, sin token', async () => {
    const api = simularApi({ 'GET /api/publico/aviso-privacidad': { status: 200, cuerpo: AVISO } })
    renderizar()

    const casilla = await screen.findByRole('checkbox', { name: AVISO.leyenda })
    expect(casilla).not.toBeChecked()
    expect(screen.getByText(AVISO.texto)).toBeInTheDocument()
    expect(pedidos(api)).toEqual(['GET /api/publico/aviso-privacidad'])
    expect(new Headers(api.mock.calls[0]?.[1]?.headers).has('Authorization')).toBe(false)
  })

  it('sin marcar no envía, muestra el error asociado y pone el foco en la casilla', async () => {
    simularApi({ 'GET /api/publico/aviso-privacidad': { status: 200, cuerpo: AVISO } })
    const alEnviar = vi.fn()
    renderizar(alEnviar)
    const casilla = await screen.findByRole('checkbox', { name: AVISO.leyenda })

    await userEvent.click(screen.getByRole('button', { name: 'Reservar' }))

    const error = await screen.findByText(MENSAJE_AVISO_NO_ACEPTADO)
    expect(alEnviar).not.toHaveBeenCalled()
    expect(casilla).toHaveAttribute('aria-invalid', 'true')
    expect(casilla).toHaveAttribute('aria-describedby', error.id)
    expect(casilla).toHaveFocus()
  })

  it('marcada, envía aceptaAvisoPrivacidad en true', async () => {
    simularApi({ 'GET /api/publico/aviso-privacidad': { status: 200, cuerpo: AVISO } })
    const alEnviar = vi.fn()
    renderizar(alEnviar)

    await userEvent.click(await screen.findByRole('checkbox', { name: AVISO.leyenda }))
    await userEvent.click(screen.getByRole('button', { name: 'Reservar' }))

    await vi.waitFor(() => expect(alEnviar).toHaveBeenCalledTimes(1))
    expect(alEnviar.mock.calls[0]?.[0]).toEqual({ aceptaAvisoPrivacidad: true })
  })

  it('si el aviso no carga, avisa y permite reintentar', async () => {
    let falla = true
    simularApi({
      'GET /api/publico/aviso-privacidad': () => (falla ? { status: 503 } : { status: 200, cuerpo: AVISO }),
    })
    renderizar()

    // El QueryClient de la SPA reintenta una vez los 5xx antes de mostrar el error.
    expect(await screen.findByRole('alert', {}, { timeout: 4000 })).toHaveTextContent('No pudimos cargar el aviso de privacidad')
    falla = false
    await userEvent.click(screen.getByRole('button', { name: 'Reintentar' }))

    expect(await screen.findByRole('checkbox', { name: AVISO.leyenda })).toBeInTheDocument()
  })
})
