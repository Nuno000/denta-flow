import { ErrorApi } from './apiClient'

interface CuerpoProblema {
  detail?: string
  errores?: Record<string, string>
}

/** Traduce un error de la API a mensajes por campo y uno general, para mostrarlos en el formulario. */
export function leerErrorDeApi(error: unknown): { campos: Record<string, string>; general: string } {
  if (error instanceof ErrorApi && typeof error.cuerpo === 'object' && error.cuerpo !== null) {
    const cuerpo = error.cuerpo as CuerpoProblema
    return {
      campos: cuerpo.errores ?? {},
      general: cuerpo.detail ?? 'No se pudo completar la operación',
    }
  }
  return { campos: {}, general: 'No pudimos conectar con el servidor. Probá de nuevo.' }
}
