import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { renderizarEn, simularApi } from '../../test/apiSimulada'
import {
  TURNOS_DEL_1_12,
  TURNO_DEL_3_12,
  apiDeAgenda,
  consultasDeAgenda,
  sesionRecepcion,
} from '../../test/datosAgenda'

describe('vista semanal (spec "Vista semanal")', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date('2026-12-02T15:00:00Z')) // miércoles 2/12
  })

  afterEach(() => {
    vi.useRealTimers()
    sessionStorage.clear()
    vi.unstubAllGlobals()
  })

  it('muestra de lunes a domingo con los turnos de cada día y una sola consulta', async () => {
    const api = simularApi(
      apiDeAgenda(sesionRecepcion.usuario, { status: 200, cuerpo: [...TURNOS_DEL_1_12, TURNO_DEL_3_12] }),
    )
    renderizarEn('/panel/agenda?vista=semana&fecha=2026-12-02', sesionRecepcion)

    expect(
      await screen.findByRole('heading', { level: 2, name: 'Semana del lunes 30/11 al domingo 6/12' }),
    ).toBeInTheDocument()
    const dias = await screen.findAllByRole('region')
    expect(dias.map((dia) => within(dia).getByRole('heading', { level: 3 }).textContent)).toEqual([
      'lunes 30/11',
      'martes 1/12',
      'miércoles 2/12 Hoy',
      'jueves 3/12',
      'viernes 4/12',
      'sábado 5/12',
      'domingo 6/12',
    ])

    // El turno de las 23:30 del 1/12 (02:30Z del 2/12) queda en el martes.
    expect(within(dias[1]).getAllByRole('listitem')).toHaveLength(TURNOS_DEL_1_12.length)
    expect(within(dias[2]).getByText('Sin turnos')).toBeInTheDocument()
    expect(within(dias[3]).getByText('08:00–08:30')).toBeInTheDocument()
    expect(within(dias[0]).getByText('Sin turnos')).toBeInTheDocument()

    await waitFor(() => expect(consultasDeAgenda(api)).toHaveLength(1))
    expect(Object.fromEntries(consultasDeAgenda(api)[0])).toEqual({ desde: '2026-11-30', hasta: '2026-12-06' })
  })

  it('marca el día de hoy', async () => {
    simularApi(apiDeAgenda(sesionRecepcion.usuario, { status: 200, cuerpo: [] }))
    renderizarEn('/panel/agenda?vista=semana', sesionRecepcion)

    const hoy = await screen.findByRole('heading', { level: 3, name: /miércoles 2\/12/ })
    expect(hoy).toHaveAttribute('aria-current', 'date')
    expect(screen.getByRole('heading', { level: 3, name: 'martes 1/12' })).not.toHaveAttribute('aria-current')
  })

  it('cambia de vista y navega por semanas', async () => {
    const usuario = userEvent.setup()
    const api = simularApi(apiDeAgenda(sesionRecepcion.usuario, { status: 200, cuerpo: [] }))
    const { router } = renderizarEn('/panel/agenda?fecha=2026-12-02', sesionRecepcion)
    await screen.findByText('No hay turnos para este día')

    await usuario.click(screen.getByRole('button', { name: 'Semana' }))
    expect(screen.getByRole('button', { name: 'Semana' })).toHaveAttribute('aria-pressed', 'true')
    expect(router.state.location.search).toBe('?vista=semana&fecha=2026-12-02')

    await usuario.click(screen.getByRole('button', { name: 'Semana siguiente' }))
    expect(
      await screen.findByRole('heading', { level: 2, name: 'Semana del lunes 7/12 al domingo 13/12' }),
    ).toBeInTheDocument()
    await waitFor(() =>
      expect(Object.fromEntries(consultasDeAgenda(api).at(-1) ?? [])).toEqual({
        desde: '2026-12-07',
        hasta: '2026-12-13',
      }),
    )

    await usuario.click(screen.getByRole('button', { name: 'Semana anterior' }))
    await usuario.click(screen.getByRole('button', { name: 'Semana anterior' }))
    expect(
      await screen.findByRole('heading', { level: 2, name: 'Semana del lunes 23/11 al domingo 29/11' }),
    ).toBeInTheDocument()

    await usuario.click(screen.getByRole('button', { name: 'Hoy' }))
    expect(
      await screen.findByRole('heading', { level: 2, name: 'Semana del lunes 30/11 al domingo 6/12' }),
    ).toBeInTheDocument()
  })

  it('anuncia la cantidad de turnos de la semana', async () => {
    simularApi(apiDeAgenda(sesionRecepcion.usuario, { status: 200, cuerpo: [TURNO_DEL_3_12] }))
    renderizarEn('/panel/agenda?vista=semana&fecha=2026-12-02', sesionRecepcion)

    expect(await screen.findByText('1 turno en la semana del lunes 30/11 al domingo 6/12')).toBeInTheDocument()
  })
})
