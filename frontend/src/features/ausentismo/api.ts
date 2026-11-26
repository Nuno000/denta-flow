/**
 * Único punto de contacto del panel de ausentismo con la API (C-23, US-031).
 * Nunca recibe DNI, teléfono, email ni token de pacientes (Ley 25.326).
 */
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { apiClient } from '../../lib/apiClient'
import { parametrosDeAusentismo, type FiltrosAusentismo } from './parametros'

export const RUTA_AUSENTISMO = '/api/reportes/ausentismo'

export interface ResumenAusentismo {
  totalEvaluados: number
  asistidos: number
  ausentesSinAviso: number
  ausentesConAviso: number
  totalAusentes: number
  tasaAusentismo: number
}

export interface AusentismoPorProfesional extends ResumenAusentismo {
  profesionalId: number
  nombre: string
  apellido: string
}

export interface AusentismoPorFranja extends ResumenAusentismo {
  codigo: string
  etiqueta: string
}

export interface AusentismoPorDiaSemana extends ResumenAusentismo {
  dia: number
  etiqueta: string
}

export interface AusentismoPorPaciente extends ResumenAusentismo {
  pacienteId: number
  nombre: string
  apellido: string
}

export interface ReporteDeAusentismo {
  desde: string
  hasta: string
  profesionalId: number | null
  minimoAusencias: number
  general: ResumenAusentismo
  porProfesional: AusentismoPorProfesional[]
  porFranja: AusentismoPorFranja[]
  porDiaSemana: AusentismoPorDiaSemana[]
  pacientesReiterados: AusentismoPorPaciente[]
}

export function construirConsultaAusentismo(filtros: FiltrosAusentismo): string {
  return `${RUTA_AUSENTISMO}?${parametrosDeAusentismo(filtros).toString()}`
}

export function obtenerReporteDeAusentismo(filtros: FiltrosAusentismo): Promise<ReporteDeAusentismo> {
  return apiClient.get<ReporteDeAusentismo>(construirConsultaAusentismo(filtros))
}

export function useReporteDeAusentismo(filtros: FiltrosAusentismo, habilitado = true) {
  return useQuery({
    queryKey: ['ausentismo', filtros],
    queryFn: () => obtenerReporteDeAusentismo(filtros),
    enabled: habilitado,
    placeholderData: keepPreviousData,
  })
}
