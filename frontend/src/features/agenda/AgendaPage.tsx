import { useSearchParams } from 'react-router-dom'
import { CLASES_BOTON_SECUNDARIO } from '../../components/formulario/estilos'
import { ErrorApi } from '../../lib/apiClient'
import { BarraDelPanel } from '../auth/BarraDelPanel'
import { useSesion } from '../auth/contextoSesion'
import { useTurnosDeAgenda, type FiltrosAgenda, type TurnoDeAgenda } from './api'
import { fechaLarga, hoyEnConsultorio, rangoDeVista, tituloDeSemana, type Vista } from './fechas'
import { FiltrosDeAgenda } from './FiltrosDeAgenda'
import { NavegacionDeFecha } from './NavegacionDeFecha'
import { normalizarParametros, parametrosDeAgenda } from './parametros'
import { VistaDiaria } from './VistaDiaria'
import { VistaSemanal } from './VistaSemanal'

function anuncio(vista: Vista, fecha: string, cantidad: number): string {
  const turnos = cantidad === 1 ? '1 turno' : `${cantidad} turnos`
  if (vista === 'dia') {
    return `${turnos} el ${fechaLarga(fecha)}`
  }
  return `${turnos} en la ${tituloDeSemana(fecha).replace('Semana', 'semana')}`
}

/**
 * Sección Agenda del panel (US-020, US-023; spec agenda-interna). La vista, la fecha y los filtros viven en
 * la URL (design D5). Para el rol profesional no se envía `profesionalId`: el backend lo fuerza (design D2).
 */
export function AgendaPage() {
  const { usuario } = useSesion()
  const [params, setParams] = useSearchParams()
  const hoy = hoyEnConsultorio()
  const { vista, fecha, filtros } = normalizarParametros(params, hoy)
  const esProfesional = usuario?.rol === 'profesional'
  const filtrosDeConsulta: FiltrosAgenda = esProfesional ? { ...filtros, profesionalId: undefined } : filtros
  const consulta = useTurnosDeAgenda(rangoDeVista(vista, fecha), filtrosDeConsulta)

  function cambiar(cambio: { vista?: Vista; fecha?: string; filtros?: FiltrosAgenda }) {
    setParams(parametrosDeAgenda({ vista, fecha, filtros, ...cambio }))
  }

  return (
    <>
      <BarraDelPanel />
      <main className="mx-auto flex max-w-7xl flex-col gap-4 p-4 sm:p-6">
        <h1 className="text-2xl font-bold text-slate-900">{esProfesional ? 'Mi agenda' : 'Agenda'}</h1>
        <NavegacionDeFecha vista={vista} fecha={fecha} hoy={hoy} alCambiar={cambiar} />
        <FiltrosDeAgenda
          filtros={esProfesional ? filtrosDeConsulta : filtros}
          conProfesional={!esProfesional}
          alCambiar={(nuevos) => cambiar({ filtros: nuevos })}
        />
        <p role="status" aria-live="polite" className="sr-only">
          {consulta.data !== undefined && !consulta.isPlaceholderData ? anuncio(vista, fecha, consulta.data.length) : ''}
        </p>
        <Contenido
          vista={vista}
          fecha={fecha}
          hoy={hoy}
          turnos={consulta.data}
          error={consulta.isError ? consulta.error : null}
          alReintentar={() => void consulta.refetch()}
        />
      </main>
    </>
  )
}

interface ContenidoProps {
  vista: Vista
  fecha: string
  hoy: string
  turnos: TurnoDeAgenda[] | undefined
  error: Error | null
  alReintentar: () => void
}

function Contenido({ vista, fecha, hoy, turnos, error, alReintentar }: ContenidoProps) {
  if (error !== null) {
    const sinPermiso = error instanceof ErrorApi && error.status === 403
    return (
      <div role="alert" className="flex flex-col items-start gap-3 rounded-md border border-red-700 bg-red-50 p-4 text-red-800">
        <p>{sinPermiso ? 'No tenés permiso para ver esta agenda.' : 'No se pudo cargar la agenda.'}</p>
        {!sinPermiso && (
          <button type="button" onClick={alReintentar} className={CLASES_BOTON_SECUNDARIO}>
            Reintentar
          </button>
        )}
      </div>
    )
  }
  if (turnos === undefined) {
    return <p className="text-slate-700">Cargando la agenda…</p>
  }
  return vista === 'dia' ? (
    <VistaDiaria fecha={fecha} turnos={turnos} />
  ) : (
    <VistaSemanal fecha={fecha} hoy={hoy} turnos={turnos} />
  )
}
