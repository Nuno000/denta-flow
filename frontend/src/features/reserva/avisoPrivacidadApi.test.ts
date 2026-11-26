import { describe, expect, it } from 'vitest'
import { MENSAJE_AVISO_NO_ACEPTADO, esquemaAvisoPrivacidad } from './avisoPrivacidadApi'

describe('esquemaAvisoPrivacidad (spec "Aceptación obligatoria del aviso al reservar")', () => {
  it('acepta la casilla marcada', () => {
    expect(esquemaAvisoPrivacidad.safeParse({ aceptaAvisoPrivacidad: true }).success).toBe(true)
  })

  it.each([
    ['desmarcada', { aceptaAvisoPrivacidad: false }],
    ['ausente', {}],
  ])('rechaza la casilla %s con el mensaje en español', (_caso, datos) => {
    const resultado = esquemaAvisoPrivacidad.safeParse(datos)

    expect(resultado.success).toBe(false)
    expect(resultado.error?.issues[0]?.message).toBe(MENSAJE_AVISO_NO_ACEPTADO)
  })
})
