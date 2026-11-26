import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { pedidos, renderizarEn, simularApi, type RespuestaSimulada } from '../../test/apiSimulada'

const TURNO = {
  situacion: 'VIGENTE',
  inicio: '2026-12-05T10:00:00-03:00',
  fin: '2026-12-05T10:30:00-03:00',
  prestacion: 'Limpieza',
  profesional: 'Ana Pérez',
  estado: 'pendiente',
  puedeModificar: true,
}
const HUECOS = [
  { inicio: '2026-12-07T11:00:00-03:00', fin: '2026-12-07T11:30:00-03:00', profesionalId: null },
  { inicio: '2026-12-07T11:30:00-03:00', fin: '2026-12-07T12:00:00-03:00', profesionalId: null },
]
const CONTACTO = { 'GET /api/publico/contacto': { status: 200, cuerpo: { contacto: 'Llamanos al 0261 555-0000' } } }

type Manejador = RespuestaSimulada | ((cuerpo: unknown) => RespuestaSimulada)

function api(reprogramar: Manejador, huecos: () => unknown[] = () => HUECOS) {
  return simularApi({
    'GET /api/publico/turnos/tok-1': { status: 200, cuerpo: TURNO },
    'GET /api/publico/turnos/tok-1/huecos': () => ({ status: 200, cuerpo: huecos() }),
    'POST /api/publico/turnos/tok-1/reprogramar': reprogramar,
    ...CONTACTO,
  })
}

async function elegirPrimerHorario(user: ReturnType<typeof userEvent.setup>) {
  const dias = await screen.findByRole('group', { name: 'Días' })
  await user.click(within(dias).getAllByRole('button')[0])
  const horarios = await screen.findByRole('group', { name: 'Horarios' })
  await user.click(within(horarios).getByRole('button', { name: '11:00' }))
}

describe('pantalla /turno/:token/reprogramar (US-012, design D4)', () => {
  afterEach(() => {
    sessionStorage.clear()
    vi.unstubAllGlobals()
  })

  it('elige día y horario, confirma el nuevo horario y muestra el éxito con foco', async () => {
    const simulada = api((cuerpo) => ({
      status: 200,
      cuerpo: { ...TURNO, inicio: (cuerpo as { inicio: string }).inicio, fin: '2026-12-07T11:30:00-03:00' },
    }))
    const user = userEvent.setup()
    renderizarEn('/turno/tok-1/reprogramar')

    expect(await screen.findByRole('heading', { level: 1, name: 'Reprogramá tu turno' })).toBeInTheDocument()
    expect(await screen.findByRole('heading', { level: 2, name: 'Elegí un nuevo día y horario' })).toBeInTheDocument()
    await elegirPrimerHorario(user)

    const confirmacion = await screen.findByRole('heading', { level: 2, name: 'Confirmá el nuevo horario' })
    await waitFor(() => expect(confirmacion).toHaveFocus())
    expect(screen.getByText('07/12/2026 11:00 (hora de Argentina)')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Confirmar nuevo horario' }))

    const listo = await screen.findByRole('heading', { level: 2, name: '¡Reprogramaste tu turno!' })
    await waitFor(() => expect(listo).toHaveFocus())
    expect(screen.getByText('07/12/2026 11:00 (hora de Argentina)')).toBeInTheDocument()
    const envio = simulada.mock.calls.find(([, init]) => init?.method === 'POST')
    expect(JSON.parse(String(envio?.[1]?.body))).toEqual({ inicio: '2026-12-07T11:00:00-03:00' })
  })

  it('ante 409 vuelve a la elección, avisa y vuelve a pedir los horarios', async () => {
    let llamadasHuecos = 0
    const simulada = api({ status: 409, cuerpo: { detail: 'El horario elegido ya no está disponible; elegí otro' } }, () => {
      llamadasHuecos += 1
      return llamadasHuecos === 1 ? HUECOS : HUECOS.slice(1)
    })
    const user = userEvent.setup()
    renderizarEn('/turno/tok-1/reprogramar')

    await elegirPrimerHorario(user)
    await user.click(await screen.findByRole('button', { name: 'Confirmar nuevo horario' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('El horario elegido ya no está disponible; elegí otro')
    expect(screen.getByRole('heading', { level: 2, name: 'Elegí un nuevo día y horario' })).toBeInTheDocument()
    await waitFor(() => expect(screen.queryByRole('button', { name: '11:00' })).not.toBeInTheDocument())
    expect(screen.getByRole('button', { name: '11:30' })).toBeInTheDocument()
    expect(pedidos(simulada).filter((p) => p === 'GET /api/publico/turnos/tok-1/huecos').length).toBeGreaterThanOrEqual(2)
  })

  it('se puede volver a elegir otro horario desde la confirmación', async () => {
    api({ status: 200, cuerpo: TURNO })
    const user = userEvent.setup()
    renderizarEn('/turno/tok-1/reprogramar')

    await elegirPrimerHorario(user)
    await user.click(await screen.findByRole('button', { name: 'Elegir otro horario' }))

    expect(await screen.findByRole('heading', { level: 2, name: 'Elegí un nuevo día y horario' })).toBeInTheDocument()
  })

  it('fuera de término muestra la alerta con el contacto y no ofrece horarios', async () => {
    const simulada = simularApi({
      'GET /api/publico/turnos/tok-1': { status: 200, cuerpo: { ...TURNO, puedeModificar: false } },
      ...CONTACTO,
    })
    renderizarEn('/turno/tok-1/reprogramar')

    expect(await screen.findByRole('alert')).toHaveTextContent('El plazo para reprogramar online ya venció')
    expect(await screen.findByText('Llamanos al 0261 555-0000')).toBeInTheDocument()
    expect(screen.queryByRole('group', { name: 'Días' })).not.toBeInTheDocument()
    expect(pedidos(simulada)).not.toContain('GET /api/publico/turnos/tok-1/huecos')
  })
})
