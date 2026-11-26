import { CLASES_BOTON_SECUNDARIO, CLASES_CONTROL } from '../../components/formulario/estilos'
import { useProfesionales, useSillones } from '../recursos/api'
import { ESTADOS_TURNO, NOMBRES_DE_ESTADO, esEstadoTurno, type FiltrosAgenda } from './api'

interface FiltrosDeAgendaProps {
  filtros: FiltrosAgenda
  /** El rol profesional no elige profesional: siempre ve su propia agenda. */
  conProfesional: boolean
  alCambiar: (filtros: FiltrosAgenda) => void
}

function idDeOpcion(valor: string): number | undefined {
  return valor === '' ? undefined : Number(valor)
}

/** Filtros por profesional, sillón y estado (spec "Filtros en la pantalla"). */
export function FiltrosDeAgenda({ filtros, conProfesional, alCambiar }: FiltrosDeAgendaProps) {
  const profesionales = useProfesionales()
  const sillones = useSillones()

  return (
    <form
      aria-label="Filtros de la agenda"
      onSubmit={(evento) => evento.preventDefault()}
      className="flex flex-col gap-3 sm:flex-row sm:flex-wrap sm:items-end"
    >
      {conProfesional && (
        <label className="flex flex-col gap-1 text-sm font-medium text-slate-900">
          Profesional
          <select
            value={filtros.profesionalId ?? ''}
            onChange={(evento) => alCambiar({ ...filtros, profesionalId: idDeOpcion(evento.target.value) })}
            className={CLASES_CONTROL}
          >
            <option value="">Todos</option>
            {(profesionales.data ?? []).map((profesional) => (
              <option key={profesional.id} value={profesional.id}>
                {profesional.nombre} {profesional.apellido}
                {profesional.activo ? '' : ' (inactivo)'}
              </option>
            ))}
          </select>
        </label>
      )}
      <label className="flex flex-col gap-1 text-sm font-medium text-slate-900">
        Sillón
        <select
          value={filtros.sillonId ?? ''}
          onChange={(evento) => alCambiar({ ...filtros, sillonId: idDeOpcion(evento.target.value) })}
          className={CLASES_CONTROL}
        >
          <option value="">Todos</option>
          {(sillones.data ?? []).map((sillon) => (
            <option key={sillon.id} value={sillon.id}>
              {sillon.nombre}
              {sillon.activo ? '' : ' (inactivo)'}
            </option>
          ))}
        </select>
      </label>
      <label className="flex flex-col gap-1 text-sm font-medium text-slate-900">
        Estado
        <select
          value={filtros.estado ?? ''}
          onChange={(evento) => {
            const valor = evento.target.value
            alCambiar({ ...filtros, estado: esEstadoTurno(valor) ? valor : undefined })
          }}
          className={CLASES_CONTROL}
        >
          <option value="">Todos (sin cancelados)</option>
          {ESTADOS_TURNO.map((estado) => (
            <option key={estado} value={estado}>
              {NOMBRES_DE_ESTADO[estado]}
            </option>
          ))}
        </select>
      </label>
      <button type="button" onClick={() => alCambiar({})} className={CLASES_BOTON_SECUNDARIO}>
        Limpiar filtros
      </button>
    </form>
  )
}
