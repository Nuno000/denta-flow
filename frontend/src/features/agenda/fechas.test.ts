import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  diasDeLaSemana,
  esFechaValida,
  fechaCorta,
  fechaLarga,
  horaEnConsultorio,
  hoyEnConsultorio,
  inicioEnConsultorio,
  lunesDeLaSemana,
  rangoDeVista,
  sumarDias,
} from './fechas'

describe('fechas del consultorio (America/Argentina/Buenos_Aires)', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  describe('hoyEnConsultorio', () => {
    it('devuelve la fecha civil del consultorio', () => {
      expect(hoyEnConsultorio(new Date('2026-11-26T15:00:00Z'))).toBe('2026-11-26')
    })

    it('a las 02:00Z todavía es el día anterior en el consultorio', () => {
      expect(hoyEnConsultorio(new Date('2026-11-27T02:00:00Z'))).toBe('2026-11-26')
    })

    it('a las 03:00Z ya es el día siguiente', () => {
      expect(hoyEnConsultorio(new Date('2026-11-27T03:00:00Z'))).toBe('2026-11-27')
    })

    it('sin argumento usa la hora actual', () => {
      vi.useFakeTimers()
      vi.setSystemTime(new Date('2026-11-27T02:00:00Z'))
      expect(hoyEnConsultorio()).toBe('2026-11-26')
    })
  })

  describe('aritmética de fechas civiles', () => {
    it('suma y resta días cruzando mes y año', () => {
      expect(sumarDias('2026-11-30', 1)).toBe('2026-12-01')
      expect(sumarDias('2026-12-31', 1)).toBe('2027-01-01')
      expect(sumarDias('2027-01-01', -1)).toBe('2026-12-31')
      expect(sumarDias('2026-12-02', 7)).toBe('2026-12-09')
    })

    it('el lunes de la semana de un miércoles', () => {
      expect(lunesDeLaSemana('2026-12-02')).toBe('2026-11-30')
    })

    it('un lunes es su propio lunes y el domingo pertenece a la semana anterior', () => {
      expect(lunesDeLaSemana('2026-11-30')).toBe('2026-11-30')
      expect(lunesDeLaSemana('2026-12-06')).toBe('2026-11-30')
      expect(lunesDeLaSemana('2026-12-07')).toBe('2026-12-07')
    })

    it('los siete días de la semana, de lunes a domingo', () => {
      expect(diasDeLaSemana('2026-12-02')).toEqual([
        '2026-11-30',
        '2026-12-01',
        '2026-12-02',
        '2026-12-03',
        '2026-12-04',
        '2026-12-05',
        '2026-12-06',
      ])
    })

    it('no depende de la zona horaria de la máquina', () => {
      vi.useFakeTimers()
      vi.setSystemTime(new Date('2026-12-31T23:59:00Z'))
      expect(lunesDeLaSemana('2027-01-01')).toBe('2026-12-28')
      expect(sumarDias('2026-12-31', 1)).toBe('2027-01-01')
    })
  })

  describe('formato para la pantalla', () => {
    it('hora del consultorio en 24 horas', () => {
      expect(horaEnConsultorio('2026-12-01T12:00:00Z')).toBe('09:00')
      expect(horaEnConsultorio('2026-12-01T02:30:00Z')).toBe('23:30')
      expect(horaEnConsultorio('2026-12-01T03:05:00Z')).toBe('00:05')
    })

    it('fecha larga en castellano', () => {
      expect(fechaLarga('2026-11-26')).toBe('jueves 26 de noviembre de 2026')
      expect(fechaLarga('2026-12-06')).toBe('domingo 6 de diciembre de 2026')
    })

    it('fecha corta para los encabezados de la semana', () => {
      expect(fechaCorta('2026-11-30')).toBe('lunes 30/11')
    })
  })

  describe('validación y rango de la vista', () => {
    it('reconoce fechas válidas', () => {
      expect(esFechaValida('2026-11-26')).toBe(true)
      expect(esFechaValida('2026-02-30')).toBe(false)
      expect(esFechaValida('26/11/2026')).toBe(false)
      expect(esFechaValida('')).toBe(false)
    })

    it('la vista diaria pide un solo día', () => {
      expect(rangoDeVista('dia', '2026-11-26')).toEqual({ desde: '2026-11-26', hasta: '2026-11-26' })
    })

    it('la vista semanal pide de lunes a domingo', () => {
      expect(rangoDeVista('semana', '2026-12-02')).toEqual({ desde: '2026-11-30', hasta: '2026-12-06' })
    })
  })

  describe('inicioEnConsultorio (C-12)', () => {
    it('arma el inicio con el desplazamiento -03:00 del consultorio', () => {
      expect(inicioEnConsultorio('2026-12-01', '09:00')).toBe('2026-12-01T09:00:00-03:00')
    })

    it('no cambia de día ni de hora: 23:30 sigue siendo 23:30 del mismo día', () => {
      expect(inicioEnConsultorio('2026-12-01', '23:30')).toBe('2026-12-01T23:30:00-03:00')
    })

    it('es el mismo instante que la hora del consultorio, sin importar la zona de la máquina', () => {
      const instante = new Date(inicioEnConsultorio('2026-12-01', '09:00'))

      expect(instante.toISOString()).toBe('2026-12-01T12:00:00.000Z')
      expect(horaEnConsultorio(instante.toISOString())).toBe('09:00')
    })
  })
})
