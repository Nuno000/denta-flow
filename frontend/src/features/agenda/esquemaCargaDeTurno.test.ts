import { describe, expect, it } from 'vitest'
import { esquemaCargaDeTurno } from './esquemaCargaDeTurno'

const VALIDO = {
  paciente: { dni: '30.123.456', nombre: ' Ana ', apellido: 'Ejemplo', telefono: '+54 11 5555-0000', email: 'ana@example.com' },
  prestacionId: '4',
  profesionalId: '10',
  sillonId: '1',
  fecha: '2026-12-01',
  hora: '09:00',
}

function errores(entrada: unknown): Record<string, string> {
  const resultado = esquemaCargaDeTurno.safeParse(entrada)
  return Object.fromEntries((resultado.error?.issues ?? []).map((issue) => [issue.path.join('.'), issue.message]))
}

function con(cambios: Record<string, unknown>, paciente: Record<string, unknown> = {}) {
  return { ...VALIDO, ...cambios, paciente: { ...VALIDO.paciente, ...paciente } }
}

describe('esquemaCargaDeTurno (C-12, design D6)', () => {
  it('acepta datos válidos y los normaliza: DNI y teléfono sin separadores, ids como número', () => {
    const resultado = esquemaCargaDeTurno.safeParse(VALIDO)

    expect(resultado.success && resultado.data).toEqual({
      paciente: { dni: '30123456', nombre: 'Ana', apellido: 'Ejemplo', telefono: '+541155550000', email: 'ana@example.com' },
      prestacionId: 4,
      profesionalId: 10,
      sillonId: 1,
      fecha: '2026-12-01',
      hora: '09:00',
    })
  })

  it('acepta un DNI de 7 dígitos', () => {
    expect(esquemaCargaDeTurno.safeParse(con({}, { dni: '1234567' })).success).toBe(true)
  })

  it.each(['', '   ', '12345', '123456789', '30A23456'])('rechaza el DNI "%s"', (dni) => {
    expect(errores(con({}, { dni }))['paciente.dni']).toBeDefined()
  })

  it.each(['nombre', 'apellido', 'telefono', 'email'] as const)('el campo %s es obligatorio', (campo) => {
    expect(errores(con({}, { [campo]: '  ' }))[`paciente.${campo}`]).toBeDefined()
  })

  it.each(['1234567', '1234567890123456', '11-ab-5555', '++541155550000'])('rechaza el teléfono "%s"', (telefono) => {
    expect(errores(con({}, { telefono }))['paciente.telefono']).toBeDefined()
  })

  it.each(['ana', 'ana@', 'ana@example', 'a na@example.com'])('rechaza el email "%s"', (email) => {
    expect(errores(con({}, { email }))['paciente.email']).toBeDefined()
  })

  it('rechaza nombre de más de 100 caracteres y email de más de 254', () => {
    expect(errores(con({}, { nombre: 'N'.repeat(101) }))['paciente.nombre']).toBeDefined()
    expect(errores(con({}, { email: `${'a'.repeat(250)}@example.com` }))['paciente.email']).toBeDefined()
  })

  it.each(['prestacionId', 'profesionalId', 'sillonId'] as const)('%s es obligatorio', (campo) => {
    expect(errores(con({ [campo]: '' }))[campo]).toBeDefined()
  })

  it.each(['', '01/12/2026', '2026-02-30'])('rechaza la fecha "%s"', (fecha) => {
    expect(errores(con({ fecha })).fecha).toBeDefined()
  })

  it.each(['', '9:00', '24:00', '09:60', '09:00:00'])('rechaza la hora "%s"', (hora) => {
    expect(errores(con({ hora })).hora).toBeDefined()
  })

  it('acepta los extremos del día: 00:00 y 23:55', () => {
    expect(esquemaCargaDeTurno.safeParse(con({ hora: '00:00' })).success).toBe(true)
    expect(esquemaCargaDeTurno.safeParse(con({ hora: '23:55' })).success).toBe(true)
  })
})
