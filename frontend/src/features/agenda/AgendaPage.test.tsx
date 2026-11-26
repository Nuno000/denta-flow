import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { renderizarEn, simularApi } from '../../test/apiSimulada'
import {
  TURNOS_DEL_1_12,
  apiDeAgenda,
  consultasDeAgenda,
  sesionProfesional,
  sesionRecepcion,
} from '../../test/datosAgenda'
import { sesionAdmin } from '../../test/datosRecursos'

/** Jueves 26/11/2026, 12:00 en el consultorio. */
const AHORA = new Date('2026-11-26T15:00:00Z')

describe('pantalla de agenda (spec agenda-interna)', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(AHORA)
  })

  afterEach(() => {
    vi.useRealTimers()
    sessionStorage.clear()
    vi.unstubAllGlobals()
  })

  it('abre en la vista diaria de hoy del consultorio', async () => {
    const api = simularApi(apiDeAgenda(sesionRecepcion.usuario, { status: 200, cuerpo: [] }))
    renderizarEn('/panel/agenda', sesionRecepcion)

    expect(await screen.findByRole('heading', { level: 1, name: 'Agenda' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 2, name: 'jueves 26 de noviembre de 2026' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Día' })).toHaveAttribute('aria-pressed', 'true')
    await waitFor(() => expect(consultasDeAgenda(api)).toHaveLength(1))
    expect(Object.fromEntries(consultasDeAgenda(api)[0])).toEqual({ desde: '2026-11-26', hasta: '2026-11-26' })
  })

  it('usa la fecha del consultorio aunque en UTC ya sea el día siguiente', async () => {
    vi.setSystemTime(new Date('2026-11-27T02:00:00Z'))
    simularApi(apiDeAgenda(sesionRecepcion.usuario, { status: 200, cuerpo: [] }))
    renderizarEn('/panel/agenda', sesionRecepcion)

    expect(
      await screen.findByRole('heading', { level: 2, name: 'jueves 26 de noviembre de 2026' }),
    ).toBeInTheDocument()
  })

  it('pasa al día siguiente, cambia la URL y pide ese día', async () => {
    const usuario = userEvent.setup()
    const api = simularApi(apiDeAgenda(sesionRecepcion.usuario, { status: 200, cuerpo: [] }))
    const { router } = renderizarEn('/panel/agenda', sesionRecepcion)
    await screen.findByText('No hay turnos para este día')

    await usuario.click(screen.getByRole('button', { name: 'Día siguiente' }))

    expect(
      await screen.findByRole('heading', { level: 2, name: 'viernes 27 de noviembre de 2026' }),
    ).toBeInTheDocument()
    expect(router.state.location.search).toBe('?fecha=2026-11-27')
    await waitFor(() => expect(consultasDeAgenda(api).at(-1)?.get('desde')).toBe('2026-11-27'))
  })

  it('día anterior y volver a hoy', async () => {
    const usuario = userEvent.setup()
    simularApi(apiDeAgenda(sesionRecepcion.usuario, { status: 200, cuerpo: [] }))
    renderizarEn('/panel/agenda?fecha=2026-12-01', sesionRecepcion)
    await screen.findByRole('heading', { level: 2, name: 'martes 1 de diciembre de 2026' })

    await usuario.click(screen.getByRole('button', { name: 'Día anterior' }))
    expect(await screen.findByRole('heading', { level: 2, name: 'lunes 30 de noviembre de 2026' })).toBeInTheDocument()

    await usuario.click(screen.getByRole('button', { name: 'Hoy' }))
    expect(
      await screen.findByRole('heading', { level: 2, name: 'jueves 26 de noviembre de 2026' }),
    ).toBeInTheDocument()
  })

  it('anuncia la fecha y la cantidad de turnos', async () => {
    simularApi(apiDeAgenda(sesionRecepcion.usuario))
    renderizarEn('/panel/agenda?fecha=2026-12-01', sesionRecepcion)

    const anuncio = await screen.findByText(`${TURNOS_DEL_1_12.length} turnos el martes 1 de diciembre de 2026`)
    expect(anuncio).toHaveAttribute('aria-live', 'polite')
  })

  it('una fecha inválida en la URL abre hoy', async () => {
    simularApi(apiDeAgenda(sesionRecepcion.usuario, { status: 200, cuerpo: [] }))
    renderizarEn('/panel/agenda?fecha=2026-02-30&vista=mes', sesionRecepcion)

    expect(
      await screen.findByRole('heading', { level: 2, name: 'jueves 26 de noviembre de 2026' }),
    ).toBeInTheDocument()
  })

  describe('rol profesional (US-023)', () => {
    it('ve "Mi agenda", sin filtro de profesional, y no envía profesionalId', async () => {
      const api = simularApi(apiDeAgenda(sesionProfesional.usuario))
      renderizarEn('/panel/agenda?fecha=2026-12-01&profesionalId=11', sesionProfesional)

      expect(await screen.findByRole('heading', { level: 1, name: 'Mi agenda' })).toBeInTheDocument()
      expect(screen.queryByRole('combobox', { name: 'Profesional' })).not.toBeInTheDocument()
      expect(screen.getByRole('combobox', { name: 'Sillón' })).toBeInTheDocument()
      expect(screen.getByRole('combobox', { name: 'Estado' })).toBeInTheDocument()
      await waitFor(() => expect(consultasDeAgenda(api)).toHaveLength(1))
      expect(consultasDeAgenda(api)[0].has('profesionalId')).toBe(false)
    })
  })

  describe('carga y errores', () => {
    it('indica que está cargando', async () => {
      simularApi(apiDeAgenda(sesionRecepcion.usuario))
      renderizarEn('/panel/agenda', sesionRecepcion)

      expect(await screen.findByText('Cargando la agenda…')).toBeInTheDocument()
    })

    it('si la API falla muestra el aviso y permite reintentar', async () => {
      const usuario = userEvent.setup()
      let fallar = true
      const api = simularApi(
        apiDeAgenda(sesionRecepcion.usuario, () => (fallar ? { status: 500 } : { status: 200, cuerpo: TURNOS_DEL_1_12 })),
      )
      renderizarEn('/panel/agenda?fecha=2026-12-01', sesionRecepcion)

      const alerta = await screen.findByRole('alert', {}, { timeout: 4000 })
      expect(alerta).toHaveTextContent('No se pudo cargar la agenda')
      const pedidosAntes = consultasDeAgenda(api).length

      fallar = false
      await usuario.click(within(alerta).getByRole('button', { name: 'Reintentar' }))

      expect(await screen.findByText('Ana Ejemplar')).toBeInTheDocument()
      expect(consultasDeAgenda(api).length).toBeGreaterThan(pedidosAntes)
    })

    it('un 403 se muestra como falta de permiso', async () => {
      simularApi(apiDeAgenda(sesionAdmin.usuario, { status: 403, cuerpo: { detail: 'Acceso denegado' } }))
      renderizarEn('/panel/agenda', sesionAdmin)

      expect(await screen.findByRole('alert')).toHaveTextContent('No tenés permiso para ver esta agenda')
      expect(screen.queryByRole('button', { name: 'Reintentar' })).not.toBeInTheDocument()
    })
  })
})
