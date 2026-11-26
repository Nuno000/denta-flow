import { render, screen, within } from '@testing-library/react'
import { createMemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { Proveedores } from '../Proveedores'
import { renderizarEn, sesionDe, simularApi } from '../test/apiSimulada'
import { apiDeRecursos, sesionAdmin } from '../test/datosRecursos'
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

describe('rutas de Recursos (C-05, spec "Sección Recursos del panel")', () => {
  afterEach(() => {
    sessionStorage.clear()
    vi.unstubAllGlobals()
  })

  it('/panel/recursos lleva a la pestaña Profesionales', async () => {
    simularApi(apiDeRecursos())
    const { router } = renderizarEn('/panel/recursos', sesionAdmin)

    expect(await screen.findByRole('heading', { level: 2, name: 'Profesionales' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 1, name: 'Recursos' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/panel/recursos/profesionales')
    const pestanas = screen.getByRole('navigation', { name: 'Tipos de recurso' })
    expect(within(pestanas).getByRole('link', { name: 'Profesionales' })).toHaveAttribute('aria-current', 'page')
    expect(within(pestanas).getByRole('link', { name: 'Sillones' })).not.toHaveAttribute('aria-current')
  })

  it.each(['recepcion', 'profesional'] as const)('%s también entra a Recursos', async (rol) => {
    simularApi(apiDeRecursos(sesionDe(rol).usuario))
    renderizarEn('/panel/recursos/prestaciones', sesionDe(rol))

    expect(await screen.findByRole('heading', { level: 2, name: 'Prestaciones' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'No tenés permiso para esta sección' })).not.toBeInTheDocument()
  })

  it('sin sesión lleva al ingreso', async () => {
    simularApi({})
    const { router } = renderizarEn('/panel/recursos/sillones')

    await screen.findByRole('heading', { level: 1 })
    expect(router.state.location.pathname).toBe('/ingresar')
  })
})

