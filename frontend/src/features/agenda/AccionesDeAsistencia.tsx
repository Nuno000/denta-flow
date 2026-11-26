import { useState } from 'react'
import { CLASES_ACCION } from '../../components/formulario/estilos'
import { ErrorApi } from '../../lib/apiClient'
import { leerErrorDeApi } from '../../lib/erroresApi'
import {
  ESTADOS_DE_ASISTENCIA,
  NOMBRES_DE_ESTADO,
  useRegistrarAsistencia,
  type EstadoDeAsistencia,
  type TurnoDeAgenda,
} from './api'
import { horaEnConsultorio } from './fechas'
import { useAhora } from './useAhora'

/** "Asistió" y "Ausente" solo desde el inicio; "Ausente con aviso" en cualquier momento (design D2, D4). */
const REQUIERE_INICIO: ReadonlySet<EstadoDeAsistencia> = new Set(['asistio', 'ausente'])

function mensajeDeError(error: unknown): string {
  if (error instanceof ErrorApi && error.status === 403) {
    return 'No tenés permiso para registrar la asistencia de este turno'
  }
  return leerErrorDeApi(error).general
}

/**
 * Acciones de asistencia de un turno (C-13, US-024; spec "Acciones de asistencia en la agenda"). La UI solo
 * refleja las reglas: el backend decide con su propia hora y responde 409 o 403 si no corresponde.
 */
export function AccionesDeAsistencia({ turno }: { turno: TurnoDeAgenda }) {
  const registrar = useRegistrarAsistencia()
  const ahora = useAhora()
  const [aviso, setAviso] = useState('')

  if (turno.estado === 'cancelado') {
    return null
  }

  const empezo = ahora >= Date.parse(turno.inicio)

  function registrarAsistencia(estado: EstadoDeAsistencia) {
    setAviso('')
    registrar.mutate({ turnoId: turno.id, estado }, { onSuccess: () => setAviso('Asistencia registrada') })
  }

  return (
    <div className="flex flex-col gap-1 border-t border-slate-200 pt-2">
      <div role="group" aria-label="Asistencia" className="flex flex-wrap gap-1">
        {ESTADOS_DE_ASISTENCIA.map((estado) => (
          <button
            key={estado}
            type="button"
            aria-pressed={turno.estado === estado}
            disabled={registrar.isPending || (REQUIERE_INICIO.has(estado) && !empezo)}
            onClick={() => registrarAsistencia(estado)}
            className={`${CLASES_ACCION} aria-pressed:border-sky-800 aria-pressed:bg-sky-800 aria-pressed:text-white disabled:cursor-not-allowed disabled:opacity-60`}
          >
            {NOMBRES_DE_ESTADO[estado]}
          </button>
        ))}
      </div>
      {!empezo && <p className="text-xs text-slate-700">Disponible desde las {horaEnConsultorio(turno.inicio)}</p>}
      <p role="status" aria-live="polite" className="text-xs font-medium text-green-800">
        {aviso}
      </p>
      {registrar.isError && (
        <p role="alert" className="rounded-md border border-red-700 bg-red-50 p-2 text-xs text-red-800">
          {mensajeDeError(registrar.error)}
        </p>
      )}
    </div>
  )
}
