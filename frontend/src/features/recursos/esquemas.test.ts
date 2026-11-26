import { describe, expect, it } from 'vitest'
import { esquemaPrestacion, esquemaProfesional, esquemaSillon } from './esquemas'

function errores(resultado: { success: boolean; error?: { issues: { path: PropertyKey[]; message: string }[] } }) {
  return Object.fromEntries((resultado.error?.issues ?? []).map((issue) => [String(issue.path[0]), issue.message]))
}

describe('esquemas de recursos (spec "Formularios de recursos con validación…", design D6)', () => {
  it.each(['0', '-1', '1.5'])('rechaza la duración %s con el mismo mensaje del backend', (duracion) => {
    const resultado = esquemaPrestacion.safeParse({ nombre: 'Limpieza', duracionMinutos: duracion })

    expect(resultado.success).toBe(false)
    expect(errores(resultado).duracionMinutos).toBe('La duración debe ser mayor que cero')
  })

  it('acepta una duración entera positiva y la convierte en número', () => {
    const resultado = esquemaPrestacion.safeParse({ nombre: ' Limpieza ', duracionMinutos: '40' })

    expect(resultado.success && resultado.data).toEqual({ nombre: 'Limpieza', duracionMinutos: 40 })
  })

  it('pide la duración si está vacía', () => {
    const resultado = esquemaPrestacion.safeParse({ nombre: 'Limpieza', duracionMinutos: '' })

    expect(errores(resultado).duracionMinutos).toBe('Ingresá la duración en minutos')
  })

  it('rechaza un nombre de sillón de 51 caracteres y uno en blanco', () => {
    expect(errores(esquemaSillon.safeParse({ nombre: 'S'.repeat(51) })).nombre).toBe(
      'El nombre no puede superar los 50 caracteres',
    )
    expect(errores(esquemaSillon.safeParse({ nombre: '   ' })).nombre).toBe('Ingresá el nombre')
    expect(esquemaSillon.safeParse({ nombre: 'S'.repeat(50) }).success).toBe(true)
  })

  it('rechaza un email mal formado y un apellido vacío del profesional', () => {
    const resultado = esquemaProfesional.safeParse({
      nombre: 'Ana',
      apellido: ' ',
      matricula: '',
      email: 'mal-formado',
    })

    expect(errores(resultado).email).toBe('El email no tiene un formato válido')
    expect(errores(resultado).apellido).toBe('Ingresá el apellido')
  })

  it('una matrícula vacía queda como null', () => {
    const resultado = esquemaProfesional.safeParse({
      nombre: 'Ana',
      apellido: 'Muestra',
      matricula: '  ',
      email: 'ana@example.com',
    })

    expect(resultado.success && resultado.data.matricula).toBeNull()
  })

  it('rechaza una matrícula de 31 caracteres', () => {
    const resultado = esquemaProfesional.safeParse({
      nombre: 'Ana',
      apellido: 'Muestra',
      matricula: 'M'.repeat(31),
      email: 'ana@example.com',
    })

    expect(errores(resultado).matricula).toBe('La matrícula no puede superar los 30 caracteres')
  })
})
