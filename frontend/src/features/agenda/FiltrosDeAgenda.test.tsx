import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { renderizarEn, simularApi } from '../../test/apiSimulada'
import { apiDeAgenda, consultasDeAgenda, sesionRecepcion } from '../../test/datosAgenda'
import { sesionAdmin } from '../../test/datosRecursos'

const RUTA = '/panel/agenda?fecha=2026-12-01'

describe('filtros de la agenda (spec "Filtros en la pantalla")', () => {
  afterEach(() => {
    sessionStorage.clear()
    vi.unstubAllGlobals()
  })

  it('ofrece profesionales, sillones y estados en selectores rotulados', async () => {
    simularApi(apiDeAgenda(sesionRecepcion.usuario))
    renderizarEn(RUTA, sesionRecepcion)

    const profesional = await screen.findByRole('combobox', { name: 'Profesional' })
    await waitFor(() => expect(within(profesional).getAllByRole('option')).toHaveLength(3))
    expect(within(profesional).getAllByRole('option').map((o) => o.textContent)).toEqual([
      'Todos',
      'Laura Ejemplo',
      'Martín Prueba (inactivo)',
    ])
    const sillon = screen.getByRole('combobox', { name: 'Sillón' })
    await waitFor(() => expect(within(sillon).getAllByRole('option')).toHaveLength(3))
    expect(within(screen.getByRole('combobox', { name: 'Estado' })).getAllByRole('option').map((o) => o.textContent))
      .toEqual(['Todos (sin cancelados)', 'Pendiente', 'Confirmado', 'Asistió', 'Ausente', 'Ausente con aviso', 'Cancelado'])
  })

  it('filtrar por profesional actualiza la URL y la consulta', async () => {
    const usuario = userEvent.setup()
    const api = simularApi(apiDeAgenda(sesionRecepcion.usuario))
    const { router } = renderizarEn(RUTA, sesionRecepcion)
    const profesional = await screen.findByRole('combobox', { name: 'Profesional' })
    await screen.findByRole('option', { name: 'Laura Ejemplo' })

    await usuario.selectOptions(profesional, 'Laura Ejemplo')

    expect(router.state.location.search).toBe('?fecha=2026-12-01&profesionalId=10')
    await waitFor(() => expect(consultasDeAgenda(api).at(-1)?.get('profesionalId')).toBe('10'))
  })

  it('filtrar por sillón y por estado se combinan', async () => {
    const usuario = userEvent.setup()
    const api = simularApi(apiDeAgenda(sesionAdmin.usuario))
    const { router } = renderizarEn(RUTA, sesionAdmin)
    await screen.findByRole('option', { name: 'Sillón 2 (inactivo)' })

    await usuario.selectOptions(screen.getByRole('combobox', { name: 'Sillón' }), 'Sillón 2 (inactivo)')
    await usuario.selectOptions(screen.getByRole('combobox', { name: 'Estado' }), 'Cancelado')

    expect(router.state.location.search).toBe('?fecha=2026-12-01&sillonId=2&estado=cancelado')
    await waitFor(() =>
      expect(Object.fromEntries(consultasDeAgenda(api).at(-1) ?? [])).toEqual({
        desde: '2026-12-01',
        hasta: '2026-12-01',
        sillonId: '2',
        estado: 'cancelado',
      }),
    )
  })

  it('respeta los filtros de un enlace', async () => {
    const api = simularApi(apiDeAgenda(sesionRecepcion.usuario, { status: 200, cuerpo: [] }))
    renderizarEn('/panel/agenda?vista=semana&fecha=2026-12-02&sillonId=2', sesionRecepcion)

    await screen.findByRole('heading', { level: 2, name: 'Semana del lunes 30/11 al domingo 6/12' })
    await waitFor(() => expect(screen.getByRole('combobox', { name: 'Sillón' })).toHaveValue('2'))
    await waitFor(() => expect(consultasDeAgenda(api)).toHaveLength(1))
    expect(Object.fromEntries(consultasDeAgenda(api)[0])).toEqual({
      desde: '2026-11-30',
      hasta: '2026-12-06',
      sillonId: '2',
    })
  })

  it('limpiar los filtros vuelve a todos, sin cancelados, en la misma fecha y vista', async () => {
    const usuario = userEvent.setup()
    const api = simularApi(apiDeAgenda(sesionRecepcion.usuario))
    const { router } = renderizarEn(
      '/panel/agenda?vista=semana&fecha=2026-12-02&profesionalId=10&sillonId=2&estado=pendiente',
      sesionRecepcion,
    )
    await screen.findByRole('heading', { level: 2, name: /Semana del/ })

    await usuario.click(screen.getByRole('button', { name: 'Limpiar filtros' }))

    expect(router.state.location.search).toBe('?vista=semana&fecha=2026-12-02')
    await waitFor(() =>
      expect(Object.fromEntries(consultasDeAgenda(api).at(-1) ?? [])).toEqual({
        desde: '2026-11-30',
        hasta: '2026-12-06',
      }),
    )
  })
})
