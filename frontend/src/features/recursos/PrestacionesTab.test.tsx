import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { pedidos, renderizarEn, sesionDe, simularApi } from '../../test/apiSimulada'
import { apiDeRecursos, sesionAdmin } from '../../test/datosRecursos'

const RUTA = '/panel/recursos/prestaciones'

describe('pestaña Prestaciones (spec gestion-recursos)', () => {
  afterEach(() => {
    sessionStorage.clear()
    vi.unstubAllGlobals()
  })

  it('lista cada prestación con nombre, duración en minutos y estado', async () => {
    simularApi(apiDeRecursos())
    renderizarEn(RUTA, sesionAdmin)

    const tabla = await screen.findByRole('table', { name: 'Prestaciones del consultorio' })
    const filas = within(tabla).getAllByRole('row').slice(1)
    expect(filas).toHaveLength(4)
    expect(within(filas[3]).getByText('Limpieza')).toBeInTheDocument()
    expect(within(filas[3]).getByText('40 min')).toBeInTheDocument()
    expect(within(filas[0]).getByText('Inactiva')).toBeInTheDocument()
    expect(within(filas[1]).getByText('Activa')).toBeInTheDocument()
  })

  it('profesional ve el listado sin acciones', async () => {
    simularApi(apiDeRecursos(sesionDe('profesional').usuario))
    renderizarEn(RUTA, sesionDe('profesional'))

    const tabla = await screen.findByRole('table', { name: 'Prestaciones del consultorio' })
    expect(within(tabla).queryAllByRole('button')).toHaveLength(0)
    expect(screen.queryByRole('button', { name: 'Nueva prestación' })).not.toBeInTheDocument()
  })

  it('una duración 0 muestra el error junto al campo y no llama a la API', async () => {
    const usuario = userEvent.setup()
    const api = simularApi(apiDeRecursos())
    renderizarEn(RUTA, sesionAdmin)

    await usuario.click(await screen.findByRole('button', { name: 'Nueva prestación' }))
    const dialogo = screen.getByRole('dialog', { name: 'Nueva prestación' })
    await usuario.type(within(dialogo).getByLabelText('Nombre'), 'Control')
    await usuario.type(within(dialogo).getByLabelText('Duración (minutos)'), '0')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Crear prestación' }))

    const duracion = within(dialogo).getByLabelText('Duración (minutos)')
    expect(await within(dialogo).findByText('La duración debe ser mayor que cero')).toBeInTheDocument()
    expect(duracion).toHaveAttribute('aria-invalid', 'true')
    expect(pedidos(api)).not.toContain('POST /api/prestaciones')
  })

  it('alta exitosa envía la duración como número y anuncia "Prestación creada"', async () => {
    const usuario = userEvent.setup()
    let enviado: unknown
    simularApi(
      apiDeRecursos(undefined, {
        'POST /api/prestaciones': (cuerpo) => {
          enviado = cuerpo
          return { status: 201, cuerpo: { id: 9, ...(cuerpo as object), activa: true } }
        },
      }),
    )
    renderizarEn(RUTA, sesionAdmin)

    await usuario.click(await screen.findByRole('button', { name: 'Nueva prestación' }))
    const dialogo = screen.getByRole('dialog', { name: 'Nueva prestación' })
    await usuario.type(within(dialogo).getByLabelText('Nombre'), 'Control')
    await usuario.type(within(dialogo).getByLabelText('Duración (minutos)'), '20')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Crear prestación' }))

    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('Prestación creada'))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(enviado).toEqual({ nombre: 'Control', duracionMinutos: 20 })
  })

  it('editar precarga nombre y duración', async () => {
    const usuario = userEvent.setup()
    let enviado: unknown
    simularApi(
      apiDeRecursos(undefined, {
        'PUT /api/prestaciones/4': (cuerpo) => {
          enviado = cuerpo
          return { status: 200, cuerpo: { id: 4, ...(cuerpo as object), activa: true } }
        },
      }),
    )
    renderizarEn(RUTA, sesionAdmin)

    await usuario.click(await screen.findByRole('button', { name: 'Editar Limpieza' }))
    const dialogo = screen.getByRole('dialog', { name: 'Editar Limpieza' })
    const duracion = within(dialogo).getByLabelText('Duración (minutos)')
    expect(duracion).toHaveValue(40)
    await usuario.clear(duracion)
    await usuario.type(duracion, '45')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Guardar cambios' }))

    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('Prestación actualizada'))
    expect(enviado).toEqual({ nombre: 'Limpieza', duracionMinutos: 45 })
  })

  it('un nombre repetido muestra el conflicto de la API en el formulario', async () => {
    const usuario = userEvent.setup()
    simularApi(
      apiDeRecursos(undefined, {
        'POST /api/prestaciones': { status: 409, cuerpo: { detail: 'Ya existe una prestación con ese nombre' } },
      }),
    )
    renderizarEn(RUTA, sesionAdmin)

    await usuario.click(await screen.findByRole('button', { name: 'Nueva prestación' }))
    const dialogo = screen.getByRole('dialog', { name: 'Nueva prestación' })
    await usuario.type(within(dialogo).getByLabelText('Nombre'), 'limpieza')
    await usuario.type(within(dialogo).getByLabelText('Duración (minutos)'), '30')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Crear prestación' }))

    expect(await within(dialogo).findByText('Ya existe una prestación con ese nombre')).toBeInTheDocument()
  })

  it('desactivar con confirmación y reactivar sin confirmación', async () => {
    const usuario = userEvent.setup()
    const api = simularApi(
      apiDeRecursos(undefined, {
        'POST /api/prestaciones/4/desactivar': {
          status: 200,
          cuerpo: { id: 4, nombre: 'Limpieza', duracionMinutos: 40, activa: false },
        },
        'POST /api/prestaciones/1/activar': {
          status: 200,
          cuerpo: { id: 1, nombre: 'Blanqueamiento', duracionMinutos: 90, activa: true },
        },
      }),
    )
    renderizarEn(RUTA, sesionAdmin)

    await usuario.click(await screen.findByRole('button', { name: 'Desactivar Limpieza' }))
    await usuario.click(
      within(screen.getByRole('dialog', { name: '¿Desactivar Limpieza?' })).getByRole('button', { name: 'Desactivar' }),
    )
    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('Prestación desactivada'))

    await usuario.click(screen.getByRole('button', { name: 'Reactivar Blanqueamiento' }))
    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('Prestación reactivada'))
    expect(pedidos(api)).toEqual(
      expect.arrayContaining(['POST /api/prestaciones/4/desactivar', 'POST /api/prestaciones/1/activar']),
    )
  })
})
