import { fechaYHoraDe } from './fechasDelConsultorio'
import type { ReservaConfirmada } from './reservaApi'
import { TituloDePaso } from './TituloDePaso'

/**
 * Paso 5: el turno quedó reservado y pendiente (PA-10). No muestra a qué email se envió la confirmación: si el
 * DNI ya existía se usa el email registrado (design D7).
 */
export function PasoConfirmacion({ reserva }: { reserva: ReservaConfirmada }) {
  return (
    <section className="flex flex-col gap-3">
      <TituloDePaso>¡Listo! Reservaste tu turno</TituloDePaso>
      <dl className="grid grid-cols-[auto_1fr] gap-x-3 gap-y-1 text-base">
        <dt className="font-semibold">Fecha y hora</dt>
        <dd>{fechaYHoraDe(reserva.inicio)} (hora de Argentina)</dd>
        <dt className="font-semibold">Prestación</dt>
        <dd>{reserva.prestacion}</dd>
        <dt className="font-semibold">Profesional</dt>
        <dd>{reserva.profesional}</dd>
      </dl>
      <p className="text-base text-slate-800">
        Tu turno queda pendiente hasta que lo confirmes desde el email que te enviamos con los enlaces para confirmar,
        cancelar o reprogramar. Si no lo ves, revisá la carpeta de correo no deseado.
      </p>
    </section>
  )
}
