import type { TurnoDeAgenda } from './api'
import { fechaLarga } from './fechas'
import { TarjetaDeTurno } from './TarjetaDeTurno'

/** Turnos de un día en orden cronológico (el backend ya los devuelve ordenados). */
export function VistaDiaria({ fecha, turnos }: { fecha: string; turnos: TurnoDeAgenda[] }) {
  if (turnos.length === 0) {
    return <p className="rounded-md border border-dashed border-slate-400 p-6 text-slate-700">No hay turnos para este día</p>
  }
  return (
    <ol aria-label={`Turnos del ${fechaLarga(fecha)}`} className="grid gap-2 md:grid-cols-2">
      {turnos.map((turno) => (
        <TarjetaDeTurno key={turno.id} turno={turno} />
      ))}
    </ol>
  )
}
