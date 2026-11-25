import { screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { pedidos, renderizarEn, sesionDe, simularApi } from '../../test/apiSimulada'

describe('RutaProtegida y PanelPage (spec "Rutas del panel protegidas por rol en el frontend")', () => {
  afterEach(() => {
    sessionStorage.clear()
    vi.unstubAllGlobals()
  })

  it('sin sesión redirige a /ingresar recordando la ruta pedida', async () => {
    simularApi({})
    const { router } = renderizarEn('/panel')

    expect(await screen.findByRole('heading', { name: 'Ingresar' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/ingresar')
    expect(router.state.location.state).toEqual({ desde: '/panel' })
  })

  it('con un rol no permitido muestra el aviso sin el contenido ni datos de usuarios', async () => {
    const api = simularApi({ 'GET /api/auth/yo': { status: 200, cuerpo: sesionDe('recepcion').usuario } })
    renderizarEn('/panel/usuarios', sesionDe('recepcion'))

    expect(await screen.findByRole('heading', { name: 'No tenés permiso para esta sección' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Usuarios' })).not.toBeInTheDocument()
    await waitFor(() => expect(pedidos(api)).toContain('GET /api/auth/yo'))
    expect(pedidos(api)).not.toContain('GET /api/usuarios')
  })

  it('con el rol permitido muestra el contenido', async () => {
    simularApi({ 'GET /api/auth/yo': { status: 200, cuerpo: sesionDe('recepcion').usuario } })
    renderizarEn('/panel', sesionDe('recepcion'))

    expect(await screen.findByRole('heading', { name: 'Panel' })).toBeInTheDocument()
    expect(screen.getByText('Recepción')).toBeInTheDocument()
  })

  it('el enlace a Usuarios aparece solo para administradores', async () => {
    simularApi({ 'GET /api/auth/yo': { status: 200, cuerpo: sesionDe('recepcion').usuario } })
    renderizarEn('/panel', sesionDe('recepcion'))
    await screen.findByRole('heading', { name: 'Panel' })
    expect(screen.queryByRole('link', { name: 'Usuarios' })).not.toBeInTheDocument()
  })

  it('un administrador ve el enlace a Usuarios', async () => {
    simularApi({ 'GET /api/auth/yo': { status: 200, cuerpo: sesionDe('administrador').usuario } })
    renderizarEn('/panel', sesionDe('administrador'))

    expect(await screen.findByRole('link', { name: 'Usuarios' })).toHaveAttribute('href', '/panel/usuarios')
  })
})
