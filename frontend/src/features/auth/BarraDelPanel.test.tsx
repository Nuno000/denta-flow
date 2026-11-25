import { screen, within } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { renderizarEn, sesionDe, simularApi } from '../../test/apiSimulada'
import { ADMIN, apiDeUsuarios } from '../../test/datosUsuarios'

describe('BarraDelPanel (spec "Menú del panel según el rol")', () => {
  afterEach(() => {
    sessionStorage.clear()
    vi.unstubAllGlobals()
  })

  it('un administrador en usuarios ve Panel y Usuarios, con Usuarios como página actual', async () => {
    simularApi(apiDeUsuarios())
    renderizarEn('/panel/usuarios', { ...sesionDe('administrador'), usuario: ADMIN })

    const menu = await screen.findByRole('navigation', { name: 'Secciones del panel' })
    const panel = within(menu).getByRole('link', { name: 'Panel' })
    const usuarios = within(menu).getByRole('link', { name: 'Usuarios' })
    expect(panel).toHaveAttribute('href', '/panel')
    expect(panel).not.toHaveAttribute('aria-current')
    expect(usuarios).toHaveAttribute('href', '/panel/usuarios')
    expect(usuarios).toHaveAttribute('aria-current', 'page')
  })

  it('recepción en el panel ve solo Panel, marcado como página actual', async () => {
    simularApi({ 'GET /api/auth/yo': { status: 200, cuerpo: sesionDe('recepcion').usuario } })
    renderizarEn('/panel', sesionDe('recepcion'))

    const menu = await screen.findByRole('navigation', { name: 'Secciones del panel' })
    expect(within(menu).getAllByRole('link')).toHaveLength(1)
    expect(within(menu).getByRole('link', { name: 'Panel' })).toHaveAttribute('aria-current', 'page')
  })

  it('en el panel no queda un segundo acceso a Usuarios fuera del menú', async () => {
    simularApi({ 'GET /api/auth/yo': { status: 200, cuerpo: ADMIN } })
    renderizarEn('/panel', { ...sesionDe('administrador'), usuario: ADMIN })

    await screen.findByRole('navigation', { name: 'Secciones del panel' })
    expect(screen.getAllByRole('link', { name: 'Usuarios' })).toHaveLength(1)
  })
})
