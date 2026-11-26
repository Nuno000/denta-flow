/**
 * Fechas de la agenda en la zona del consultorio (SU-09, spec zona-horaria-consultorio; design D4).
 * Las fechas civiles viajan como cadenas `AAAA-MM-DD`; la aritmética se hace en UTC sobre esa fecha civil
 * y el formato usa `Intl` con `timeZone`, así nada depende de la zona de la máquina del usuario.
 */

export const ZONA_CONSULTORIO = 'America/Argentina/Buenos_Aires'

export type Vista = 'dia' | 'semana'

/** Rango de días del consultorio, ambos inclusivos. */
export interface RangoAgenda {
  desde: string
  hasta: string
}

const MS_POR_DIA = 86_400_000
const FORMATO_ISO = /^\d{4}-\d{2}-\d{2}$/

const fechaCivilDelConsultorio = new Intl.DateTimeFormat('en-CA', {
  timeZone: ZONA_CONSULTORIO,
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
})

const horaDelConsultorio = new Intl.DateTimeFormat('es-AR', {
  timeZone: ZONA_CONSULTORIO,
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
})

// Las fechas civiles se representan al mediodía UTC y se formatean en UTC: el día nunca se corre.
const partesDeFechaLarga = new Intl.DateTimeFormat('es-AR', {
  timeZone: 'UTC',
  weekday: 'long',
  day: 'numeric',
  month: 'long',
  year: 'numeric',
})

function aInstanteCivil(fecha: string): number {
  const [anio, mes, dia] = fecha.split('-').map(Number)
  return Date.UTC(anio, mes - 1, dia, 12)
}

function deInstanteCivil(instante: number): string {
  return new Date(instante).toISOString().slice(0, 10)
}

/** `true` si la cadena es una fecha `AAAA-MM-DD` que existe en el calendario. */
export function esFechaValida(fecha: string): boolean {
  return FORMATO_ISO.test(fecha) && deInstanteCivil(aInstanteCivil(fecha)) === fecha
}

/** Fecha de hoy en el consultorio. */
export function hoyEnConsultorio(ahora: Date = new Date()): string {
  return fechaCivilDelConsultorio.format(ahora)
}

export function sumarDias(fecha: string, dias: number): string {
  return deInstanteCivil(aInstanteCivil(fecha) + dias * MS_POR_DIA)
}

/** Lunes de la semana (lunes a domingo) que contiene la fecha. */
export function lunesDeLaSemana(fecha: string): string {
  const diaDeSemana = new Date(aInstanteCivil(fecha)).getUTCDay() // 0 = domingo
  const desdeLunes = (diaDeSemana + 6) % 7
  return sumarDias(fecha, -desdeLunes)
}

export function diasDeLaSemana(fecha: string): string[] {
  const lunes = lunesDeLaSemana(fecha)
  return Array.from({ length: 7 }, (_, i) => sumarDias(lunes, i))
}

/** Hora del consultorio (`HH:mm`, 24 horas) de un instante ISO-8601. */
export function horaEnConsultorio(instanteIso: string): string {
  return horaDelConsultorio.format(new Date(instanteIso))
}

function partes(fecha: string): Record<string, string> {
  const resultado: Record<string, string> = {}
  for (const parte of partesDeFechaLarga.formatToParts(new Date(aInstanteCivil(fecha)))) {
    resultado[parte.type] = parte.value
  }
  return resultado
}

/** "jueves 26 de noviembre de 2026". */
export function fechaLarga(fecha: string): string {
  const { weekday, day, month, year } = partes(fecha)
  return `${weekday} ${day} de ${month} de ${year}`
}

/** "lunes 30/11". */
export function fechaCorta(fecha: string): string {
  const [, mes, dia] = fecha.split('-')
  return `${partes(fecha).weekday} ${Number(dia)}/${mes}`
}

/** Días que pide cada vista: uno solo o la semana de lunes a domingo. */
export function rangoDeVista(vista: Vista, fecha: string): RangoAgenda {
  if (vista === 'dia') {
    return { desde: fecha, hasta: fecha }
  }
  const lunes = lunesDeLaSemana(fecha)
  return { desde: lunes, hasta: sumarDias(lunes, 6) }
}

/** Fecha del consultorio en la que cae un instante ISO-8601 (p. ej., 02:30Z es el día anterior). */
export function fechaDeInstante(instanteIso: string): string {
  return hoyEnConsultorio(new Date(instanteIso))
}

/** "Semana del lunes 30/11 al domingo 6/12". */
export function tituloDeSemana(fecha: string): string {
  const lunes = lunesDeLaSemana(fecha)
  return `Semana del ${fechaCorta(lunes)} al ${fechaCorta(sumarDias(lunes, 6))}`
}
