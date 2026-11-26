import { Dialogo } from '../../components/formulario/Dialogo'
import { CLASES_BOTON_SECUNDARIO } from '../../components/formulario/estilos'
import {
  NOMBRES_DE_ESTADO,
  esEstadoTurno,
  useHistorialDeTurno,
  type AccionHistorial,
  type CambioDeTurno,
  type TurnoDeAgenda,
} from './api'
import { ZONA_CONSULTORIO, horaEnConsultorio } from './fechas'

const fechaHoraConsultorio = new Intl.DateTimeFormat('es-AR', {
  timeZone: ZONA_CONSULTORIO,
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
})

const NOMBRES_DE_ACCION: Record<AccionHistorial, string> = {
  creacion: 'Creación',
  confirmacion: 'Confirmación',
  cancelacion: 'Cancelación',
  reprogramacion: 'Reprogramación',
  cambio_estado: 'Cambio de estado',
}

const NOMBRES_DE_CAMPO: Record<string, string> = {
  estado: 'Estado',
  inicio: 'Inicio',
  fin: 'Fin',
  origen: 'Origen',
  es_sobreturno: 'Sobreturno',
  profesional_id: 'Profesional (ID)',
  sillon_id: 'Sillón (ID)',
  prestacion_id: 'Prestación (ID)',
  paciente_id: 'Paciente (ID)',
}

const ISO_INSTANTE = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}/

function formatearFechaHora(iso: string): string {
  return fechaHoraConsultorio.format(new Date(iso))
}

function nombreDelActor(cambio: CambioDeTurno): string {
  if (cambio.actorTipo === 'usuario') {
    return cambio.actorEmail ?? 'Usuario del consultorio'
  }
  return cambio.actorTipo === 'paciente' ? 'Paciente' : 'Sistema'
}

function formatearValor(campo: string, valor: string): string {
  if (campo === 'estado' && esEstadoTurno(valor)) {
    return NOMBRES_DE_ESTADO[valor]
  }
  if (campo === 'es_sobreturno') {
    return valor === 'true' ? 'Sí' : 'No'
  }
  if (campo === 'origen') {
    return valor === 'online' ? 'Online' : valor === 'recepcion' ? 'Recepción' : valor
  }
  if (ISO_INSTANTE.test(valor)) {
    return formatearFechaHora(valor)
  }
  return valor
}

function detallesDelCambio(cambio: CambioDeTurno): { campo: string; texto: string }[] {
  const claves = Array.from(new Set([...Object.keys(cambio.valorAnterior), ...Object.keys(cambio.valorNuevo)]))
  return claves.map((clave) => {
    const etiqueta = NOMBRES_DE_CAMPO[clave] ?? clave
    const anterior = cambio.valorAnterior[clave]
    const nuevo = cambio.valorNuevo[clave]
    const texto =
      anterior !== undefined && nuevo !== undefined
        ? `${formatearValor(clave, anterior)} → ${formatearValor(clave, nuevo)}`
        : nuevo !== undefined
          ? formatearValor(clave, nuevo)
          : formatearValor(clave, anterior ?? '')
    return { campo: etiqueta, texto }
  })
}

interface HistorialDeTurnoDialogoProps {
  turno: TurnoDeAgenda
  alCerrar: () => void
}

/**
 * Registro de cambios de un turno en solo lectura (C-13, US-025; spec auditoria-turnos).
 * Visible únicamente para el administrador; nunca ofrece acciones de edición ni borrado.
 */
export function HistorialDeTurnoDialogo({ turno, alCerrar }: HistorialDeTurnoDialogoProps) {
  const consulta = useHistorialDeTurno(turno.id)

  return (
    <Dialogo
      titulo={`Historial de cambios — ${turno.paciente.nombre} ${turno.paciente.apellido} (${horaEnConsultorio(turno.inicio)})`}
      alCerrar={alCerrar}
    >
      <div className="max-h-96 overflow-y-auto">
        {consulta.isLoading && <p className="text-sm text-slate-700">Cargando historial…</p>}
        {consulta.isError && (
          <div role="alert" className="flex flex-col items-start gap-2 rounded-md border border-red-700 bg-red-50 p-3 text-sm text-red-800">
            <p>No se pudo cargar el historial del turno.</p>
            <button type="button" onClick={() => void consulta.refetch()} className={CLASES_BOTON_SECUNDARIO}>
              Reintentar
            </button>
          </div>
        )}
        {consulta.data !== undefined && consulta.data.length === 0 && (
          <p className="text-sm text-slate-700">Este turno todavía no tiene cambios registrados.</p>
        )}
        {consulta.data !== undefined && consulta.data.length > 0 && (
          <ol className="flex flex-col gap-3">
            {consulta.data.map((cambio) => (
              <li key={cambio.id} className="rounded-md border border-slate-200 bg-slate-50 p-3 text-sm">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <span className="font-semibold text-slate-900">{NOMBRES_DE_ACCION[cambio.accion]}</span>
                  <time dateTime={cambio.ocurridoEn} className="text-xs tabular-nums text-slate-600">
                    {formatearFechaHora(cambio.ocurridoEn)}
                  </time>
                </div>
                <p className="mt-1 text-xs text-slate-700">
                  Por: <span className="font-medium text-slate-900">{nombreDelActor(cambio)}</span>
                </p>
                <ul className="mt-2 flex flex-col gap-0.5 text-xs text-slate-700">
                  {detallesDelCambio(cambio).map((detalle) => (
                    <li key={detalle.campo}>
                      <span className="font-medium">{detalle.campo}:</span> {detalle.texto}
                    </li>
                  ))}
                </ul>
              </li>
            ))}
          </ol>
        )}
      </div>
      <div className="flex justify-end">
        <button type="button" onClick={alCerrar} className={CLASES_BOTON_SECUNDARIO}>
          Cerrar
        </button>
      </div>
    </Dialogo>
  )
}
