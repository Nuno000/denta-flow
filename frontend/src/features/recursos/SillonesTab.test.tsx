import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { pedidos, renderizarEn, sesionDe, simularApi } from '../../test/apiSimulada'
import { SILLONES, apiDeRecursos, sesionAdmin } from '../../test/datosRecursos'

const RUTA = '/panel/recursos/sillones'

describe('pestaña Sillones (spec gestion-recursos)', () => {
  afterEach(() => {
    sessionStorage.clear()
    vi.unstubAllGlobals()
  })

  it('lista los sillones con nombre y estado en texto', async () => {
    simularApi(apiDeRecursos())
    renderizarEn(RUTA, sesionAdmin)

    const tabla = await screen.findByRole('table', { name: 'Sillones del consultorio' })
    const filas = within(tabla).getAllByRole('row').slice(1)
    expect(filas).toHaveLength(2)
    expect(within(filas[0]).getByText('Sillón 1')).toBeInTheDocument()
    expect(within(filas[0]).getByText('Activo')).toBeInTheDocument()
    expect(within(filas[1]).getByText('Inactivo')).toBeInTheDocument()
    expect(within(tabla).getAllByRole('columnheader').map((th) => th.textContent)).toEqual([
      'Nombre',
      'Estado',
      'Acciones',
    ])
  })

  it('recepción ve el listado sin alta ni acciones', async () => {
    simularApi(apiDeRecursos(sesionDe('recepcion').usuario))
    renderizarEn(RUTA, sesionDe('recepcion'))

    const tabla = await screen.findByRole('table', { name: 'Sillones del consultorio' })
    expect(within(tabla).getAllByRole('row')).toHaveLength(3)
    expect(screen.queryByRole('button', { name: 'Nuevo sillón' })).not.toBeInTheDocument()
    expect(within(tabla).queryAllByRole('button')).toHaveLength(0)
    expect(within(tabla).queryByRole('columnheader', { name: 'Acciones' })).not.toBeInTheDocument()
  })

  it('el administrador ve el alta y las acciones de cada fila', async () => {
    simularApi(apiDeRecursos())
    renderizarEn(RUTA, sesionAdmin)

    expect(await screen.findByRole('button', { name: 'Editar Sillón 1' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Nuevo sillón' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Desactivar Sillón 1' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Reactivar Sillón 2' })).toBeInTheDocument()
  })

  it('alta exitosa: cierra el formulario, anuncia y refresca el listado', async () => {
    const usuario = userEvent.setup()
    let listado = SILLONES
    simularApi(
      apiDeRecursos(undefined, {
        'GET /api/sillones': () => ({ status: 200, cuerpo: listado }),
        'POST /api/sillones': (cuerpo) => {
          const nuevo = { id: 3, nombre: (cuerpo as { nombre: string }).nombre, activo: true }
          listado = [...SILLONES, nuevo]
          return { status: 201, cuerpo: nuevo }
        },
      }),
    )
    renderizarEn(RUTA, sesionAdmin)

    await usuario.click(await screen.findByRole('button', { name: 'Nuevo sillón' }))
    const dialogo = screen.getByRole('dialog', { name: 'Nuevo sillón' })
    await usuario.type(within(dialogo).getByLabelText('Nombre'), 'Sillón 3')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Crear sillón' }))

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(screen.getByRole('status')).toHaveTextContent('Sillón creado')
    expect(await screen.findByText('Sillón 3')).toBeInTheDocument()
  })

  it('un nombre repetido deja el formulario abierto con el mensaje de la API', async () => {
    const usuario = userEvent.setup()
    simularApi(
      apiDeRecursos(undefined, {
        'POST /api/sillones': { status: 409, cuerpo: { detail: 'Ya existe un sillón con ese nombre' } },
      }),
    )
    renderizarEn(RUTA, sesionAdmin)

    await usuario.click(await screen.findByRole('button', { name: 'Nuevo sillón' }))
    const dialogo = screen.getByRole('dialog', { name: 'Nuevo sillón' })
    await usuario.type(within(dialogo).getByLabelText('Nombre'), 'sillón 1')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Crear sillón' }))

    expect(await within(dialogo).findByText('Ya existe un sillón con ese nombre')).toBeInTheDocument()
    expect(screen.getByRole('dialog', { name: 'Nuevo sillón' })).toBeInTheDocument()
  })

  it('editar precarga el nombre y envía el cambio', async () => {
    const usuario = userEvent.setup()
    const api = simularApi(
      apiDeRecursos(undefined, {
        'PUT /api/sillones/1': (cuerpo) => ({ status: 200, cuerpo: { id: 1, ...(cuerpo as object), activo: true } }),
      }),
    )
    renderizarEn(RUTA, sesionAdmin)

    await usuario.click(await screen.findByRole('button', { name: 'Editar Sillón 1' }))
    const dialogo = screen.getByRole('dialog', { name: 'Editar Sillón 1' })
    const nombre = within(dialogo).getByLabelText('Nombre')
    expect(nombre).toHaveValue('Sillón 1')
    await usuario.clear(nombre)
    await usuario.type(nombre, 'Box A')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Guardar cambios' }))

    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('Sillón actualizado'))
    expect(pedidos(api)).toContain('PUT /api/sillones/1')
  })

  it('desactivar pide confirmación y cancelar no llama a la API', async () => {
    const usuario = userEvent.setup()
    const api = simularApi(apiDeRecursos())
    renderizarEn(RUTA, sesionAdmin)

    await usuario.click(await screen.findByRole('button', { name: 'Desactivar Sillón 1' }))
    const dialogo = screen.getByRole('dialog', { name: '¿Desactivar Sillón 1?' })
    expect(dialogo).toHaveTextContent('se conserva')
    expect(dialogo).toHaveTextContent('turnos nuevos')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Cancelar' }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(pedidos(api)).not.toContain('POST /api/sillones/1/desactivar')
  })

  it('confirmar la desactivación la envía y la anuncia', async () => {
    const usuario = userEvent.setup()
    const api = simularApi(
      apiDeRecursos(undefined, {
        'POST /api/sillones/1/desactivar': { status: 200, cuerpo: { id: 1, nombre: 'Sillón 1', activo: false } },
      }),
    )
    renderizarEn(RUTA, sesionAdmin)

    await usuario.click(await screen.findByRole('button', { name: 'Desactivar Sillón 1' }))
    await usuario.click(
      within(screen.getByRole('dialog', { name: '¿Desactivar Sillón 1?' })).getByRole('button', { name: 'Desactivar' }),
    )

    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('Sillón desactivado'))
    expect(pedidos(api)).toContain('POST /api/sillones/1/desactivar')
  })

  it('reactivar no pide confirmación', async () => {
    const usuario = userEvent.setup()
    const api = simularApi(
      apiDeRecursos(undefined, {
        'POST /api/sillones/2/activar': { status: 200, cuerpo: { id: 2, nombre: 'Sillón 2', activo: true } },
      }),
    )
    renderizarEn(RUTA, sesionAdmin)

    await usuario.click(await screen.findByRole('button', { name: 'Reactivar Sillón 2' }))

    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('Sillón reactivado'))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(pedidos(api)).toContain('POST /api/sillones/2/activar')
  })

  it('si el listado falla lo informa sin romperse', async () => {
    simularApi(apiDeRecursos(undefined, { 'GET /api/sillones': { status: 500, cuerpo: { detail: 'Error' } } }))
    renderizarEn(RUTA, sesionAdmin)

    expect(await screen.findByRole('alert', {}, { timeout: 4000 })).toHaveTextContent(
      'No pudimos cargar los sillones',
    )
  })
})
