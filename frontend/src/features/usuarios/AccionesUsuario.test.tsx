import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { pedidos, renderizarEn, sesionDe, simularApi } from '../../test/apiSimulada'
import { ADMIN, LAURA, RECEPCION, apiDeUsuarios } from '../../test/datosUsuarios'

const sesionAdmin = { ...sesionDe('administrador'), usuario: ADMIN }

describe('Acciones sobre un usuario (spec "Modificación y baja desde la pantalla")', () => {
  afterEach(() => {
    sessionStorage.clear()
    vi.unstubAllGlobals()
  })

  it('cambiar rol ofrece los profesionales disponibles más el vinculado actual y actualiza la fila', async () => {
    const lauraActiva = { ...LAURA, activo: true }
    let cambiado = false
    simularApi({
      ...apiDeUsuarios(),
      'GET /api/usuarios': () => ({
        status: 200,
        cuerpo: [ADMIN, cambiado ? { ...RECEPCION, rol: 'profesional', profesionalId: 11, profesionalNombre: 'Martín Prueba' } : RECEPCION, lauraActiva],
      }),
      'PUT /api/usuarios/2/rol': (cuerpo) => {
        expect(cuerpo).toEqual({ rol: 'profesional', profesionalId: 11 })
        cambiado = true
        return { status: 200, cuerpo: {} }
      },
    })
    renderizarEn('/panel/usuarios', sesionAdmin)

    await userEvent.click(await screen.findByRole('button', { name: 'Cambiar rol de laura@example.com' }))
    let dialogo = await screen.findByRole('dialog', { name: 'Cambiar rol de laura@example.com' })
    const opcionesDeLaura = within(within(dialogo).getByLabelText('Profesional')).getAllByRole('option')
    expect(opcionesDeLaura.map((o) => o.textContent)).toEqual(expect.arrayContaining(['Laura Ejemplo', 'Martín Prueba']))
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Cancelar' }))

    await userEvent.click(screen.getByRole('button', { name: 'Cambiar rol de recepcion@example.com' }))
    dialogo = await screen.findByRole('dialog', { name: 'Cambiar rol de recepcion@example.com' })
    await userEvent.selectOptions(within(dialogo).getByLabelText('Rol'), 'profesional')
    await userEvent.selectOptions(within(dialogo).getByLabelText('Profesional'), '11')
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Guardar' }))

    expect(await screen.findByText('Martín Prueba')).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('Rol actualizado')
  })

  it('desactivar pide confirmación nombrando el email y al confirmar ofrece reactivar', async () => {
    let desactivado = false
    simularApi({
      ...apiDeUsuarios(),
      'GET /api/usuarios': () => ({ status: 200, cuerpo: [ADMIN, { ...RECEPCION, activo: !desactivado }] }),
      'POST /api/usuarios/2/desactivar': () => {
        desactivado = true
        return { status: 200, cuerpo: {} }
      },
    })
    renderizarEn('/panel/usuarios', sesionAdmin)

    await userEvent.click(await screen.findByRole('button', { name: 'Desactivar a recepcion@example.com' }))
    const dialogo = await screen.findByRole('dialog', { name: '¿Desactivar a recepcion@example.com?' })
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Desactivar' }))

    expect(await screen.findByRole('button', { name: 'Reactivar a recepcion@example.com' })).toBeInTheDocument()
    expect(screen.getByText('Inactivo')).toBeInTheDocument()
  })

  it('cancelar la desactivación no llama a la API', async () => {
    const api = simularApi(apiDeUsuarios())
    renderizarEn('/panel/usuarios', sesionAdmin)

    await userEvent.click(await screen.findByRole('button', { name: 'Desactivar a recepcion@example.com' }))
    const dialogo = await screen.findByRole('dialog', { name: '¿Desactivar a recepcion@example.com?' })
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Cancelar' }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(pedidos(api)).not.toContain('POST /api/usuarios/2/desactivar')
  })

  it('reactivar llama a la API sin pedir confirmación', async () => {
    const api = simularApi({ ...apiDeUsuarios(), 'POST /api/usuarios/3/activar': { status: 200, cuerpo: {} } })
    renderizarEn('/panel/usuarios', sesionAdmin)

    await userEvent.click(await screen.findByRole('button', { name: 'Reactivar a laura@example.com' }))

    await vi.waitFor(() => expect(pedidos(api)).toContain('POST /api/usuarios/3/activar'))
  })

  it('blanquear valida largo y confirmación, y al terminar lo anuncia sin mostrar la contraseña', async () => {
    const api = simularApi({ ...apiDeUsuarios(), 'PUT /api/usuarios/2/contrasena': { status: 204 } })
    renderizarEn('/panel/usuarios', sesionAdmin)

    await userEvent.click(await screen.findByRole('button', { name: 'Blanquear contraseña de recepcion@example.com' }))
    const dialogo = await screen.findByRole('dialog', { name: 'Blanquear contraseña de recepcion@example.com' })
    await userEvent.type(within(dialogo).getByLabelText('Contraseña nueva'), 'corta')
    await userEvent.type(within(dialogo).getByLabelText('Repetí la contraseña'), 'otra')
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Cambiar contraseña' }))

    expect(await within(dialogo).findByText('La contraseña debe tener al menos 8 caracteres')).toBeInTheDocument()
    expect(within(dialogo).getByText('Las contraseñas no coinciden')).toBeInTheDocument()
    expect(pedidos(api)).not.toContain('PUT /api/usuarios/2/contrasena')

    await userEvent.clear(within(dialogo).getByLabelText('Contraseña nueva'))
    await userEvent.type(within(dialogo).getByLabelText('Contraseña nueva'), 'contrasena-nueva')
    await userEvent.clear(within(dialogo).getByLabelText('Repetí la contraseña'))
    await userEvent.type(within(dialogo).getByLabelText('Repetí la contraseña'), 'contrasena-nueva')
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Cambiar contraseña' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Contraseña cambiada')
    expect(screen.queryByText('contrasena-nueva')).not.toBeInTheDocument()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })
})
