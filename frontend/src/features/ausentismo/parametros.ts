import { esFechaValida, sumarDias } from '../agenda/fechas'

export const MAXIMO_DIAS_AUSENTISMO = 366
export const MINIMO_AUSENCIAS_DEFECTO = 2
export const OPCIONES_MINIMO_AUSENCIAS = [1, 2, 3, 5] as const

export interface FiltrosAusentismo {
  desde: string
  hasta: string
  profesionalId?: number
  minimoAusencias: number
}

const MS_POR_DIA = 86_400_000

const formatoPorcentaje = new Intl.NumberFormat('es-AR', {
  minimumFractionDigits: 1,
  maximumFractionDigits: 1,
})

function aMediodiaUtc(fecha: string): number {
  const [anio, mes, dia] = fecha.split('-').map(Number)
  return Date.UTC(anio, mes - 1, dia, 12)
}

function enteroPositivo(valor: string | null): number | undefined {
  if (valor === null || !/^\d+$/.test(valor)) {
    return undefined
  }
  const numero = Number(valor)
  return Number.isSafeInteger(numero) && numero > 0 ? numero : undefined
}

/** Rango por defecto: últimos 30 días inclusivos en hora del consultorio. */
export function rangoUltimosDias(hoy: string, dias: number): { desde: string; hasta: string } {
  return { desde: sumarDias(hoy, -(dias - 1)), hasta: hoy }
}

/** Normaliza los parámetros de la URL a filtros seguros. */
export function normalizarParametrosAusentismo(params: URLSearchParams, hoy: string): FiltrosAusentismo {
  const defecto = rangoUltimosDias(hoy, 30)
  const desdeCrudo = params.get('desde')
  const hastaCrudo = params.get('hasta')
  const desde = desdeCrudo !== null && esFechaValida(desdeCrudo) ? desdeCrudo : defecto.desde
  const hasta = hastaCrudo !== null && esFechaValida(hastaCrudo) ? hastaCrudo : defecto.hasta
  const profesionalId = enteroPositivo(params.get('profesionalId'))
  const minimoAusencias = enteroPositivo(params.get('minimoAusencias')) ?? MINIMO_AUSENCIAS_DEFECTO

  return { desde, hasta, profesionalId, minimoAusencias }
}

/** Serializa los filtros para sincronizarlos en la URL de la pantalla. */
export function parametrosDeAusentismo(filtros: FiltrosAusentismo): URLSearchParams {
  const params = new URLSearchParams({
    desde: filtros.desde,
    hasta: filtros.hasta,
    minimoAusencias: String(filtros.minimoAusencias),
  })
  if (filtros.profesionalId !== undefined) {
    params.set('profesionalId', String(filtros.profesionalId))
  }
  return params
}

/** Valida el rango de fechas antes de llamar a la API. Devuelve `null` si es válido. */
export function validarRangoAusentismo(desde: string, hasta: string): string | null {
  if (!esFechaValida(desde) || !esFechaValida(hasta)) {
    return 'Ingresá fechas válidas en formato AAAA-MM-DD.'
  }
  const dias = Math.round((aMediodiaUtc(hasta) - aMediodiaUtc(desde)) / MS_POR_DIA) + 1
  if (dias < 1) {
    return 'La fecha hasta no puede ser anterior a la fecha desde.'
  }
  if (dias > MAXIMO_DIAS_AUSENTISMO) {
    return `El rango no puede superar ${MAXIMO_DIAS_AUSENTISMO} días.`
  }
  return null
}

/** Formatea una tasa (0..100) en estilo argentino con 1 decimal (p. ej. "25,0 %"). */
export function formatearPorcentaje(tasa: number): string {
  return `${formatoPorcentaje.format(tasa)} %`
}
