import { CLASES_BOTON_SECUNDARIO } from '../../components/formulario/estilos'
import { ContactoDelConsultorio } from './ContactoDelConsultorio'
import { CLASES_ALERTA, CLASES_OPCION } from './estilosReserva'
import { usePrestacionesPublicas, type PrestacionPublica } from './reservaApi'
import { TituloDePaso } from './TituloDePaso'

interface PasoPrestacionProps {
  elegida: PrestacionPublica | null
  alElegir: (prestacion: PrestacionPublica) => void
}

/** Paso 1: la prestación (US-010). La duración se muestra porque define el largo del turno (RN-TU-03). */
export function PasoPrestacion({ elegida, alElegir }: PasoPrestacionProps) {
  const prestaciones = usePrestacionesPublicas()

  return (
    <section className="flex flex-col gap-3">
      <TituloDePaso>Elegí la prestación</TituloDePaso>
      {prestaciones.isPending && <p>Cargando las prestaciones…</p>}
      {prestaciones.isError && (
        <div role="alert" className={CLASES_ALERTA}>
          <p>No pudimos cargar las prestaciones.</p>
          <button type="button" className={`${CLASES_BOTON_SECUNDARIO} mt-2`} onClick={() => void prestaciones.refetch()}>
            Reintentar
          </button>
        </div>
      )}
      {prestaciones.isSuccess && prestaciones.data.length === 0 && (
        <div className="flex flex-col gap-1">
          <p>Por ahora no hay prestaciones para reservar online.</p>
          <ContactoDelConsultorio />
        </div>
      )}
      {prestaciones.isSuccess && (
        <ul className="flex flex-col gap-2">
          {prestaciones.data.map((prestacion) => (
            <li key={prestacion.id}>
              <button
                type="button"
                className={CLASES_OPCION}
                aria-pressed={elegida?.id === prestacion.id}
                onClick={() => alElegir(prestacion)}
              >
                {prestacion.nombre} ({prestacion.duracionMinutos} min)
              </button>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
