import { describe, expect, it } from 'vitest'
import { MENSAJE_AVISO_NO_ACEPTADO } from './avisoPrivacidadApi'
import { esquemaDatosReserva, type DatosReserva } from './esquemasReserva'

const VALIDOS: DatosReserva = {
  dni: '30.111.222',
  nombre: 'Carla',
  apellido: 'Ejemplo',
  telefono: '(0261) 555-0000',
  email: 'carla@example.com',
  aceptaAvisoPrivacidad: true,
}

function erroresDe(datos: Partial<Record<keyof DatosReserva, unknown>>): Record<string, string> {
  const resultado = esquemaDatosReserva.safeParse({ ...VALIDOS, ...datos })
  if (resultado.success) {
    return {}
  }
  return Object.fromEntries(resultado.error.issues.map((i) => [String(i.path[0]), i.message]))
}

describe('esquema de los datos de la reserva (spec "Datos de la reserva validados en el backend", design D6)', () => {
  it('acepta datos válidos y normaliza DNI, teléfono y email como el backend', () => {
    const resultado = esquemaDatosReserva.parse({ ...VALIDOS, email: ' Carla@Example.com ' })

    expect(resultado.dni).toBe('30111222')
    expect(resultado.telefono).toBe('02615550000')
    expect(resultado.email).toBe('carla@example.com')
  })

  it.each(['1234567', '12345678', '12.345.678'])('acepta el DNI %s', (dni) => {
    expect(erroresDe({ dni })).toEqual({})
  })

  it.each(['', '123456', '123456789', '12a45678'])('rechaza el DNI "%s"', (dni) => {
    expect(erroresDe({ dni }).dni).toBe('Ingresá un DNI de 7 u 8 dígitos')
  })

  it('exige nombre y apellido', () => {
    expect(erroresDe({ nombre: '  ', apellido: '' })).toEqual({
      nombre: 'Ingresá tu nombre',
      apellido: 'Ingresá tu apellido',
    })
  })

  it.each(['', '1234567', '12345678901234567', 'abc12345678'])('rechaza el teléfono "%s"', (telefono) => {
    expect(erroresDe({ telefono }).telefono).toBe('Ingresá un teléfono de 8 a 15 dígitos')
  })

  it.each(['', 'ana@', 'ana@example', 'ana example@x.com'])('rechaza el email "%s"', (email) => {
    expect(erroresDe({ email }).email).toBe('Ingresá un email válido')
  })

  it('exige aceptar el aviso de privacidad con el mensaje de C-15', () => {
    expect(erroresDe({ aceptaAvisoPrivacidad: false }).aceptaAvisoPrivacidad).toBe(MENSAJE_AVISO_NO_ACEPTADO)
  })
})
