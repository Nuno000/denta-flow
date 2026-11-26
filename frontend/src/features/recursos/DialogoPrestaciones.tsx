import { useState } from 'react'
import { Dialogo } from '../../components/formulario/Dialogo'
import { CLASES_BOTON_PRIMARIO, CLASES_BOTON_SECUNDARIO } from '../../components/formulario/estilos'
import { leerErrorDeApi } from '../../lib/erroresApi'
import { useAsignarPrestaciones, type Prestacion, type Profesional } from './api'
import { MensajeDeError } from './comunes'

interface DialogoPrestacionesProps {
  profesional: Profesional
  prestaciones: Prestacion[]
  alTerminar: (mensaje: string) => void
  alCerrar: () => void
}

/**
 * Elección de las prestaciones que realiza un profesional (spec "Asignación de prestaciones desde el
 * panel"): se ofrecen las activas y las inactivas que ya tenía, rotuladas, para poder quitarlas. Guardar
 * reemplaza el conjunto completo (design D5).
 */
export function DialogoPrestaciones({ profesional, prestaciones, alTerminar, alCerrar }: DialogoPrestacionesProps) {
  const asignar = useAsignarPrestaciones()
  const [elegidas, setElegidas] = useState<Set<number>>(() => new Set(profesional.prestacionIds))
  const [error, setError] = useState<string | null>(null)
  const nombreCompleto = `${profesional.nombre} ${profesional.apellido}`
  const ofrecidas = prestaciones.filter(
    (prestacion) => prestacion.activa || profesional.prestacionIds.includes(prestacion.id),
  )

  const alternar = (id: number) => {
    setElegidas((actuales) => {
      const nuevas = new Set(actuales)
      if (nuevas.has(id)) {
        nuevas.delete(id)
      } else {
        nuevas.add(id)
      }
      return nuevas
    })
  }

  const guardar = async () => {
    setError(null)
    try {
      await asignar.mutateAsync({ id: profesional.id, prestacionIds: [...elegidas].sort((a, b) => a - b) })
      alTerminar('Prestaciones actualizadas')
    } catch (causa) {
      setError(leerErrorDeApi(causa).general)
    }
  }

  return (
    <Dialogo titulo={`Prestaciones de ${nombreCompleto}`} alCerrar={alCerrar}>
      {error !== null && <MensajeDeError>{error}</MensajeDeError>}
      <fieldset className="flex flex-col gap-2">
        <legend className="mb-1 text-sm font-medium text-slate-900">Prestaciones que realiza</legend>
        {ofrecidas.length === 0 && <p className="text-sm text-slate-700">No hay prestaciones activas.</p>}
        {ofrecidas.map((prestacion) => (
          <label key={prestacion.id} className="flex items-center gap-2 text-slate-900">
            <input
              type="checkbox"
              checked={elegidas.has(prestacion.id)}
              onChange={() => alternar(prestacion.id)}
              className="h-5 w-5 accent-sky-800 focus:outline-none focus-visible:ring-2 focus-visible:ring-sky-700"
            />
            <span>{prestacion.activa ? prestacion.nombre : `${prestacion.nombre} (inactiva)`}</span>
          </label>
        ))}
      </fieldset>
      <div className="flex flex-wrap justify-end gap-2">
        <button type="button" onClick={alCerrar} className={CLASES_BOTON_SECUNDARIO}>
          Cancelar
        </button>
        <button type="button" disabled={asignar.isPending} onClick={() => void guardar()} className={CLASES_BOTON_PRIMARIO}>
          Guardar prestaciones
        </button>
      </div>
    </Dialogo>
  )
}
