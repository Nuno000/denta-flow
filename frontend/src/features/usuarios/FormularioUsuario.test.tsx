import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { pedidos, renderizarEn, sesionDe, simularApi } from '../../test/apiSimulada'
import { ADMIN, RECEPCION, apiDeUsuarios } from '../../test/datosUsuarios'

const sesionAdmin = { ...sesionDe('administrador'), usuario: ADMIN }

async function abrirAlta() {
  await userEvent.click(await screen.findByRole('button', { name: 'Nuevo usuario' }))
  return screen.findByRole('dialog', { name: 'Nuevo usuario' })
}

describe('FormularioUsuario (spec "Alta de usuario desde la pantalla")', () => {
  afterEach(() => {
    sessionStorage.clear()
    vi.unstubAllGlobals()
  })

  it('el campo profesional aparece solo con el rol profesional y es obligatorio', async () => {
    const api = simularApi(apiDeUsuarios())
    renderizarEn('/panel/usuarios', sesionAdmin)
    const dialogo = await abrirAlta()

    expect(within(dialogo).queryByLabelText('Profesional')).not.toBeInTheDocument()
    await userEvent.selectOptions(within(dialogo).getByLabelText('Rol'), 'profesional')
    const profesional = within(dialogo).getByLabelText('Profesional')
    expect(within(profesional).getByRole('option', { name: 'Martín Prueba' })).toBeInTheDocument()

    await userEvent.type(within(dialogo).getByLabelText('Email'), 'martin@example.com')
    await userEvent.type(within(dialogo).getByLabelText('Contraseña'), 'contrasena-valida')
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Crear usuario' }))

    expect(await within(dialogo).findByText('Elegí el profesional')).toBeInTheDocument()
    expect(pedidos(api)).not.toContain('POST /api/usuarios')
  })

  it('valida email y largo de contraseña junto a cada campo sin llamar a la API', async () => {
    const api = simularApi(apiDeUsuarios())
    renderizarEn('/panel/usuarios', sesionAdmin)
    const dialogo = await abrirAlta()

    await userEvent.type(within(dialogo).getByLabelText('Email'), 'sin-arroba')
    await userEvent.type(within(dialogo).getByLabelText('Contraseña'), '1234567')
    await userEvent.selectOptions(within(dialogo).getByLabelText('Rol'), 'recepcion')
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Crear usuario' }))

    expect(await within(dialogo).findByText('El email no tiene un formato válido')).toBeInTheDocument()
    expect(within(dialogo).getByText('La contraseña debe tener al menos 8 caracteres')).toBeInTheDocument()
    expect(within(dialogo).getByLabelText('Email')).toHaveFocus()
    expect(pedidos(api)).not.toContain('POST /api/usuarios')
  })

  it('un 409 muestra que el email está en uso y conserva los datos', async () => {
    simularApi({
      ...apiDeUsuarios(),
      'POST /api/usuarios': { status: 409, cuerpo: { detail: 'El email ya está en uso' } },
    })
    renderizarEn('/panel/usuarios', sesionAdmin)
    const dialogo = await abrirAlta()

    await userEvent.type(within(dialogo).getByLabelText('Email'), 'recepcion@example.com')
    await userEvent.type(within(dialogo).getByLabelText('Contraseña'), 'contrasena-valida')
    await userEvent.selectOptions(within(dialogo).getByLabelText('Rol'), 'recepcion')
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Crear usuario' }))

    expect(await within(dialogo).findByText('El email ya está en uso')).toBeInTheDocument()
    expect(within(dialogo).getByLabelText('Email')).toHaveValue('recepcion@example.com')
  })

  it('un alta correcta cierra el diálogo, lo anuncia y el usuario aparece en el listado', async () => {
    const nuevo = { ...RECEPCION, id: 9, email: 'nueva@example.com' }
    let creados = false
    simularApi({
      ...apiDeUsuarios(),
      'GET /api/usuarios': () => ({ status: 200, cuerpo: creados ? [ADMIN, nuevo] : [ADMIN] }),
      'POST /api/usuarios': (cuerpo) => {
        creados = true
        expect(cuerpo).toEqual({ email: 'nueva@example.com', contrasena: 'contrasena-valida', rol: 'recepcion', profesionalId: null })
        return { status: 201, cuerpo: nuevo }
      },
    })
    renderizarEn('/panel/usuarios', sesionAdmin)
    const dialogo = await abrirAlta()

    await userEvent.type(within(dialogo).getByLabelText('Email'), 'nueva@example.com')
    await userEvent.type(within(dialogo).getByLabelText('Contraseña'), 'contrasena-valida')
    await userEvent.selectOptions(within(dialogo).getByLabelText('Rol'), 'recepcion')
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Crear usuario' }))

    expect(await screen.findByText('nueva@example.com')).toBeInTheDocument()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('Usuario creado')
  })

  it('cancelar cierra el diálogo y devuelve el foco al botón que lo abrió', async () => {
    simularApi(apiDeUsuarios())
    renderizarEn('/panel/usuarios', sesionAdmin)
    const dialogo = await abrirAlta()

    expect(within(dialogo).getByLabelText('Email')).toHaveFocus()
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Cancelar' }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Nuevo usuario' })).toHaveFocus()
  })
})
