import { describe, expect, it } from 'vitest'
import {
  formatearPorcentaje,
  normalizarParametrosAusentismo,
  parametrosDeAusentismo,
  validarRangoAusentismo,
} from './parametros'

describe('parametros de ausentismo', () => {
  it('sin parámetros usa últimos 30 días en hora del consultorio y mínimo 2', () => {
    const filtros = normalizarParametrosAusentismo(new URLSearchParams(), '2026-11-26')

    expect(filtros).toEqual({
      desde: '2026-10-28',
      hasta: '2026-11-26',
      profesionalId: undefined,
      minimoAusencias: 2,
    })
  })

  it('conserva fechas válidas, profesionalId y minimoAusencias desde la URL', () => {
    const params = new URLSearchParams('desde=2026-11-01&hasta=2026-11-26&profesionalId=3&minimoAusencias=3')

    expect(normalizarParametrosAusentismo(params, '2026-11-26')).toEqual({
      desde: '2026-11-01',
      hasta: '2026-11-26',
      profesionalId: 3,
      minimoAusencias: 3,
    })
  })

  it('ignora valores inválidos en la URL y vuelve a los defaults seguros', () => {
    const params = new URLSearchParams('desde=no-fecha&hasta=2026-02-30&profesionalId=abc&minimoAusencias=0')

    expect(normalizarParametrosAusentismo(params, '2026-11-26')).toEqual({
      desde: '2026-10-28',
      hasta: '2026-11-26',
      profesionalId: undefined,
      minimoAusencias: 2,
    })
  })

  it('serializa parámetros a URLSearchParams omitiendo profesionalId vacío', () => {
    const params = parametrosDeAusentismo({
      desde: '2026-11-01',
      hasta: '2026-11-26',
      minimoAusencias: 2,
    })

    expect(params.toString()).toBe('desde=2026-11-01&hasta=2026-11-26&minimoAusencias=2')
  })

  it('valida rango invertido y máximo de 366 días', () => {
    expect(validarRangoAusentismo('2026-11-26', '2026-11-01')).toContain('anterior')
    expect(validarRangoAusentismo('2025-01-01', '2026-01-02')).toContain('366')
    expect(validarRangoAusentismo('2026-11-01', '2026-11-26')).toBeNull()
  })

  it('formatea porcentajes en formato argentino con un decimal', () => {
    expect(formatearPorcentaje(0)).toBe('0,0 %')
    expect(formatearPorcentaje(66.7)).toBe('66,7 %')
  })
})
