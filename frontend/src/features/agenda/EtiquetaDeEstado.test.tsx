import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { ESTADOS_TURNO } from './api'
import { EtiquetaDeEstado, EtiquetaDeSobreturno } from './EtiquetaDeEstado'

describe('estado y sobreturno en texto (spec "Estado y sobreturno visibles sin depender del color")', () => {
  it.each([
    ['pendiente', 'Pendiente'],
    ['confirmado', 'Confirmado'],
    ['cancelado', 'Cancelado'],
    ['asistio', 'Asistió'],
    ['ausente', 'Ausente'],
    ['ausente_con_aviso', 'Ausente con aviso'],
  ] as const)('%s se lee "%s"', (estado, texto) => {
    render(<EtiquetaDeEstado estado={estado} />)
    expect(screen.getByText(texto)).toBeInTheDocument()
  })

  it('cubre los seis estados', () => {
    expect(ESTADOS_TURNO).toHaveLength(6)
  })

  it('el sobreturno se lee en texto', () => {
    render(<EtiquetaDeSobreturno />)
    expect(screen.getByText('Sobreturno')).toBeInTheDocument()
  })
})
