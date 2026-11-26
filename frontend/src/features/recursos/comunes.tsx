import type { ReactNode } from 'react'
import { Dialogo } from '../../components/formulario/Dialogo'
import { CLASES_BOTON_PRIMARIO, CLASES_BOTON_SECUNDARIO } from '../../components/formulario/estilos'

export function Avisos({ aviso, error }: { aviso: string; error: string | null }) {
  return (
    <>
      <p role="status" aria-live="polite" className="min-h-6 text-sm font-medium text-green-800">
        {aviso}
      </p>
      {error !== null && <MensajeDeError>{error}</MensajeDeError>}
    </>
  )
}

export function MensajeDeError({ children }: { children: ReactNode }) {
  return (
    <p role="alert" className="rounded-md border border-red-700 bg-red-50 p-3 text-sm text-red-800">
      {children}
    </p>
  )
}

/** Título de la pestaña y, para el administrador, el botón de alta. */
export function EncabezadoDeTab({ titulo, alta }: { titulo: string; alta?: { texto: string; alPulsar: () => void } }) {
  return (
    <div className="flex flex-wrap items-center justify-between gap-3">
      <h2 className="text-xl font-bold text-slate-900">{titulo}</h2>
      {alta !== undefined && (
        <button type="button" onClick={alta.alPulsar} className={CLASES_BOTON_PRIMARIO}>
          {alta.texto}
        </button>
      )}
    </div>
  )
}

/** Estado en texto (no solo color), en masculino o femenino según el recurso. */
export function Estado({ activo, femenino = false }: { activo: boolean; femenino?: boolean }) {
  const texto = activo ? (femenino ? 'Activa' : 'Activo') : femenino ? 'Inactiva' : 'Inactivo'
  return <span className={activo ? 'text-green-800' : 'text-slate-700'}>{texto}</span>
}

interface ConfirmarDesactivacionProps {
  nombre: string
  pendiente: boolean
  alConfirmar: () => void
  alCerrar: () => void
}

/** Confirmación de la baja (spec "Desactivación con confirmación en el panel"): se conserva, no se borra. */
export function ConfirmarDesactivacion({ nombre, pendiente, alConfirmar, alCerrar }: ConfirmarDesactivacionProps) {
  return (
    <Dialogo titulo={`¿Desactivar ${nombre}?`} alCerrar={alCerrar}>
      <p className="text-slate-700">
        {nombre} se conserva con todos sus datos, pero deja de ofrecerse para turnos nuevos. Podés reactivarlo
        cuando quieras.
      </p>
      <div className="flex flex-wrap justify-end gap-2">
        <button type="button" onClick={alCerrar} className={CLASES_BOTON_SECUNDARIO}>
          Cancelar
        </button>
        <button type="button" disabled={pendiente} onClick={alConfirmar} className={CLASES_BOTON_PRIMARIO}>
          Desactivar
        </button>
      </div>
    </Dialogo>
  )
}
