import { Link, useParams } from 'react-router-dom'
import { CLASES_BOTON_PRIMARIO, CLASES_BOTON_SECUNDARIO } from '../../components/formulario/estilos'
import { ContactoDelConsultorio } from '../reserva/ContactoDelConsultorio'
import { CLASES_ALERTA } from '../reserva/estilosReserva'
import { TituloDePaso } from '../reserva/TituloDePaso'
import { motivoDelError, useCancelarTurno } from './accionesTurnoApi'
import { AvisoFueraDeTermino, PantallaDelTurno } from './PantallaDelTurno'
import { ResumenDelTurno } from './ResumenDelTurno'

/**
 * Cancelación por el paciente (US-012, RN-CA-01..03): pide confirmar antes de cancelar y, fuera de la anticipación
 * mínima, solo informa cómo contactar al consultorio.
 */
export function CancelarTurnoPage() {
  const { token = '' } = useParams()
  const cancelar = useCancelarTurno(token)

  const resultado = cancelar.isSuccess ? (
    <section className="flex flex-col gap-3">
      <TituloDePaso>Cancelaste tu turno</TituloDePaso>
      <ResumenDelTurno {...cancelar.data} />
      <p>El horario quedó libre. Cuando quieras, podés reservar un turno nuevo.</p>
      <div>
        <Link to="/reservar" className={`${CLASES_BOTON_SECUNDARIO} inline-flex min-h-11 items-center`}>
          Reservar un turno nuevo
        </Link>
      </div>
    </section>
  ) : undefined

  return (
    <PantallaDelTurno titulo="Cancelá tu turno" resultado={resultado}>
      {(turno, tokenDelTurno) =>
        turno.puedeModificar ? (
          <section className="flex flex-col gap-3">
            <TituloDePaso>¿Querés cancelar tu turno?</TituloDePaso>
            <ResumenDelTurno {...turno} />
            <p>Si lo cancelás, el horario queda libre para otra persona.</p>
            {cancelar.isError && (
              <div role="alert" className={`${CLASES_ALERTA} flex flex-col gap-1`}>
                <p>{motivoDelError(cancelar.error).mensaje}</p>
                {motivoDelError(cancelar.error).motivo === 'conflicto' && <ContactoDelConsultorio />}
              </div>
            )}
            <div className="flex flex-col gap-2 sm:flex-row">
              <button
                type="button"
                className={`${CLASES_BOTON_PRIMARIO} min-h-11`}
                disabled={cancelar.isPending}
                onClick={() => cancelar.mutate()}
              >
                Confirmar cancelación
              </button>
              <Link
                to={`/turno/${encodeURIComponent(tokenDelTurno)}/reprogramar`}
                className={`${CLASES_BOTON_SECUNDARIO} inline-flex min-h-11 items-center justify-center`}
              >
                Prefiero reprogramarlo
              </Link>
            </div>
          </section>
        ) : (
          <section className="flex flex-col gap-3">
            <TituloDePaso>No podés cancelar online</TituloDePaso>
            <ResumenDelTurno {...turno} />
            <AvisoFueraDeTermino accion="cancelar" />
          </section>
        )
      }
    </PantallaDelTurno>
  )
}
