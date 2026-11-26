import { render, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { TURNOS_DEL_1_12 } from '../../test/datosAgenda'
import { TarjetaDeTurno } from './TarjetaDeTurno'

const TURNO = TURNOS_DEL_1_12[0]

describe('tarjeta de turno', () => {
  it('rotula prestación, profesional y sillón', () => {
    render(<ol><TarjetaDeTurno turno={TURNO} /></ol>)
    const tarjeta = screen.getByRole('listitem')
    expect(within(tarjeta).getByText('Prestación')).toBeVisible()
    expect(within(tarjeta).getByText('Laura Ejemplo')).toBeInTheDocument()
  })

  it('la versión compacta (columnas angostas de la semana) apila los datos y deja los rótulos para lectores de pantalla', () => {
    render(<ol><TarjetaDeTurno turno={TURNO} compacta /></ol>)
    const tarjeta = screen.getByRole('listitem')
    expect(within(tarjeta).getByText('Prestación')).toHaveClass('sr-only')
    expect(tarjeta.querySelector('dl')).toHaveClass('grid-cols-1')
  })
})
