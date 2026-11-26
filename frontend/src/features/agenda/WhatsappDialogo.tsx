import { Dialogo } from '../../components/formulario/Dialogo'
import { CLASES_BOTON_PRIMARIO, CLASES_BOTON_SECUNDARIO } from '../../components/formulario/estilos'
import { leerErrorDeApi } from '../../lib/erroresApi'
import { useMensajeWhatsapp, type TurnoDeAgenda } from './api'
import { horaEnConsultorio } from './fechas'

interface WhatsappDialogoProps {
  turno: TurnoDeAgenda
  alCerrar: () => void
}

const CLASES_AVISO = 'rounded-md border border-red-700 bg-red-50 p-3 text-sm text-red-800'

/**
 * Mensaje de WhatsApp prellenado de un turno (C-21, US-032; design D6): vista previa y un enlace real a `wa.me` que
 * se abre en otra pestaña. No envía nada: recepción revisa el texto y lo envía desde su WhatsApp (RN-NO-04).
 */
export function WhatsappDialogo({ turno, alCerrar }: WhatsappDialogoProps) {
  const consulta = useMensajeWhatsapp(turno.id)

  return (
    <Dialogo
      titulo={`WhatsApp — ${turno.paciente.nombre} ${turno.paciente.apellido} (${horaEnConsultorio(turno.inicio)})`}
      alCerrar={alCerrar}
    >
      {consulta.isPending && <p className="text-sm text-slate-700">Preparando el mensaje…</p>}
      {consulta.isError && (
        <div role="alert" className={`${CLASES_AVISO} flex flex-col items-start gap-2`}>
          <p>{leerErrorDeApi(consulta.error).general}</p>
          <button type="button" onClick={() => void consulta.refetch()} className={CLASES_BOTON_SECUNDARIO}>
            Reintentar
          </button>
        </div>
      )}
      {consulta.isSuccess && (
        <div className="flex flex-col gap-3">
          <figure className="flex flex-col gap-1">
            <figcaption className="text-sm font-medium text-slate-900">Mensaje</figcaption>
            <p className="rounded-md border border-slate-300 bg-slate-50 p-3 text-sm break-words whitespace-pre-line text-slate-900">
              {consulta.data.mensaje}
            </p>
          </figure>
          {consulta.data.enlace !== null ? (
            <a
              href={consulta.data.enlace}
              target="_blank"
              rel="noopener noreferrer"
              className={`${CLASES_BOTON_PRIMARIO} inline-flex min-h-11 items-center justify-center`}
            >
              Abrir WhatsApp
            </a>
          ) : (
            <p role="alert" className={CLASES_AVISO}>
              El paciente no tiene un teléfono válido para WhatsApp. Podés copiar el mensaje y enviarlo por otro medio.
            </p>
          )}
        </div>
      )}
      <div className="flex justify-end">
        <button type="button" onClick={alCerrar} className={CLASES_BOTON_SECUNDARIO}>
          Cerrar
        </button>
      </div>
    </Dialogo>
  )
}
