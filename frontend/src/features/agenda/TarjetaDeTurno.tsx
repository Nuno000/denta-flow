import { useContext, useState } from 'react'
import { CLASES_ACCION } from '../../components/formulario/estilos'
import { ContextoSesion } from '../auth/contextoSesion'
import { AccionesDeAsistencia } from './AccionesDeAsistencia'
import type { TurnoDeAgenda } from './api'
import { EtiquetaDeEstado, EtiquetaDeSobreturno } from './EtiquetaDeEstado'
import { horaEnConsultorio } from './fechas'
import { HistorialDeTurnoDialogo } from './HistorialDeTurnoDialogo'
import { WhatsappDialogo } from './WhatsappDialogo'

interface TarjetaDeTurnoProps {
  turno: TurnoDeAgenda
  /** Para columnas angostas (vista semanal): datos apilados, rótulos solo para lectores de pantalla. */
  compacta?: boolean
}

/** Un turno de la agenda: horario en hora del consultorio, paciente, prestación, profesional, sillón y estado. */
export function TarjetaDeTurno({ turno, compacta = false }: TarjetaDeTurnoProps) {
  const sesion = useContext(ContextoSesion)
  const rol = sesion?.usuario?.rol
  const esAdministrador = rol === 'administrador'
  // C-21 (US-032): recordar o pedir confirmación por WhatsApp solo tiene sentido en turnos vigentes.
  const puedeEnviarWhatsapp =
    (rol === 'administrador' || rol === 'recepcion') && (turno.estado === 'pendiente' || turno.estado === 'confirmado')
  const [viendoHistorial, setViendoHistorial] = useState(false)
  const [viendoWhatsapp, setViendoWhatsapp] = useState(false)
  const cancelado = turno.estado === 'cancelado'
  const claseRotulo = compacta ? 'sr-only' : 'font-medium'
  return (
    <li
      className={`flex flex-col gap-1 rounded-md border border-slate-300 bg-white p-3 ${cancelado ? 'opacity-80' : ''}`}
    >
      <div className="flex flex-wrap items-center justify-between gap-2">
        <span className="font-semibold tabular-nums text-slate-900">
          {horaEnConsultorio(turno.inicio)}–{horaEnConsultorio(turno.fin)}
        </span>
        <span className="flex flex-wrap gap-1">
          <EtiquetaDeEstado estado={turno.estado} />
          {turno.esSobreturno && <EtiquetaDeSobreturno />}
        </span>
      </div>
      <span className="font-medium break-words text-slate-900">
        {turno.paciente.nombre} {turno.paciente.apellido}
      </span>
      <dl
        className={`grid gap-x-2 text-sm break-words text-slate-700 ${compacta ? 'grid-cols-1' : 'grid-cols-[auto_1fr]'}`}
      >
        <dt className={claseRotulo}>Prestación</dt>
        <dd>{turno.prestacion.nombre}</dd>
        <dt className={claseRotulo}>Profesional</dt>
        <dd>
          {turno.profesional.nombre} {turno.profesional.apellido}
        </dd>
        <dt className={claseRotulo}>Sillón</dt>
        <dd>{turno.sillon.nombre}</dd>
      </dl>
      <AccionesDeAsistencia turno={turno} />
      {(puedeEnviarWhatsapp || esAdministrador) && (
        <div className="flex flex-wrap gap-2 pt-1">
          {puedeEnviarWhatsapp && (
            <button type="button" onClick={() => setViendoWhatsapp(true)} className={CLASES_ACCION}>
              WhatsApp
            </button>
          )}
          {esAdministrador && (
            <button type="button" onClick={() => setViendoHistorial(true)} className={CLASES_ACCION}>
              Ver historial
            </button>
          )}
        </div>
      )}
      {viendoHistorial && <HistorialDeTurnoDialogo turno={turno} alCerrar={() => setViendoHistorial(false)} />}
      {viendoWhatsapp && <WhatsappDialogo turno={turno} alCerrar={() => setViendoWhatsapp(false)} />}
    </li>
  )
}
