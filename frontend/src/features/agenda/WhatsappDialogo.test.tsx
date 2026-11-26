import { QueryClientProvider } from '@tanstack/react-query'
import { render, renderHook, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactNode } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { crearQueryClient } from '../../lib/queryClient'
import { pedidos, simularApi } from '../../test/apiSimulada'
import { TURNOS_DEL_1_12 } from '../../test/datosAgenda'
import { useMensajeWhatsapp } from './api'
import { WhatsappDialogo } from './WhatsappDialogo'

const TURNO = TURNOS_DEL_1_12[0]
const RUTA = `GET /api/turnos/${TURNO.id}/whatsapp`
const MENSAJE = 'Hola Ana, te recordamos tu turno de Consulta el 01/12/2026 a las 09:00'
const ENLACE = 'https://wa.me/5491155550000?text=Hola%20Ana'

function envoltorio({ children }: { children: ReactNode }) {
  return <QueryClientProvider client={crearQueryClient()}>{children}</QueryClientProvider>
}

describe('mensaje de WhatsApp de un turno (C-21, spec mensajes-whatsapp)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('useMensajeWhatsapp pide el mensaje del turno y no consulta sin turno', async () => {
    const api = simularApi({ [RUTA]: { status: 200, cuerpo: { mensaje: MENSAJE, enlace: ENLACE } } })

    renderHook(() => useMensajeWhatsapp(null), { wrapper: envoltorio })
    expect(api).not.toHaveBeenCalled()

    const { result } = renderHook(() => useMensajeWhatsapp(TURNO.id), { wrapper: envoltorio })
    await waitFor(() => expect(result.current.data?.enlace).toBe(ENLACE))
    expect(pedidos(api)).toEqual([RUTA])
  })

  it('muestra la vista previa y el enlace "Abrir WhatsApp" en una pestaña nueva', async () => {
    simularApi({ [RUTA]: { status: 200, cuerpo: { mensaje: MENSAJE, enlace: ENLACE } } })
    render(<WhatsappDialogo turno={TURNO} alCerrar={() => {}} />, { wrapper: envoltorio })

    const dialogo = await screen.findByRole('dialog', { name: /WhatsApp — Ana Ejemplar/ })
    expect(await within(dialogo).findByText(MENSAJE)).toBeInTheDocument()
    const enlace = within(dialogo).getByRole('link', { name: 'Abrir WhatsApp' })
    expect(enlace).toHaveAttribute('href', ENLACE)
    expect(enlace).toHaveAttribute('target', '_blank')
    expect(enlace).toHaveAttribute('rel', 'noopener noreferrer')
  })

  it('sin teléfono válido avisa, muestra el mensaje y no ofrece el enlace', async () => {
    simularApi({ [RUTA]: { status: 200, cuerpo: { mensaje: MENSAJE, enlace: null } } })
    render(<WhatsappDialogo turno={TURNO} alCerrar={() => {}} />, { wrapper: envoltorio })

    expect(await screen.findByRole('alert')).toHaveTextContent('no tiene un teléfono válido para WhatsApp')
    expect(screen.getByText(MENSAJE)).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Abrir WhatsApp' })).not.toBeInTheDocument()
  })

  it('si la API falla muestra el motivo y permite reintentar', async () => {
    let intentos = 0
    simularApi({
      [RUTA]: () => {
        intentos += 1
        return intentos === 1
          ? { status: 409, cuerpo: { detail: 'Solo se envían por WhatsApp turnos pendientes o confirmados' } }
          : { status: 200, cuerpo: { mensaje: MENSAJE, enlace: ENLACE } }
      },
    })
    const usuario = userEvent.setup()
    render(<WhatsappDialogo turno={TURNO} alCerrar={() => {}} />, { wrapper: envoltorio })

    expect(await screen.findByRole('alert')).toHaveTextContent('Solo se envían por WhatsApp turnos pendientes o confirmados')
    await usuario.click(screen.getByRole('button', { name: 'Reintentar' }))

    expect(await screen.findByRole('link', { name: 'Abrir WhatsApp' })).toBeInTheDocument()
  })

  it('"Cerrar" y Escape cierran el diálogo', async () => {
    simularApi({ [RUTA]: { status: 200, cuerpo: { mensaje: MENSAJE, enlace: ENLACE } } })
    const alCerrar = vi.fn()
    const usuario = userEvent.setup()
    render(<WhatsappDialogo turno={TURNO} alCerrar={alCerrar} />, { wrapper: envoltorio })

    await usuario.click(await screen.findByRole('button', { name: 'Cerrar' }))
    await usuario.keyboard('{Escape}')

    expect(alCerrar).toHaveBeenCalledTimes(2)
  })
})
