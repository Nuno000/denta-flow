import type { UseFormRegisterReturn } from 'react-hook-form'
import { CLASES_BOTON_SECUNDARIO, ariaDeError } from '../../components/formulario/estilos'
import { useAvisoPrivacidad } from './avisoPrivacidadApi'

interface AvisoPrivacidadProps {
  /** Resultado de `register('aceptaAvisoPrivacidad')` del formulario de reserva. */
  registro: UseFormRegisterReturn
  error?: string
}

const ID = 'acepta-aviso-privacidad'

/**
 * Aviso de privacidad con casilla de aceptación obligatoria para la reserva (US-040, RN-PR-01, C-15
 * design D9). El texto va plegado para no tapar el formulario en el celular; la leyenda es la etiqueta
 * de la casilla y el error queda asociado con `aria-describedby`.
 */
export function AvisoPrivacidad({ registro, error }: AvisoPrivacidadProps) {
  const aviso = useAvisoPrivacidad()

  if (aviso.isPending) {
    return <p className="text-sm text-slate-700">Cargando el aviso de privacidad…</p>
  }

  if (aviso.isError) {
    return (
      <div role="alert" className="flex flex-col items-start gap-2 rounded-md border border-red-700 p-3 text-sm text-red-800">
        <p>No pudimos cargar el aviso de privacidad. Sin aceptarlo no se puede reservar.</p>
        <button type="button" className={CLASES_BOTON_SECUNDARIO} onClick={() => void aviso.refetch()}>
          Reintentar
        </button>
      </div>
    )
  }

  return (
    <fieldset className="flex flex-col gap-2 rounded-md border border-slate-300 p-3">
      <legend className="px-1 text-sm font-semibold text-slate-900">Aviso de privacidad</legend>
      <details className="text-sm text-slate-800">
        <summary className="cursor-pointer font-medium text-sky-800 underline focus:outline-none focus-visible:ring-2 focus-visible:ring-sky-700">
          Leer el aviso completo
        </summary>
        <p className="mt-2 whitespace-pre-line">{aviso.data.texto}</p>
      </details>
      <div className="flex items-start gap-2">
        <input
          id={ID}
          type="checkbox"
          className="mt-1 h-5 w-5 shrink-0 accent-sky-800 focus:outline-none focus-visible:ring-2 focus-visible:ring-sky-700"
          {...ariaDeError(ID, error)}
          {...registro}
        />
        <label htmlFor={ID} className="text-base text-slate-900">
          {aviso.data.leyenda}
        </label>
      </div>
      {error !== undefined && (
        <p id={`${ID}-error`} className="text-sm text-red-800">
          {error}
        </p>
      )}
    </fieldset>
  )
}
