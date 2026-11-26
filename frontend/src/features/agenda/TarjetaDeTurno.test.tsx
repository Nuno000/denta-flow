import { QueryClientProvider } from '@tanstack/react-query'
import { render, screen, within } from '@testing-library/react'
import type { ReactNode } from 'react'
import { describe, expect, it } from 'vitest'
import { crearQueryClient } from '../../lib/queryClient'
import { TURNOS_DEL_1_12 } from '../../test/datosAgenda'
import { TarjetaDeTurno } from './TarjetaDeTurno'

const TURNO = TURNOS_DEL_1_12[0]

function mostrar(contenido: ReactNode) {
  return render(<QueryClientProvider client={crearQueryClient()}>{contenido}</QueryClientProvider>)
}

describe('tarjeta de turno', () => {
  it('rotula prestación, profesional y sillón', () => {
    mostrar(<ol><TarjetaDeTurno turno={TURNO} /></ol>)
    const tarjeta = screen.getByRole('listitem')
    expect(within(tarjeta).getByText('Prestación')).toBeVisible()
    expect(within(tarjeta).getByText('Laura Ejemplo')).toBeInTheDocument()
  })

  it('la versión compacta (columnas angostas de la semana) apila los datos y deja los rótulos para lectores de pantalla', () => {
    mostrar(<ol><TarjetaDeTurno turno={TURNO} compacta /></ol>)
    const tarjeta = screen.getByRole('listitem')
    expect(within(tarjeta).getByText('Prestación')).toHaveClass('sr-only')
    expect(tarjeta.querySelector('dl')).toHaveClass('grid-cols-1')
  })
})
