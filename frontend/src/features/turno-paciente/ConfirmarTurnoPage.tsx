import { CLASES_BOTON_PRIMARIO } from '../../components/formulario/estilos'
import { CLASES_ALERTA } from '../reserva/estilosReserva'
import { TituloDePaso } from '../reserva/TituloDePaso'
import { useParams } from 'react-router-dom'
import { motivoDelError, useConfirmarTurno } from './accionesTurnoApi'
import { PantallaDelTurno } from './PantallaDelTurno'
import { ResumenDelTurno } from './ResumenDelTurno'

/**
 * Confirmación de asistencia (US-011, C-17 design Q1 opción B): abrir el enlace solo muestra el turno; confirmar
 * requiere tocar el botón, así los previsualizadores de email no confirman por el paciente.
 */
export function ConfirmarTurnoPage() {
  const { token = '' } = useParams()
  const confirmar = useConfirmarTurno(token)

  const resultado = confirmar.isSuccess ? (
    <section className="flex flex-col gap-3">
      <TituloDePaso>¡Turno confirmado!</TituloDePaso>
      <ResumenDelTurno {...confirmar.data} />
      <p>Te esperamos. Si no podés ir, cancelá o reprogramá desde el email con anticipación.</p>
    </section>
  ) : undefined

  return (
    <PantallaDelTurno titulo="Confirmá tu turno" resultado={resultado}>
      {(turno) =>
        turno.estado === 'confirmado' ? (
          <section className="flex flex-col gap-3">
            <TituloDePaso>Tu turno ya está confirmado</TituloDePaso>
            <ResumenDelTurno {...turno} />
          </section>
        ) : (
          <section className="flex flex-col gap-3">
            <TituloDePaso>Revisá los datos de tu turno</TituloDePaso>
            <ResumenDelTurno {...turno} />
            {confirmar.isError && (
              <p role="alert" className={CLASES_ALERTA}>
                {motivoDelError(confirmar.error).mensaje}
              </p>
            )}
            <div>
              <button
                type="button"
                className={`${CLASES_BOTON_PRIMARIO} min-h-11 w-full sm:w-auto`}
                disabled={confirmar.isPending}
                onClick={() => confirmar.mutate()}
              >
                Confirmar mi turno
              </button>
            </div>
          </section>
        )
      }
    </PantallaDelTurno>
  )
}
