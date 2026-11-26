import { describe, expect, it } from 'vitest'
import { normalizarParametros, parametrosDeAgenda } from './parametros'

const HOY = '2026-11-26'

function normalizar(query: string) {
  return normalizarParametros(new URLSearchParams(query), HOY)
}

describe('parámetros de la agenda en la URL (design D5)', () => {
  it('sin parámetros: vista diaria de hoy, sin filtros', () => {
    expect(normalizar('')).toEqual({ vista: 'dia', fecha: HOY, filtros: {} })
  })

  it('lee vista, fecha y filtros válidos', () => {
    expect(normalizar('vista=semana&fecha=2026-12-02&profesionalId=10&sillonId=2&estado=pendiente')).toEqual({
      vista: 'semana',
      fecha: '2026-12-02',
      filtros: { profesionalId: 10, sillonId: 2, estado: 'pendiente' },
    })
  })

  it('valores inválidos vuelven al valor por defecto', () => {
    expect(normalizar('vista=mes&fecha=2026-02-30&profesionalId=abc&sillonId=-1&estado=borrado')).toEqual({
      vista: 'dia',
      fecha: HOY,
      filtros: {},
    })
  })

  it('los identificadores deben ser enteros positivos', () => {
    expect(normalizar('profesionalId=1.5&sillonId=0').filtros).toEqual({})
  })

  it('escribe solo lo que difiere del valor por defecto', () => {
    expect(parametrosDeAgenda({ vista: 'dia', fecha: '2026-11-27', filtros: {} }).toString()).toBe(
      'fecha=2026-11-27',
    )
    expect(
      parametrosDeAgenda({ vista: 'semana', fecha: '2026-12-02', filtros: { sillonId: 2, estado: 'cancelado' } })
        .toString(),
    ).toBe('vista=semana&fecha=2026-12-02&sillonId=2&estado=cancelado')
  })
})
