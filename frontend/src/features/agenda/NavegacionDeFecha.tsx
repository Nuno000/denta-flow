import { CLASES_BOTON_SECUNDARIO } from '../../components/formulario/estilos'
import { fechaLarga, sumarDias, tituloDeSemana, type Vista } from './fechas'

interface NavegacionDeFechaProps {
  vista: Vista
  fecha: string
  hoy: string
  alCambiar: (cambio: { vista?: Vista; fecha?: string }) => void
}

const CLASES_SELECTOR =
  'px-4 py-2 font-medium focus:outline-none focus-visible:ring-2 focus-visible:ring-sky-700 aria-pressed:bg-sky-800 aria-pressed:text-white'

/** Fecha mostrada (como encabezado), anterior / Hoy / siguiente y el selector Día / Semana. */
export function NavegacionDeFecha({ vista, fecha, hoy, alCambiar }: NavegacionDeFechaProps) {
  const paso = vista === 'dia' ? 1 : 7
  const unidad = vista === 'dia' ? 'Día' : 'Semana'

  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div role="group" aria-label="Vista" className="inline-flex overflow-hidden rounded-md border border-slate-500">
          {(['dia', 'semana'] as const).map((opcion) => (
            <button
              key={opcion}
              type="button"
              aria-pressed={vista === opcion}
              onClick={() => alCambiar({ vista: opcion })}
              className={`${CLASES_SELECTOR} ${vista === opcion ? '' : 'bg-white text-slate-900 hover:bg-slate-100'}`}
            >
              {opcion === 'dia' ? 'Día' : 'Semana'}
            </button>
          ))}
        </div>
        <div className="flex flex-wrap gap-2">
          <button type="button" onClick={() => alCambiar({ fecha: sumarDias(fecha, -paso) })} className={CLASES_BOTON_SECUNDARIO}>
            {unidad} anterior
          </button>
          <button type="button" onClick={() => alCambiar({ fecha: hoy })} className={CLASES_BOTON_SECUNDARIO}>
            Hoy
          </button>
          <button type="button" onClick={() => alCambiar({ fecha: sumarDias(fecha, paso) })} className={CLASES_BOTON_SECUNDARIO}>
            {unidad} siguiente
          </button>
        </div>
      </div>
      <h2 className="text-xl font-bold text-slate-900 first-letter:uppercase">
        {vista === 'dia' ? fechaLarga(fecha) : tituloDeSemana(fecha)}
      </h2>
    </div>
  )
}
