import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { pedidos, renderizarEn, sesionDe, simularApi } from '../../test/apiSimulada'
import { PROFESIONALES, apiDeRecursos, sesionAdmin } from '../../test/datosRecursos'

const RUTA = '/panel/recursos/profesionales'

describe('pestaña Profesionales (spec gestion-recursos)', () => {
  afterEach(() => {
    sessionStorage.clear()
    vi.unstubAllGlobals()
  })

  it('lista nombre completo, email, matrícula, estado y los nombres de sus prestaciones', async () => {
    simularApi(apiDeRecursos())
    renderizarEn(RUTA, sesionAdmin)

    const tabla = await screen.findByRole('table', { name: 'Profesionales del consultorio' })
    const filas = within(tabla).getAllByRole('row').slice(1)
    expect(filas).toHaveLength(2)
    expect(within(filas[0]).getByText('Laura Ejemplo')).toBeInTheDocument()
    expect(within(filas[0]).getByText('laura@example.com')).toBeInTheDocument()
    expect(within(filas[0]).getByText('MP-0001')).toBeInTheDocument()
    expect(within(filas[0]).getByText('Blanqueamiento (inactiva), Limpieza')).toBeInTheDocument()
    expect(within(filas[1]).getByText('Inactivo')).toBeInTheDocument()
    expect(within(filas[1]).getAllByText('—')).toHaveLength(2)
  })

  it('recepción no ve alta, edición, desactivación ni asignación', async () => {
    simularApi(apiDeRecursos(sesionDe('recepcion').usuario))
    renderizarEn(RUTA, sesionDe('recepcion'))

    const tabla = await screen.findByRole('table', { name: 'Profesionales del consultorio' })
    expect(within(tabla).queryAllByRole('button')).toHaveLength(0)
    expect(screen.queryByRole('button', { name: 'Nuevo profesional' })).not.toBeInTheDocument()
  })

  it('alta con matrícula vacía la envía como null y anuncia "Profesional creado"', async () => {
    const usuario = userEvent.setup()
    let enviado: unknown
    simularApi(
      apiDeRecursos(undefined, {
        'POST /api/profesionales': (cuerpo) => {
          enviado = cuerpo
          return { status: 201, cuerpo: { id: 12, ...(cuerpo as object), activo: true, prestacionIds: [] } }
        },
      }),
    )
    renderizarEn(RUTA, sesionAdmin)

    await usuario.click(await screen.findByRole('button', { name: 'Nuevo profesional' }))
    const dialogo = screen.getByRole('dialog', { name: 'Nuevo profesional' })
    await usuario.type(within(dialogo).getByLabelText('Nombre'), 'Ana')
    await usuario.type(within(dialogo).getByLabelText('Apellido'), 'Muestra')
    await usuario.type(within(dialogo).getByLabelText('Email'), 'ana@example.com')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Crear profesional' }))

    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('Profesional creado'))
    expect(enviado).toEqual({ nombre: 'Ana', apellido: 'Muestra', matricula: null, email: 'ana@example.com' })
  })

  it('un email repetido muestra el conflicto junto al email', async () => {
    const usuario = userEvent.setup()
    simularApi(
      apiDeRecursos(undefined, {
        'POST /api/profesionales': { status: 409, cuerpo: { detail: 'Ya existe un profesional con ese email' } },
      }),
    )
    renderizarEn(RUTA, sesionAdmin)

    await usuario.click(await screen.findByRole('button', { name: 'Nuevo profesional' }))
    const dialogo = screen.getByRole('dialog', { name: 'Nuevo profesional' })
    await usuario.type(within(dialogo).getByLabelText('Nombre'), 'Ana')
    await usuario.type(within(dialogo).getByLabelText('Apellido'), 'Muestra')
    await usuario.type(within(dialogo).getByLabelText('Email'), 'laura@example.com')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Crear profesional' }))

    expect(await within(dialogo).findByText('Ya existe un profesional con ese email')).toBeInTheDocument()
    expect(within(dialogo).getByLabelText('Email')).toHaveAttribute('aria-invalid', 'true')
  })

  it('editar precarga los datos, incluida la matrícula', async () => {
    const usuario = userEvent.setup()
    let enviado: unknown
    simularApi(
      apiDeRecursos(undefined, {
        'PUT /api/profesionales/10': (cuerpo) => {
          enviado = cuerpo
          return { status: 200, cuerpo: { ...PROFESIONALES[0], ...(cuerpo as object) } }
        },
      }),
    )
    renderizarEn(RUTA, sesionAdmin)

    await usuario.click(await screen.findByRole('button', { name: 'Editar Laura Ejemplo' }))
    const dialogo = screen.getByRole('dialog', { name: 'Editar Laura Ejemplo' })
    expect(within(dialogo).getByLabelText('Matrícula (opcional)')).toHaveValue('MP-0001')
    await usuario.clear(within(dialogo).getByLabelText('Matrícula (opcional)'))
    await usuario.click(within(dialogo).getByRole('button', { name: 'Guardar cambios' }))

    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('Profesional actualizado'))
    expect(enviado).toEqual({ nombre: 'Laura', apellido: 'Ejemplo', matricula: null, email: 'laura@example.com' })
  })

  it('el diálogo de prestaciones ofrece las activas y la inactiva que ya tenía, rotulada', async () => {
    const usuario = userEvent.setup()
    simularApi(apiDeRecursos())
    renderizarEn(RUTA, sesionAdmin)

    await usuario.click(await screen.findByRole('button', { name: 'Prestaciones de Laura Ejemplo' }))
    const dialogo = screen.getByRole('dialog', { name: 'Prestaciones de Laura Ejemplo' })
    const casillas = within(dialogo).getAllByRole('checkbox')
    expect(casillas.map((casilla) => casilla.getAttribute('aria-label') ?? casilla.closest('label')?.textContent)).toEqual([
      'Blanqueamiento (inactiva)',
      'Consulta',
      'Extracción',
      'Limpieza',
    ])
    expect(within(dialogo).getByRole('checkbox', { name: 'Blanqueamiento (inactiva)' })).toBeChecked()
    expect(within(dialogo).getByRole('checkbox', { name: 'Limpieza' })).toBeChecked()
    expect(within(dialogo).getByRole('checkbox', { name: 'Consulta' })).not.toBeChecked()
  })

  it('un profesional sin la inactiva no la ve ofrecida', async () => {
    const usuario = userEvent.setup()
    simularApi(apiDeRecursos())
    renderizarEn(RUTA, sesionAdmin)

    await usuario.click(await screen.findByRole('button', { name: 'Prestaciones de Martín Prueba' }))
    const dialogo = screen.getByRole('dialog', { name: 'Prestaciones de Martín Prueba' })
    expect(within(dialogo).queryByRole('checkbox', { name: /Blanqueamiento/ })).not.toBeInTheDocument()
    expect(within(dialogo).getAllByRole('checkbox')).toHaveLength(3)
  })

  it('guardar envía el conjunto elegido y anuncia "Prestaciones actualizadas"', async () => {
    const usuario = userEvent.setup()
    let enviado: unknown
    const api = simularApi(
      apiDeRecursos(undefined, {
        'PUT /api/profesionales/10/prestaciones': (cuerpo) => {
          enviado = cuerpo
          return { status: 200, cuerpo: { ...PROFESIONALES[0], ...(cuerpo as object) } }
        },
      }),
    )
    renderizarEn(RUTA, sesionAdmin)

    await usuario.click(await screen.findByRole('button', { name: 'Prestaciones de Laura Ejemplo' }))
    const dialogo = screen.getByRole('dialog', { name: 'Prestaciones de Laura Ejemplo' })
    await usuario.click(within(dialogo).getByRole('checkbox', { name: 'Blanqueamiento (inactiva)' }))
    await usuario.click(within(dialogo).getByRole('checkbox', { name: 'Consulta' }))
    await usuario.click(within(dialogo).getByRole('button', { name: 'Guardar prestaciones' }))

    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('Prestaciones actualizadas'))
    expect(enviado).toEqual({ prestacionIds: [2, 4] })
    expect(pedidos(api)).toContain('PUT /api/profesionales/10/prestaciones')
  })

  it('desactivar un profesional pide confirmación', async () => {
    const usuario = userEvent.setup()
    const api = simularApi(
      apiDeRecursos(undefined, {
        'POST /api/profesionales/10/desactivar': { status: 200, cuerpo: { ...PROFESIONALES[0], activo: false } },
      }),
    )
    renderizarEn(RUTA, sesionAdmin)

    await usuario.click(await screen.findByRole('button', { name: 'Desactivar Laura Ejemplo' }))
    await usuario.click(
      within(screen.getByRole('dialog', { name: '¿Desactivar Laura Ejemplo?' })).getByRole('button', {
        name: 'Desactivar',
      }),
    )

    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('Profesional desactivado'))
    expect(pedidos(api)).toContain('POST /api/profesionales/10/desactivar')
  })
})
