import type { TurnoDeAgenda } from './api'
import { diasDeLaSemana, fechaCorta, fechaDeInstante, fechaLarga } from './fechas'
import { TarjetaDeTurno } from './TarjetaDeTurno'

interface VistaSemanalProps {
  fecha: string
  hoy: string
  turnos: TurnoDeAgenda[]
}

/**
 * Semana de lunes a domingo: un bloque por día con sus turnos. En el celular los días se apilan; en
 * pantallas anchas quedan en siete columnas. Cada turno va al día del consultorio en que empieza.
 */
export function VistaSemanal({ fecha, hoy, turnos }: VistaSemanalProps) {
  const porDia = new Map<string, TurnoDeAgenda[]>()
  for (const turno of turnos) {
    const dia = fechaDeInstante(turno.inicio)
    porDia.set(dia, [...(porDia.get(dia) ?? []), turno])
  }

  return (
    <div className="grid grid-cols-1 gap-3 lg:grid-cols-7">
      {diasDeLaSemana(fecha).map((dia) => {
        const esHoy = dia === hoy
        const delDia = porDia.get(dia) ?? []
        const idTitulo = `dia-${dia}`
        return (
          <section
            key={dia}
            aria-labelledby={idTitulo}
            className={`flex min-w-0 flex-col gap-2 rounded-md border p-2 ${esHoy ? 'border-sky-700 bg-sky-50' : 'border-slate-300 bg-slate-50'}`}
          >
            <h3
              id={idTitulo}
              aria-current={esHoy ? 'date' : undefined}
              className="text-sm font-semibold text-slate-900"
            >
              {fechaCorta(dia)}
              {esHoy && (
                <>
                  {' '}
                  <span className="rounded-full bg-sky-800 px-2 py-0.5 text-xs text-white">Hoy</span>
                </>
              )}
            </h3>
            {delDia.length === 0 ? (
              <p className="text-sm text-slate-700">Sin turnos</p>
            ) : (
              <ol aria-label={`Turnos del ${fechaLarga(dia)}`} className="flex flex-col gap-2">
                {delDia.map((turno) => (
                  <TarjetaDeTurno key={turno.id} turno={turno} compacta />
                ))}
              </ol>
            )}
          </section>
        )
      })}
    </div>
  )
}
