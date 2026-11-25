import { render, screen } from '@testing-library/react'
import { createMemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { Proveedores } from '../Proveedores'
import { renderizarEn, sesionDe, simularApi } from '../test/apiSimulada'
import { rutas } from './rutas'

describe('ruta /', () => {
  afterEach(() => {
    sessionStorage.clear()
    vi.unstubAllGlobals()
  })

  it('muestra el nombre DentaFlow dentro del QueryClientProvider y el router', async () => {
    const router = createMemoryRouter(rutas, { initialEntries: ['/'] })

    render(<Proveedores router={router} />)

    expect(await screen.findByRole('heading', { level: 1, name: 'DentaFlow' })).toBeInTheDocument()
  })

  it('sin sesión ofrece el ingreso del personal (spec "Acceso al ingreso desde la portada")', async () => {
    simularApi({})
    renderizarEn('/')

    expect(await screen.findByRole('link', { name: 'Ingreso del personal' })).toHaveAttribute('href', '/ingresar')
  })

  it('con sesión ofrece ir al panel', async () => {
    simularApi({ 'GET /api/auth/yo': { status: 200, cuerpo: sesionDe('recepcion').usuario } })
    renderizarEn('/', sesionDe('recepcion'))

    expect(await screen.findByRole('link', { name: 'Ir al panel' })).toHaveAttribute('href', '/panel')
    expect(screen.queryByRole('link', { name: 'Ingreso del personal' })).not.toBeInTheDocument()
  })
})
