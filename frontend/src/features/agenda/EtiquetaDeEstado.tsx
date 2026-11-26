import { NOMBRES_DE_ESTADO, type EstadoTurno } from './api'

const BASE = 'inline-flex items-center rounded-full border px-2 py-0.5 text-xs font-semibold'

/** El color solo refuerza el texto; cada par fondo/texto supera el contraste AA (4.5:1). */
const CLASES_DE_ESTADO: Record<EstadoTurno, string> = {
  pendiente: 'border-amber-700 bg-amber-50 text-amber-900',
  confirmado: 'border-sky-700 bg-sky-50 text-sky-900',
  asistio: 'border-green-700 bg-green-50 text-green-900',
  ausente: 'border-red-700 bg-red-50 text-red-900',
  ausente_con_aviso: 'border-orange-700 bg-orange-50 text-orange-900',
  cancelado: 'border-slate-500 bg-slate-100 text-slate-800 line-through',
}

/** Estado del turno en texto (spec "Estado y sobreturno visibles sin depender del color"). */
export function EtiquetaDeEstado({ estado }: { estado: EstadoTurno }) {
  return <span className={`${BASE} ${CLASES_DE_ESTADO[estado]}`}>{NOMBRES_DE_ESTADO[estado]}</span>
}

export function EtiquetaDeSobreturno() {
  return <span className={`${BASE} border-violet-700 bg-violet-50 text-violet-900`}>Sobreturno</span>
}
