import { formatearPorcentaje } from './parametros'

interface BarraDeTasaProps {
  etiqueta: string
  tasa: number
  totalAusentes: number
  totalEvaluados: number
  ausentesSinAviso: number
  ausentesConAviso: number
}

function colorDeBarra(tasa: number, totalEvaluados: number): string {
  if (totalEvaluados === 0) return 'bg-slate-300'
  if (tasa >= 30) return 'bg-red-700'
  if (tasa >= 15) return 'bg-amber-600'
  return 'bg-emerald-700'
}

function colorDeTexto(tasa: number, totalEvaluados: number): string {
  if (totalEvaluados === 0) return 'text-slate-600'
  if (tasa >= 30) return 'text-red-800'
  if (tasa >= 15) return 'text-amber-900'
  return 'text-emerald-800'
}

/** Barra horizontal accesible para mostrar tasa y conteo de ausentismo por franja o día. */
export function BarraDeTasa({
  etiqueta,
  tasa,
  totalAusentes,
  totalEvaluados,
  ausentesSinAviso,
  ausentesConAviso,
}: BarraDeTasaProps) {
  const ancho = totalEvaluados === 0 ? 0 : Math.min(100, Math.max(0, tasa))

  return (
    <div className="flex flex-col gap-1.5">
      <div className="flex flex-wrap items-baseline justify-between gap-2 text-sm">
        <span className="font-medium text-slate-900">{etiqueta}</span>
        <span className="flex items-baseline gap-2">
          <span className="text-xs text-slate-700">
            {totalAusentes}/{totalEvaluados} ausentes
            {totalAusentes > 0 ? ` (${ausentesSinAviso} s/a · ${ausentesConAviso} c/a)` : ''}
          </span>
          <span className={`font-semibold tabular-nums ${colorDeTexto(tasa, totalEvaluados)}`}>
            {formatearPorcentaje(tasa)}
          </span>
        </span>
      </div>
      <div
        role="progressbar"
        aria-label={`Tasa de ausentismo en ${etiqueta}`}
        aria-valuenow={tasa}
        aria-valuemin={0}
        aria-valuemax={100}
        className="h-2.5 w-full overflow-hidden rounded-full bg-slate-200"
      >
        <div
          className={`h-full rounded-full transition-all ${colorDeBarra(tasa, totalEvaluados)}`}
          style={{ width: `${ancho}%` }}
        />
      </div>
    </div>
  )
}
