import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { pedidos, renderizarEn, sesionDe, simularApi } from '../../test/apiSimulada'
import { leerSesion } from './sesion'

const respuestaLogin = {
  token: 'token-nuevo',
  venceEn: '2026-11-25T21:00:00Z',
  usuario: sesionDe('recepcion').usuario,
}

describe('IngresarPage (spec "Pantalla de ingreso")', () => {
  afterEach(() => {
    sessionStorage.clear()
    vi.unstubAllGlobals()
  })

  it('los campos tienen etiqueta', async () => {
    simularApi({})
    renderizarEn('/ingresar')

    expect(await screen.findByLabelText('Email')).toBeInTheDocument()
    expect(screen.getByLabelText('Contraseña')).toHaveAttribute('type', 'password')
  })

  it('un email mal formado muestra el error junto al campo, lo enfoca y no llama a la API', async () => {
    const api = simularApi({})
    renderizarEn('/ingresar')

    await userEvent.type(await screen.findByLabelText('Email'), 'recepcion-sin-arroba')
    await userEvent.type(screen.getByLabelText('Contraseña'), 'algo')
    await userEvent.click(screen.getByRole('button', { name: 'Ingresar' }))

    const email = screen.getByLabelText('Email')
    expect(await screen.findByText('El email no tiene un formato válido')).toBeInTheDocument()
    expect(email).toHaveAttribute('aria-invalid', 'true')
    expect(email).toHaveFocus()
    expect(pedidos(api)).toEqual([])
  })

  it('un email vacío pide completarlo', async () => {
    simularApi({})
    renderizarEn('/ingresar')

    await userEvent.click(await screen.findByRole('button', { name: 'Ingresar' }))

    expect(await screen.findByText('Ingresá tu email')).toBeInTheDocument()
    expect(screen.getByLabelText('Email')).toHaveFocus()
  })

  it('un 401 muestra el mensaje genérico como alerta y conserva el email', async () => {
    simularApi({ 'POST /api/auth/login': { status: 401, cuerpo: { detail: 'Email o contraseña incorrectos' } } })
    renderizarEn('/ingresar')

    await userEvent.type(await screen.findByLabelText('Email'), 'recepcion@example.com')
    await userEvent.type(screen.getByLabelText('Contraseña'), 'incorrecta')
    await userEvent.click(screen.getByRole('button', { name: 'Ingresar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Email o contraseña incorrectos')
    expect(screen.getByLabelText('Email')).toHaveValue('recepcion@example.com')
  })

  it('un 429 avisa que hubo demasiados intentos', async () => {
    simularApi({ 'POST /api/auth/login': { status: 429, cuerpo: { detail: 'Demasiados intentos' } } })
    renderizarEn('/ingresar')

    await userEvent.type(await screen.findByLabelText('Email'), 'recepcion@example.com')
    await userEvent.type(screen.getByLabelText('Contraseña'), 'incorrecta')
    await userEvent.click(screen.getByRole('button', { name: 'Ingresar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(/demasiados intentos/i)
  })

  it('un ingreso correcto guarda la sesión y lleva al panel', async () => {
    simularApi({
      'POST /api/auth/login': { status: 200, cuerpo: respuestaLogin },
      'GET /api/auth/yo': { status: 200, cuerpo: respuestaLogin.usuario },
    })
    const { router } = renderizarEn('/ingresar')

    await userEvent.type(await screen.findByLabelText('Email'), 'recepcion@example.com')
    await userEvent.type(screen.getByLabelText('Contraseña'), 'correcta')
    await userEvent.click(screen.getByRole('button', { name: 'Ingresar' }))

    await waitFor(() => expect(router.state.location.pathname).toBe('/panel'))
    expect(await screen.findByText('recepcion@example.com')).toBeInTheDocument()
    expect(leerSesion()?.token).toBe('token-nuevo')
  })

  it('después de ingresar vuelve a la ruta que se había pedido', async () => {
    simularApi({
      'POST /api/auth/login': { status: 200, cuerpo: { ...respuestaLogin, usuario: sesionDe('administrador').usuario } },
      'GET /api/auth/yo': { status: 200, cuerpo: sesionDe('administrador').usuario },
      'GET /api/usuarios': { status: 200, cuerpo: [] },
      'GET /api/roles': { status: 200, cuerpo: [] },
      'GET /api/usuarios/profesionales-disponibles': { status: 200, cuerpo: [] },
    })
    const { router } = renderizarEn('/panel/usuarios')

    await userEvent.type(await screen.findByLabelText('Email'), 'administrador@example.com')
    await userEvent.type(screen.getByLabelText('Contraseña'), 'correcta')
    await userEvent.click(screen.getByRole('button', { name: 'Ingresar' }))

    await waitFor(() => expect(router.state.location.pathname).toBe('/panel/usuarios'))
  })

  it('el botón se deshabilita mientras envía', async () => {
    let responder: (valor: Response) => void = () => undefined
    vi.stubGlobal('fetch', vi.fn(() => new Promise<Response>((resolver) => (responder = resolver))))
    renderizarEn('/ingresar')

    await userEvent.type(await screen.findByLabelText('Email'), 'recepcion@example.com')
    await userEvent.type(screen.getByLabelText('Contraseña'), 'correcta')
    await userEvent.click(screen.getByRole('button', { name: 'Ingresar' }))

    expect(await screen.findByRole('button', { name: /Ingresando/ })).toBeDisabled()
    responder(new Response(JSON.stringify({ detail: 'x' }), { status: 401, headers: { 'Content-Type': 'application/json' } }))
  })
})
