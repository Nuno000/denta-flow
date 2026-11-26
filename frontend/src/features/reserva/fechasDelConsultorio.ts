/**
 * Fechas y horas de la reserva siempre en la zona del consultorio (SU-09, design D4), nunca en la del navegador:
 * los días se calculan en `America/Argentina/Buenos_Aires` y las horas se leen del texto ISO con offset que
 * devuelve la API (`2026-12-02T10:00:00-03:00`).
 */
export const ZONA_CONSULTORIO = 'America/Argentina/Buenos_Aires'

/** Días que ofrece la reserva: el máximo de C-10 (design Q5). */
export const DIAS_OFRECIDOS = 14

export interface DiaDeReserva {
  /** AAAA-MM-DD en el consultorio. */
  fecha: string
  /** Texto corto visible, p. ej. "mié 2/12". */
  etiqueta: string
}

const FECHA_ISO = new Intl.DateTimeFormat('en-CA', {
  timeZone: ZONA_CONSULTORIO,
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
})

const ETIQUETA_DIA = new Intl.DateTimeFormat('es-AR', { timeZone: 'UTC', weekday: 'short', day: 'numeric', month: 'numeric' })

/** Hoy y los días siguientes en el consultorio, a partir del instante `ahora`. */
export function proximosDias(ahora: Date, cantidad = DIAS_OFRECIDOS): DiaDeReserva[] {
  const [anio, mes, dia] = FECHA_ISO.format(ahora).split('-').map(Number)
  return Array.from({ length: cantidad }, (_, i) => {
    const fecha = new Date(Date.UTC(anio, mes - 1, dia + i))
    return { fecha: fecha.toISOString().slice(0, 10), etiqueta: ETIQUETA_DIA.format(fecha).replace(',', '') }
  })
}

/** "10:00" de "2026-12-02T10:00:00-03:00". */
export function horaDe(instanteIso: string): string {
  return instanteIso.slice(11, 16)
}

/** "02/12/2026 10:00" de "2026-12-02T10:00:00-03:00". */
export function fechaYHoraDe(instanteIso: string): string {
  const [anio, mes, dia] = instanteIso.slice(0, 10).split('-')
  return `${dia}/${mes}/${anio} ${horaDe(instanteIso)}`
}
