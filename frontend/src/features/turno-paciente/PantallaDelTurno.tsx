import type { ReactNode } from 'react'
import { Link, useParams } from 'react-router-dom'
import { CLASES_BOTON_SECUNDARIO } from '../../components/formulario/estilos'
import { ContactoDelConsultorio } from '../reserva/ContactoDelConsultorio'
import { CLASES_ALERTA } from '../reserva/estilosReserva'
import { TituloDePaso } from '../reserva/TituloDePaso'
import { motivoDelError, useTurnoPublico, type TurnoPublico } from './accionesTurnoApi'
import { ResumenDelTurno } from './ResumenDelTurno'

interface PantallaDelTurnoProps {
  titulo: string
  /** Contenido cuando el turno está vigente. */
  children: (turno: TurnoPublico, token: string) => ReactNode
  /** Contenido que reemplaza al resto (p. ej. el éxito de la acción), aunque el turno ya no esté vigente. */
  resultado?: ReactNode
}

/**
 * Marco común de `/turno/:token/*` (C-17 design D6): mobile-first, con `h1` fijo y un `h2` que toma el foco en
 * cada cambio. Resuelve el token y muestra los casos de enlace inválido, turno cancelado y turno vencido.
 */
export function PantallaDelTurno({ titulo, children, resultado }: PantallaDelTurnoProps) {
  const { token = '' } = useParams()
  const turno = useTurnoPublico(token)

  return (
    <main className="mx-auto flex min-h-screen w-full max-w-xl flex-col gap-4 p-4 sm:p-6">
      <h1 className="text-2xl font-bold text-slate-900">{titulo}</h1>
      {resultado ?? <Contenido turno={turno} token={token} contenido={children} />}
    </main>
  )
}

function Contenido({
  turno,
  token,
  contenido,
}: {
  turno: ReturnType<typeof useTurnoPublico>
  token: string
  contenido: PantallaDelTurnoProps['children']
}) {
  if (turno.isPending) {
    return <p role="status">Cargando tu turno…</p>
  }
  if (turno.isError) {
    if (motivoDelError(turno.error).motivo === 'no-encontrado') {
      return (
        <section className="flex flex-col gap-3">
          <TituloDePaso>El enlace no es válido</TituloDePaso>
          <p>El enlace no es válido o ya venció. Si necesitás ayuda con tu turno, comunicate con el consultorio:</p>
          <ContactoDelConsultorio />
        </section>
      )
    }
    return (
      <div role="alert" className={CLASES_ALERTA}>
        <p>No pudimos cargar tu turno.</p>
        <button type="button" className={`${CLASES_BOTON_SECUNDARIO} mt-2 min-h-11`} onClick={() => void turno.refetch()}>
          Reintentar
        </button>
      </div>
    )
  }
  const datos = turno.data
  if (datos.situacion !== 'VIGENTE') {
    return (
      <section className="flex flex-col gap-3">
        <TituloDePaso>{datos.situacion === 'CANCELADO' ? 'Este turno fue cancelado' : 'Este turno ya pasó'}</TituloDePaso>
        <ResumenDelTurno {...datos} />
        <p>Ya no se puede confirmar, cancelar ni reprogramar. Si necesitás atenderte, reservá un turno nuevo o comunicate con el consultorio:</p>
        <ContactoDelConsultorio />
        <div>
          <Link to="/reservar" className={`${CLASES_BOTON_SECUNDARIO} inline-flex min-h-11 items-center`}>
            Reservar un turno nuevo
          </Link>
        </div>
      </section>
    )
  }
  return <>{contenido(datos, token)}</>
}

/** Aviso de plazo vencido para cancelar o reprogramar online (RN-CA-03, design Q3) con el contacto del consultorio. */
export function AvisoFueraDeTermino({ accion }: { accion: 'cancelar' | 'reprogramar' }) {
  return (
    <div role="alert" className={`${CLASES_ALERTA} flex flex-col gap-1`}>
      <p>El plazo para {accion} online ya venció. Comunicate directamente con el consultorio:</p>
      <ContactoDelConsultorio />
    </div>
  )
}
