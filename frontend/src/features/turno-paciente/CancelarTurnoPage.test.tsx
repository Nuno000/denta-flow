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
  estado: 'confirmado',
  puedeModificar: true,
}
const CONTACTO = { 'GET /api/publico/contacto': { status: 200, cuerpo: { contacto: 'Llamanos al 0261 555-0000' } } }

describe('pantalla /turno/:token/cancelar (US-012, RN-CA-01..03)', () => {
  afterEach(() => {
    sessionStorage.clear()
    vi.unstubAllGlobals()
  })

  it('dentro de término pide confirmar la cancelación antes de cancelar', async () => {
    const api = simularApi({ 'GET /api/publico/turnos/tok-1': { status: 200, cuerpo: TURNO } })
    renderizarEn('/turno/tok-1/cancelar')

    expect(await screen.findByRole('heading', { level: 1, name: 'Cancelá tu turno' })).toBeInTheDocument()
    expect(await screen.findByRole('heading', { level: 2, name: '¿Querés cancelar tu turno?' })).toBeInTheDocument()
    expect(screen.getByText('05/12/2026 10:00 (hora de Argentina)')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Confirmar cancelación' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Prefiero reprogramarlo' })).toHaveAttribute(
      'href',
      '/turno/tok-1/reprogramar',
    )
    expect(pedidos(api)).toEqual(['GET /api/publico/turnos/tok-1'])
  })

  it('cancela, informa el éxito y le pasa el foco al título', async () => {
    const api = simularApi({
      'GET /api/publico/turnos/tok-1': { status: 200, cuerpo: TURNO },
      'POST /api/publico/turnos/tok-1/cancelar': {
        status: 200,
        cuerpo: { ...TURNO, situacion: 'CANCELADO', estado: 'cancelado', puedeModificar: false },
      },
    })
    const user = userEvent.setup()
    renderizarEn('/turno/tok-1/cancelar')

    await user.click(await screen.findByRole('button', { name: 'Confirmar cancelación' }))

    const titulo = await screen.findByRole('heading', { level: 2, name: 'Cancelaste tu turno' })
    await waitFor(() => expect(titulo).toHaveFocus())
    expect(pedidos(api)).toContain('POST /api/publico/turnos/tok-1/cancelar')
  })

  it('fuera de término muestra una alerta con el contacto del consultorio y no ofrece cancelar', async () => {
    simularApi({ 'GET /api/publico/turnos/tok-1': { status: 200, cuerpo: { ...TURNO, puedeModificar: false } }, ...CONTACTO })
    renderizarEn('/turno/tok-1/cancelar')

    const alerta = await screen.findByRole('alert')
    expect(alerta).toHaveTextContent('El plazo para cancelar online ya venció')
    expect(await screen.findByText('Llamanos al 0261 555-0000')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Confirmar cancelación' })).not.toBeInTheDocument()
  })

  it('si el backend rechaza por fuera de término muestra su mensaje', async () => {
    simularApi({
      'GET /api/publico/turnos/tok-1': { status: 200, cuerpo: TURNO },
      'POST /api/publico/turnos/tok-1/cancelar': {
        status: 409,
        cuerpo: { detail: 'El plazo de cancelación o reprogramación online ha vencido.' },
      },
      ...CONTACTO,
    })
    const user = userEvent.setup()
    renderizarEn('/turno/tok-1/cancelar')

    await user.click(await screen.findByRole('button', { name: 'Confirmar cancelación' }))

    expect(await screen.findByText('El plazo de cancelación o reprogramación online ha vencido.')).toBeInTheDocument()
  })

  it('con teclado se llega al botón de cancelación con Tab', async () => {
    simularApi({ 'GET /api/publico/turnos/tok-1': { status: 200, cuerpo: TURNO } })
    const user = userEvent.setup()
    renderizarEn('/turno/tok-1/cancelar')
    const boton = await screen.findByRole('button', { name: 'Confirmar cancelación' })

    await user.tab()

    expect(boton).toHaveFocus()
  })
})
