import { CLASES_BOTON_SECUNDARIO } from '../../components/formulario/estilos'
import { CLASES_ALERTA, CLASES_OPCION } from './estilosReserva'
import { useProfesionalesDe, type ProfesionalPublico } from './reservaApi'
import { TituloDePaso } from './TituloDePaso'

interface PasoProfesionalProps {
  prestacionId: number
  /** `undefined` = todavía no eligió; `null` = cualquier profesional (PA-05). */
  elegido: ProfesionalPublico | null | undefined
  alElegir: (profesional: ProfesionalPublico | null) => void
  alVolver: () => void
}

/** Paso 2: un profesional que realiza la prestación o "Cualquier profesional", primera opción (PA-05). */
export function PasoProfesional({ prestacionId, elegido, alElegir, alVolver }: PasoProfesionalProps) {
  const profesionales = useProfesionalesDe(prestacionId)

  return (
    <section className="flex flex-col gap-3">
      <TituloDePaso>Elegí el profesional</TituloDePaso>
      <ul className="flex flex-col gap-2">
        <li>
          <button type="button" className={CLASES_OPCION} aria-pressed={elegido === null} onClick={() => alElegir(null)}>
            Cualquier profesional
          </button>
        </li>
        {profesionales.isSuccess &&
          profesionales.data.map((profesional) => (
            <li key={profesional.id}>
              <button
                type="button"
                className={CLASES_OPCION}
                aria-pressed={elegido?.id === profesional.id}
                onClick={() => alElegir(profesional)}
              >
                {profesional.nombre} {profesional.apellido}
              </button>
            </li>
          ))}
      </ul>
      {profesionales.isPending && <p>Cargando los profesionales…</p>}
      {profesionales.isError && (
        <p role="alert" className={CLASES_ALERTA}>
          No pudimos cargar los profesionales. Podés seguir con "Cualquier profesional".
        </p>
      )}
      <div>
        <button type="button" className={CLASES_BOTON_SECUNDARIO} onClick={alVolver}>
          Volver
        </button>
      </div>
    </section>
  )
}
