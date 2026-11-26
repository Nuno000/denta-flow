/** Clases y atributos compartidos por los formularios y tablas del panel (usuarios, recursos). */

export const CLASES_CONTROL =
  'rounded-md border border-slate-500 bg-white px-3 py-2 text-base text-slate-900 focus:outline-none focus-visible:ring-2 focus-visible:ring-sky-700 aria-[invalid=true]:border-red-700'

export const CLASES_BOTON_PRIMARIO =
  'rounded-md bg-sky-800 px-4 py-2 font-semibold text-white hover:bg-sky-900 focus:outline-none focus-visible:ring-2 focus-visible:ring-sky-700 focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:bg-slate-500'

/** Botón chico de acción dentro de una fila de tabla. */
export const CLASES_ACCION =
  'rounded-md border border-slate-400 px-2 py-1 text-sm font-medium text-slate-900 hover:bg-slate-100 focus:outline-none focus-visible:ring-2 focus-visible:ring-sky-700'

export const CLASES_BOTON_SECUNDARIO =
  'rounded-md border border-slate-500 px-4 py-2 font-medium text-slate-900 hover:bg-slate-100 focus:outline-none focus-visible:ring-2 focus-visible:ring-sky-700'

/** Atributos ARIA de un control según tenga o no error. */
export function ariaDeError(id: string, error: string | undefined) {
  return {
    'aria-invalid': error !== undefined ? ('true' as const) : ('false' as const),
    'aria-describedby': error !== undefined ? `${id}-error` : undefined,
  }
}
