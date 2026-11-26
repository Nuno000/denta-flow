import { screen, within } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { renderizarEn, sesionDe, simularApi } from '../../test/apiSimulada'
import { apiDeAgenda } from '../../test/datosAgenda'
import { apiDeRecursos } from '../../test/datosRecursos'
import { ADMIN, apiDeUsuarios } from '../../test/datosUsuarios'

describe('BarraDelPanel (spec "Menú del panel según el rol")', () => {
  afterEach(() => {
    sessionStorage.clear()
    vi.unstubAllGlobals()
  })

  it('un administrador en usuarios ve Panel, Agenda, Recursos y Usuarios, con Usuarios como página actual', async () => {
    simularApi(apiDeUsuarios())
    renderizarEn('/panel/usuarios', { ...sesionDe('administrador'), usuario: ADMIN })

    const menu = await screen.findByRole('navigation', { name: 'Secciones del panel' })
    expect(within(menu).getAllByRole('link').map((enlace) => enlace.textContent)).toEqual([
      'Panel',
      'Agenda',
      'Lista de espera',
      'Recursos',
      'Usuarios',
    ])
    const panel = within(menu).getByRole('link', { name: 'Panel' })
    const usuarios = within(menu).getByRole('link', { name: 'Usuarios' })
    expect(panel).toHaveAttribute('href', '/panel')
    expect(panel).not.toHaveAttribute('aria-current')
    expect(usuarios).toHaveAttribute('href', '/panel/usuarios')
    expect(usuarios).toHaveAttribute('aria-current', 'page')
  })

  it('recepción en el panel ve solo Panel, Agenda y Recursos, con Panel como página actual', async () => {
    simularApi({ 'GET /api/auth/yo': { status: 200, cuerpo: sesionDe('recepcion').usuario } })
    renderizarEn('/panel', sesionDe('recepcion'))

    const menu = await screen.findByRole('navigation', { name: 'Secciones del panel' })
    expect(within(menu).getAllByRole('link').map((enlace) => enlace.textContent)).toEqual([
      'Panel',
      'Agenda',
      'Lista de espera',
      'Recursos',
    ])
    expect(within(menu).getByRole('link', { name: 'Panel' })).toHaveAttribute('aria-current', 'page')
    expect(within(menu).getByRole('link', { name: 'Recursos' })).not.toHaveAttribute('aria-current')
  })

  it('un profesional en la pestaña Sillones tiene Recursos como página actual (C-05)', async () => {
    simularApi(apiDeRecursos(sesionDe('profesional').usuario))
    renderizarEn('/panel/recursos/sillones', sesionDe('profesional'))

    const menu = await screen.findByRole('navigation', { name: 'Secciones del panel' })
    expect(within(menu).getAllByRole('link').map((enlace) => enlace.textContent)).toEqual([
      'Panel',
      'Agenda',
      'Recursos',
    ])
    expect(within(menu).getByRole('link', { name: 'Recursos' })).toHaveAttribute('aria-current', 'page')
    expect(within(menu).getByRole('link', { name: 'Recursos' })).toHaveAttribute('href', '/panel/recursos')
  })

  it('un profesional en la agenda con filtros en la URL tiene Agenda como página actual (C-11)', async () => {
    simularApi(apiDeAgenda(sesionDe('profesional').usuario, { status: 200, cuerpo: [] }))
    renderizarEn('/panel/agenda?vista=semana&fecha=2026-12-02&sillonId=1', sesionDe('profesional'))

    const menu = await screen.findByRole('navigation', { name: 'Secciones del panel' })
    expect(within(menu).getAllByRole('link').map((enlace) => enlace.textContent)).toEqual([
      'Panel',
      'Agenda',
      'Recursos',
    ])
    const agenda = within(menu).getByRole('link', { name: 'Agenda' })
    expect(agenda).toHaveAttribute('href', '/panel/agenda')
    expect(agenda).toHaveAttribute('aria-current', 'page')
    expect(within(menu).getByRole('link', { name: 'Panel' })).not.toHaveAttribute('aria-current')
  })

  it('en el panel no queda un segundo acceso a Usuarios fuera del menú', async () => {
    simularApi({ 'GET /api/auth/yo': { status: 200, cuerpo: ADMIN } })
    renderizarEn('/panel', { ...sesionDe('administrador'), usuario: ADMIN })

    await screen.findByRole('navigation', { name: 'Secciones del panel' })
    expect(screen.getAllByRole('link', { name: 'Usuarios' })).toHaveLength(1)
  })
})
