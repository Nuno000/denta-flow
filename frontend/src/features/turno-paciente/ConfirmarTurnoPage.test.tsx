import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { pedidos, renderizarEn, simularApi } from '../../test/apiSimulada'

const TURNO = {
  situacion: 'VIGENTE',
  inicio: '2026-12-05T10:00:00-03:00',
  fin: '2026-12-05T10:30:00-03:00',
  prestacion: 'Limpieza',
  profesional: 'Ana Pérez',
  estado: 'pendiente',
  puedeModificar: true,
}

describe('pantalla /turno/:token/confirmar (US-011, design Q1 opción B)', () => {
  afterEach(() => {
    sessionStorage.clear()
    vi.unstubAllGlobals()
  })

  it('muestra los datos del turno y no confirma solo por abrir el enlace', async () => {
    const api = simularApi({ 'GET /api/publico/turnos/tok-1': { status: 200, cuerpo: TURNO } })
    renderizarEn('/turno/tok-1/confirmar')

    expect(await screen.findByRole('heading', { level: 1, name: 'Confirmá tu turno' })).toBeInTheDocument()
    expect(await screen.findByText('05/12/2026 10:00 (hora de Argentina)')).toBeInTheDocument()
    expect(screen.getByText('Limpieza')).toBeInTheDocument()
    expect(screen.getByText('Ana Pérez')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Confirmar mi turno' })).toBeInTheDocument()
    expect(pedidos(api)).toEqual(['GET /api/publico/turnos/tok-1'])
  })

  it('confirma con un toque, muestra "¡Turno confirmado!" y le pasa el foco', async () => {
    const api = simularApi({
      'GET /api/publico/turnos/tok-1': { status: 200, cuerpo: TURNO },
      'POST /api/publico/turnos/tok-1/confirmar': { status: 200, cuerpo: { ...TURNO, estado: 'confirmado' } },
    })
    const user = userEvent.setup()
    renderizarEn('/turno/tok-1/confirmar')

    await user.click(await screen.findByRole('button', { name: 'Confirmar mi turno' }))

    const titulo = await screen.findByRole('heading', { level: 2, name: '¡Turno confirmado!' })
    await waitFor(() => expect(titulo).toHaveFocus())
    expect(pedidos(api)).toContain('POST /api/publico/turnos/tok-1/confirmar')
  })

  it('un turno ya confirmado lo informa sin ofrecer el botón', async () => {
    simularApi({ 'GET /api/publico/turnos/tok-1': { status: 200, cuerpo: { ...TURNO, estado: 'confirmado' } } })
    renderizarEn('/turno/tok-1/confirmar')

    expect(await screen.findByRole('heading', { level: 2, name: 'Tu turno ya está confirmado' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Confirmar mi turno' })).not.toBeInTheDocument()
  })

  it('un turno cancelado muestra un aviso explicativo', async () => {
    simularApi({
      'GET /api/publico/turnos/tok-1': {
        status: 200,
        cuerpo: { ...TURNO, situacion: 'CANCELADO', estado: 'cancelado', puedeModificar: false },
      },
      'GET /api/publico/contacto': { status: 200, cuerpo: { contacto: 'Llamanos al 0261 555-0000' } },
    })
    renderizarEn('/turno/tok-1/confirmar')

    expect(await screen.findByRole('heading', { level: 2, name: 'Este turno fue cancelado' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Confirmar mi turno' })).not.toBeInTheDocument()
    expect(await screen.findByRole('link', { name: 'Reservar un turno nuevo' })).toHaveAttribute('href', '/reservar')
  })

  it('un turno vencido muestra un aviso explicativo', async () => {
    simularApi({
      'GET /api/publico/turnos/tok-1': { status: 200, cuerpo: { ...TURNO, situacion: 'VENCIDO', puedeModificar: false } },
      'GET /api/publico/contacto': { status: 200, cuerpo: { contacto: 'Llamanos al 0261 555-0000' } },
    })
    renderizarEn('/turno/tok-1/confirmar')

    expect(await screen.findByRole('heading', { level: 2, name: 'Este turno ya pasó' })).toBeInTheDocument()
  })

  it('un enlace inválido muestra que no es válido', async () => {
    simularApi({ 'GET /api/publico/contacto': { status: 200, cuerpo: { contacto: 'Llamanos al 0261 555-0000' } } })
    renderizarEn('/turno/nada/confirmar')

    expect(await screen.findByRole('heading', { level: 2, name: 'El enlace no es válido' })).toBeInTheDocument()
    expect(await screen.findByText('Llamanos al 0261 555-0000')).toBeInTheDocument()
  })

  it('si la confirmación falla muestra el motivo en una alerta', async () => {
    simularApi({
      'GET /api/publico/turnos/tok-1': { status: 200, cuerpo: TURNO },
      'POST /api/publico/turnos/tok-1/confirmar': { status: 409, cuerpo: { detail: 'El turno ya ha vencido o fue atendido' } },
    })
    const user = userEvent.setup()
    renderizarEn('/turno/tok-1/confirmar')

    await user.click(await screen.findByRole('button', { name: 'Confirmar mi turno' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('El turno ya ha vencido o fue atendido')
  })
})
