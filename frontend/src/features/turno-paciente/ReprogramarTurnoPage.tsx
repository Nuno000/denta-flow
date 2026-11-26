import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { CLASES_BOTON_PRIMARIO, CLASES_BOTON_SECUNDARIO } from '../../components/formulario/estilos'
import { ContactoDelConsultorio } from '../reserva/ContactoDelConsultorio'
import { CLASES_ALERTA, CLASES_CHIP } from '../reserva/estilosReserva'
import { horaDe, proximosDias } from '../reserva/fechasDelConsultorio'
import type { HuecoPublico } from '../reserva/reservaApi'
import { TituloDePaso } from '../reserva/TituloDePaso'
import { motivoDelError, useHuecosReprogramacion, useReprogramarTurno, type TurnoPublico } from './accionesTurnoApi'
import { AvisoFueraDeTermino, PantallaDelTurno } from './PantallaDelTurno'
import { ResumenDelTurno } from './ResumenDelTurno'

/**
 * Reprogramación por el paciente (US-012, C-17 design D4 y Q4): mismo profesional y prestación, nuevo día y
 * horario entre los huecos del motor. Si otro pedido tomó el horario (409), vuelve a la elección con un aviso y
 * los horarios actualizados.
 */
export function ReprogramarTurnoPage() {
  const { token = '' } = useParams()
  const reprogramar = useReprogramarTurno(token)

  const resultado = reprogramar.isSuccess ? (
    <section className="flex flex-col gap-3">
      <TituloDePaso>¡Reprogramaste tu turno!</TituloDePaso>
      <ResumenDelTurno {...reprogramar.data} />
    </section>
  ) : undefined

  return (
    <PantallaDelTurno titulo="Reprogramá tu turno" resultado={resultado}>
      {(turno) =>
        turno.puedeModificar ? (
          <EleccionDeHorario token={token} turno={turno} reprogramar={reprogramar} />
        ) : (
          <section className="flex flex-col gap-3">
            <TituloDePaso>No podés reprogramar online</TituloDePaso>
            <ResumenDelTurno {...turno} />
            <AvisoFueraDeTermino accion="reprogramar" />
          </section>
        )
      }
    </PantallaDelTurno>
  )
}

interface EleccionDeHorarioProps {
  token: string
  turno: TurnoPublico
  reprogramar: ReturnType<typeof useReprogramarTurno>
}

function EleccionDeHorario({ token, turno, reprogramar }: EleccionDeHorarioProps) {
  const [dias] = useState(() => proximosDias(new Date()))
  const [fecha, setFecha] = useState<string | null>(null)
  const [hueco, setHueco] = useState<HuecoPublico | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)
  const huecos = useHuecosReprogramacion(token, fecha)

  async function confirmar(elegido: HuecoPublico) {
    setAviso(null)
    try {
      await reprogramar.mutateAsync(elegido.inicio)
    } catch (error) {
      setHueco(null)
      setAviso(motivoDelError(error).mensaje)
    }
  }

  if (hueco !== null) {
    return (
      <section className="flex flex-col gap-3">
        <TituloDePaso key="confirmacion">Confirmá el nuevo horario</TituloDePaso>
        <ResumenDelTurno inicio={hueco.inicio} prestacion={turno.prestacion} profesional={turno.profesional} />
        <div className="flex flex-col gap-2 sm:flex-row">
          <button
            type="button"
            className={`${CLASES_BOTON_PRIMARIO} min-h-11`}
            disabled={reprogramar.isPending}
            onClick={() => void confirmar(hueco)}
          >
            Confirmar nuevo horario
          </button>
          <button type="button" className={`${CLASES_BOTON_SECUNDARIO} min-h-11`} onClick={() => setHueco(null)}>
            Elegir otro horario
          </button>
        </div>
      </section>
    )
  }

  return (
    <section className="flex flex-col gap-3">
      <TituloDePaso key="eleccion">Elegí un nuevo día y horario</TituloDePaso>
      <h3 className="text-base font-semibold text-slate-900">Tu turno actual</h3>
      <ResumenDelTurno {...turno} />
      {aviso !== null && (
        <p role="alert" className={CLASES_ALERTA}>
          {aviso}
        </p>
      )}
      <div role="group" aria-label="Días" className="grid grid-cols-4 gap-2 sm:grid-cols-7">
        {dias.map((dia) => (
          <button
            key={dia.fecha}
            type="button"
            className={CLASES_CHIP}
            aria-pressed={dia.fecha === fecha}
            onClick={() => setFecha(dia.fecha)}
          >
            {dia.etiqueta}
          </button>
        ))}
      </div>
      {fecha !== null && huecos.isPending && <p role="status">Buscando horarios…</p>}
      {huecos.isError && (
        <div role="alert" className={CLASES_ALERTA}>
          <p>{motivoDelError(huecos.error).mensaje}</p>
          <button type="button" className={`${CLASES_BOTON_SECUNDARIO} mt-2 min-h-11`} onClick={() => void huecos.refetch()}>
            Reintentar
          </button>
        </div>
      )}
      {huecos.isSuccess && huecos.data.length === 0 && (
        <div className="flex flex-col gap-1">
          <p>No hay horarios disponibles para ese día. Probá con otro día o comunicate con el consultorio:</p>
          <ContactoDelConsultorio />
        </div>
      )}
      {huecos.isSuccess && huecos.data.length > 0 && (
        <div role="group" aria-label="Horarios" className="grid grid-cols-4 gap-2 sm:grid-cols-6">
          {huecos.data.map((opcion) => (
            <button key={opcion.inicio} type="button" className={CLASES_CHIP} onClick={() => setHueco(opcion)}>
              {horaDe(opcion.inicio)}
            </button>
          ))}
        </div>
      )}
    </section>
  )
}
