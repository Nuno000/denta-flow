/**
 * Estado de la pantalla de agenda guardado en la URL (design D5): vista, fecha y filtros. Un valor
 * inválido vuelve a su valor por defecto, así nunca se consulta la API con basura.
 */
import { agregarFiltros, esEstadoTurno, type FiltrosAgenda } from './api'
import { esFechaValida, type Vista } from './fechas'

export interface ParametrosAgenda {
  vista: Vista
  fecha: string
  filtros: FiltrosAgenda
}

function idPositivo(valor: string | null): number | undefined {
  if (valor === null || !/^\d+$/.test(valor)) {
    return undefined
  }
  const numero = Number(valor)
  return numero > 0 ? numero : undefined
}

export function normalizarParametros(params: URLSearchParams, hoy: string): ParametrosAgenda {
  const fecha = params.get('fecha') ?? ''
  const estado = params.get('estado') ?? ''
  const filtros: FiltrosAgenda = {}
  const profesionalId = idPositivo(params.get('profesionalId'))
  const sillonId = idPositivo(params.get('sillonId'))
  if (profesionalId !== undefined) filtros.profesionalId = profesionalId
  if (sillonId !== undefined) filtros.sillonId = sillonId
  if (esEstadoTurno(estado)) filtros.estado = estado
  return {
    vista: params.get('vista') === 'semana' ? 'semana' : 'dia',
    fecha: esFechaValida(fecha) ? fecha : hoy,
    filtros,
  }
}

/** Parámetros de URL para un estado de la agenda; la vista diaria (por defecto) no se escribe. */
export function parametrosDeAgenda({ vista, fecha, filtros }: ParametrosAgenda): URLSearchParams {
  const params = new URLSearchParams()
  if (vista === 'semana') params.set('vista', vista)
  params.set('fecha', fecha)
  agregarFiltros(params, filtros)
  return params
}
