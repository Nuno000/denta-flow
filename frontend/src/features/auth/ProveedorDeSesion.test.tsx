import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { pedidos, renderizarEn, sesionDe, simularApi } from '../../test/apiSimulada'
import { leerSesion } from './sesion'

describe('ProveedorDeSesion (spec "Cierre y vencimiento de la sesión en el frontend")', () => {
  afterEach(() => {
    sessionStorage.clear()
    vi.unstubAllGlobals()
  })

  it('al iniciar con un token guardado lo valida con /api/auth/yo y muestra el panel', async () => {
    const api = simularApi({ 'GET /api/auth/yo': { status: 200, cuerpo: sesionDe('recepcion').usuario } })

    renderizarEn('/panel', sesionDe('recepcion'))

    expect(await screen.findByText('recepcion@example.com')).toBeInTheDocument()
    expect(pedidos(api)).toContain('GET /api/auth/yo')
  })

  it('si /api/auth/yo responde 401 descarta la sesión y muestra la pantalla de ingreso', async () => {
    simularApi({ 'GET /api/auth/yo': { status: 401, cuerpo: { detail: 'Tenés que ingresar' } } })

    renderizarEn('/panel', sesionDe('recepcion'))

    expect(await screen.findByRole('heading', { name: 'Ingresar' })).toBeInTheDocument()
    expect(leerSesion()).toBeNull()
  })

  it('cerrar sesión descarta el token y vuelve a la pantalla de ingreso', async () => {
    simularApi({ 'GET /api/auth/yo': { status: 200, cuerpo: sesionDe('recepcion').usuario } })
    const { router } = renderizarEn('/panel', sesionDe('recepcion'))

    await userEvent.click(await screen.findByRole('button', { name: 'Cerrar sesión' }))

    expect(await screen.findByRole('heading', { name: 'Ingresar' })).toBeInTheDocument()
    expect(leerSesion()).toBeNull()
    await router.navigate('/panel')
    await waitFor(() => expect(router.state.location.pathname).toBe('/ingresar'))
  })
})
