import { fechaYHoraDe } from '../reserva/fechasDelConsultorio'

interface ResumenDelTurnoProps {
  inicio: string
  prestacion: string
  profesional: string
}

/** Datos del turno que ve el paciente: fecha y hora, prestación y profesional. Nunca datos personales ni sillón. */
export function ResumenDelTurno({ inicio, prestacion, profesional }: ResumenDelTurnoProps) {
  return (
    <dl className="grid grid-cols-[auto_1fr] gap-x-3 gap-y-1 text-base">
      <dt className="font-semibold">Fecha y hora</dt>
      <dd>{fechaYHoraDe(inicio)} (hora de Argentina)</dd>
      <dt className="font-semibold">Prestación</dt>
      <dd>{prestacion}</dd>
      <dt className="font-semibold">Profesional</dt>
      <dd>{profesional}</dd>
    </dl>
  )
}
