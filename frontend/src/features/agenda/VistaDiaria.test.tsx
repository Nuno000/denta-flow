import { screen, within } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { renderizarEn, simularApi } from '../../test/apiSimulada'
import { apiDeAgenda, sesionRecepcion } from '../../test/datosAgenda'

describe('vista diaria (spec "Vista diaria")', () => {
  afterEach(() => {
    sessionStorage.clear()
    vi.unstubAllGlobals()
  })

  it('lista los turnos en orden con horario, paciente, prestación, profesional, sillón y estado', async () => {
    simularApi(apiDeAgenda(sesionRecepcion.usuario))
    renderizarEn('/panel/agenda?fecha=2026-12-01', sesionRecepcion)

    const lista = await screen.findByRole('list', { name: 'Turnos del martes 1 de diciembre de 2026' })
    const turnos = within(lista).getAllByRole('listitem')
    expect(turnos).toHaveLength(6)

    const primero = within(turnos[0])
    expect(primero.getByText('09:00–09:30')).toBeInTheDocument()
    expect(primero.getByText('Ana Ejemplar')).toBeInTheDocument()
    expect(primero.getByText('Consulta')).toBeInTheDocument()
    expect(primero.getByText('Laura Ejemplo')).toBeInTheDocument()
    expect(primero.getByText('Sillón 1')).toBeInTheDocument()
    expect(primero.getByText('Confirmado')).toBeInTheDocument()

    expect(within(turnos[1]).getByText('10:00–10:40')).toBeInTheDocument()
    expect(within(turnos[1]).getByText('Martín Prueba')).toBeInTheDocument()
    expect(within(turnos[1]).getByText('Pendiente')).toBeInTheDocument()
  })

  it('muestra en hora del consultorio el turno de las 23:30, que en UTC ya es del día siguiente', async () => {
    simularApi(apiDeAgenda(sesionRecepcion.usuario))
    renderizarEn('/panel/agenda?fecha=2026-12-01', sesionRecepcion)

    const lista = await screen.findByRole('list', { name: /Turnos del/ })
    const ultimo = within(lista).getAllByRole('listitem').at(-1)
    expect(ultimo).toHaveTextContent('23:30–00:00')
  })

  it('marca el sobreturno en texto', async () => {
    simularApi(apiDeAgenda(sesionRecepcion.usuario))
    renderizarEn('/panel/agenda?fecha=2026-12-01', sesionRecepcion)

    const lista = await screen.findByRole('list', { name: /Turnos del/ })
    const turnos = within(lista).getAllByRole('listitem')
    expect(within(turnos[2]).getByText('Sobreturno')).toBeInTheDocument()
    expect(within(turnos[0]).queryByText('Sobreturno')).not.toBeInTheDocument()
  })

  it('avisa cuando el día no tiene turnos', async () => {
    simularApi(apiDeAgenda(sesionRecepcion.usuario, { status: 200, cuerpo: [] }))
    renderizarEn('/panel/agenda?fecha=2026-12-01', sesionRecepcion)

    expect(await screen.findByText('No hay turnos para este día')).toBeInTheDocument()
    expect(screen.getByText('0 turnos el martes 1 de diciembre de 2026')).toBeInTheDocument()
  })
})
