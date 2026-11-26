import { useState } from 'react'
import { CLASES_BOTON_SECUNDARIO } from '../../components/formulario/estilos'
import { ContactoDelConsultorio } from './ContactoDelConsultorio'
import { CLASES_ALERTA, CLASES_CHIP } from './estilosReserva'
import { horaDe, proximosDias } from './fechasDelConsultorio'
import { useHuecosDelDia, type HuecoPublico } from './reservaApi'
import { TituloDePaso } from './TituloDePaso'

interface PasoHorarioProps {
  prestacionId: number
  profesionalId: number | null
  fecha: string | null
  hueco: HuecoPublico | null
  /** Aviso a mostrar al entrar (p. ej. el horario elegido ya no estaba disponible). */
  aviso: string | null
  alElegirFecha: (fecha: string) => void
  alElegirHueco: (hueco: HuecoPublico) => void
  alVolver: () => void
}

/** Paso 3: día (14 días del consultorio, design Q5) y horario. Solo se ofrecen huecos del motor (RN-DI-01). */
export function PasoHorario(props: PasoHorarioProps) {
  const { prestacionId, profesionalId, fecha, hueco, aviso, alElegirFecha, alElegirHueco, alVolver } = props
  const [dias] = useState(() => proximosDias(new Date()))
  const huecos = useHuecosDelDia(prestacionId, profesionalId, fecha)

  return (
    <section className="flex flex-col gap-3">
      <TituloDePaso>Elegí día y horario</TituloDePaso>
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
            onClick={() => alElegirFecha(dia.fecha)}
          >
            {dia.etiqueta}
          </button>
        ))}
      </div>
      {fecha !== null && huecos.isPending && <p>Buscando horarios…</p>}
      {huecos.isError && (
        <div role="alert" className={CLASES_ALERTA}>
          <p>No pudimos cargar los horarios.</p>
          <button type="button" className={`${CLASES_BOTON_SECUNDARIO} mt-2`} onClick={() => void huecos.refetch()}>
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
            <button
              key={opcion.inicio}
              type="button"
              className={CLASES_CHIP}
              aria-pressed={opcion.inicio === hueco?.inicio}
              onClick={() => alElegirHueco(opcion)}
            >
              {horaDe(opcion.inicio)}
            </button>
          ))}
        </div>
      )}
      <div>
        <button type="button" className={CLASES_BOTON_SECUNDARIO} onClick={alVolver}>
          Volver
        </button>
      </div>
    </section>
  )
}
