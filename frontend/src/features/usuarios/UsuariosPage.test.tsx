import { screen, within } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { pedidos, renderizarEn, sesionDe, simularApi } from '../../test/apiSimulada'
import { ADMIN, apiDeUsuarios } from '../../test/datosUsuarios'

const sesionAdmin = { ...sesionDe('administrador'), usuario: ADMIN }

describe('UsuariosPage (spec "Pantalla de gestión de usuarios")', () => {
  afterEach(() => {
    sessionStorage.clear()
    vi.unstubAllGlobals()
  })

  it('lista todos los usuarios con email, rol visible, profesional y estado en texto', async () => {
    simularApi(apiDeUsuarios())
    renderizarEn('/panel/usuarios', sesionAdmin)

    const tabla = await screen.findByRole('table', { name: 'Usuarios del consultorio' })
    const filas = within(tabla).getAllByRole('row').slice(1)
    expect(filas).toHaveLength(3)
    expect(within(filas[1]).getByText('recepcion@example.com')).toBeInTheDocument()
    expect(within(filas[1]).getByText('Recepción')).toBeInTheDocument()
    expect(within(filas[1]).getByText('Activo')).toBeInTheDocument()
    expect(within(filas[2]).getByText('Laura Ejemplo')).toBeInTheDocument()
    expect(within(filas[2]).getByText('Inactivo')).toBeInTheDocument()
  })

  it('la fila propia lleva "(vos)" y no ofrece cambiar rol, desactivar ni blanquear', async () => {
    simularApi(apiDeUsuarios())
    renderizarEn('/panel/usuarios', sesionAdmin)

    const tabla = await screen.findByRole('table', { name: 'Usuarios del consultorio' })
    const propia = within(tabla).getAllByRole('row')[1]
    expect(within(propia).getByText('(vos)')).toBeInTheDocument()
    expect(within(propia).queryAllByRole('button')).toHaveLength(0)

    const otra = within(tabla).getAllByRole('row')[2]
    expect(within(otra).getByRole('button', { name: 'Cambiar rol de recepcion@example.com' })).toBeInTheDocument()
    expect(within(otra).getByRole('button', { name: 'Desactivar a recepcion@example.com' })).toBeInTheDocument()
    expect(within(otra).getByRole('button', { name: 'Blanquear contraseña de recepcion@example.com' })).toBeInTheDocument()
  })

  it('un usuario inactivo ofrece reactivarlo', async () => {
    simularApi(apiDeUsuarios())
    renderizarEn('/panel/usuarios', sesionAdmin)

    expect(await screen.findByRole('button', { name: 'Reactivar a laura@example.com' })).toBeInTheDocument()
  })

  it('recepción que entra a la dirección ve el aviso de permiso y no pide los usuarios', async () => {
    const api = simularApi({ 'GET /api/auth/yo': { status: 200, cuerpo: sesionDe('recepcion').usuario } })
    renderizarEn('/panel/usuarios', sesionDe('recepcion'))

    expect(await screen.findByRole('heading', { name: 'No tenés permiso para esta sección' })).toBeInTheDocument()
    expect(pedidos(api)).not.toContain('GET /api/usuarios')
  })

  it('si el listado falla lo informa', async () => {
    simularApi({ ...apiDeUsuarios(), 'GET /api/usuarios': { status: 500, cuerpo: { detail: 'Error' } } })
    renderizarEn('/panel/usuarios', sesionAdmin)

    // Un 500 se reintenta una vez (queryClient) con 1 s de espera antes de mostrar el error.
    expect(await screen.findByRole('alert', {}, { timeout: 4000 })).toHaveTextContent('No pudimos cargar los usuarios')
  })
})
