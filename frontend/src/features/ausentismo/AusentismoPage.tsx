import { Link, useSearchParams } from 'react-router-dom'
import { CLASES_BOTON_SECUNDARIO, CLASES_CONTROL } from '../../components/formulario/estilos'
import { Celda } from '../../components/tabla/Celda'
import { ErrorApi } from '../../lib/apiClient'
import { hoyEnConsultorio } from '../agenda/fechas'
import { BarraDelPanel } from '../auth/BarraDelPanel'
import { useSesion } from '../auth/contextoSesion'
import { useProfesionales } from '../recursos/api'
import {
  useReporteDeAusentismo,
  type AusentismoPorPaciente,
  type AusentismoPorProfesional,
  type ReporteDeAusentismo,
} from './api'
import { BarraDeTasa } from './BarraDeTasa'
import {
  OPCIONES_MINIMO_AUSENCIAS,
  formatearPorcentaje,
  normalizarParametrosAusentismo,
  parametrosDeAusentismo,
  rangoUltimosDias,
  validarRangoAusentismo,
  type FiltrosAusentismo,
} from './parametros'

const ATAJOS_DE_PERIODO = [
  { dias: 7, etiqueta: 'Últimos 7 días' },
  { dias: 30, etiqueta: 'Últimos 30 días' },
  { dias: 90, etiqueta: 'Últimos 90 días' },
] as const

function idDeOpcion(valor: string): number | undefined {
  return valor === '' ? undefined : Number(valor)
}

/**
 * Panel de ausentismo (C-23, US-031; spec panel-ausentismo). Muestra métricas agregadas a partir de los estados de
 * asistencia registrados. Administrador y recepción ven todo el consultorio y pueden filtrar por profesional; el
 * rol profesional ve únicamente sus propios indicadores (verificado también por el backend).
 */
export function AusentismoPage() {
  const { usuario } = useSesion()
  const [params, setParams] = useSearchParams()
  const hoy = hoyEnConsultorio()
  const filtrosUrl = normalizarParametrosAusentismo(params, hoy)
  const esProfesional = usuario?.rol === 'profesional'

  const filtrosDeConsulta: FiltrosAusentismo = esProfesional
    ? { ...filtrosUrl, profesionalId: undefined }
    : filtrosUrl

  const errorDeRango = validarRangoAusentismo(filtrosDeConsulta.desde, filtrosDeConsulta.hasta)
  const profesionales = useProfesionales()
  const consulta = useReporteDeAusentismo(filtrosDeConsulta, errorDeRango === null)

  function actualizar(parcial: Partial<FiltrosAusentismo>) {
    const siguiente: FiltrosAusentismo = { ...filtrosDeConsulta, ...parcial }
    setParams(parametrosDeAusentismo(siguiente))
  }

  const anuncio =
    consulta.data !== undefined && !consulta.isPlaceholderData && errorDeRango === null
      ? `Tasa de ausentismo ${formatearPorcentaje(consulta.data.general.tasaAusentismo)} sobre ${consulta.data.general.totalEvaluados} turnos evaluados.`
      : ''

  return (
    <>
      <BarraDelPanel />
      <main className="mx-auto flex max-w-7xl flex-col gap-6 p-4 sm:p-6">
        <div className="flex flex-col gap-1">
          <h1 className="text-2xl font-bold text-slate-900">
            {esProfesional ? 'Mi ausentismo' : 'Panel de ausentismo'}
          </h1>
          <p className="text-sm text-slate-700">
            Indicadores calculados sobre turnos con asistencia registrada (asistió, ausente y ausente con aviso) en hora
            del consultorio.
          </p>
        </div>

        <section aria-label="Filtros de ausentismo" className="flex flex-col gap-4 rounded-lg border border-slate-300 bg-white p-4">
          <div className="flex flex-wrap gap-2">
            {ATAJOS_DE_PERIODO.map((atajo) => {
              const rango = rangoUltimosDias(hoy, atajo.dias)
              const activo = filtrosDeConsulta.desde === rango.desde && filtrosDeConsulta.hasta === rango.hasta
              return (
                <button
                  key={atajo.dias}
                  type="button"
                  aria-pressed={activo}
                  onClick={() => actualizar(rango)}
                  className={
                    activo
                      ? 'rounded-md bg-sky-800 px-3 py-1.5 text-sm font-semibold text-white focus:outline-none focus-visible:ring-2 focus-visible:ring-sky-700'
                      : 'rounded-md border border-slate-400 bg-white px-3 py-1.5 text-sm font-medium text-slate-900 hover:bg-slate-100 focus:outline-none focus-visible:ring-2 focus-visible:ring-sky-700'
                  }
                >
                  {atajo.etiqueta}
                </button>
              )
            })}
          </div>

          <form
            aria-label="Selector de período y filtros"
            onSubmit={(evento) => evento.preventDefault()}
            className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4"
          >
            <label className="flex flex-col gap-1 text-sm font-medium text-slate-900">
              Desde
              <input
                type="date"
                value={filtrosDeConsulta.desde}
                onChange={(evento) => actualizar({ desde: evento.target.value })}
                className={CLASES_CONTROL}
              />
            </label>
            <label className="flex flex-col gap-1 text-sm font-medium text-slate-900">
              Hasta
              <input
                type="date"
                value={filtrosDeConsulta.hasta}
                onChange={(evento) => actualizar({ hasta: evento.target.value })}
                className={CLASES_CONTROL}
              />
            </label>
            {!esProfesional && (
              <label className="flex flex-col gap-1 text-sm font-medium text-slate-900">
                Profesional
                <select
                  value={filtrosDeConsulta.profesionalId ?? ''}
                  onChange={(evento) => actualizar({ profesionalId: idDeOpcion(evento.target.value) })}
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
              Ausencias reiteradas
              <select
                value={filtrosDeConsulta.minimoAusencias}
                onChange={(evento) => actualizar({ minimoAusencias: Number(evento.target.value) })}
                className={CLASES_CONTROL}
              >
                {OPCIONES_MINIMO_AUSENCIAS.map((opcion) => (
                  <option key={opcion} value={opcion}>
                    {opcion === 1 ? '1 o más ausencias' : `${opcion} o más ausencias`}
                  </option>
                ))}
              </select>
            </label>
          </form>

          {errorDeRango !== null && (
            <div role="alert" className="rounded-md border border-red-700 bg-red-50 px-3 py-2 text-sm text-red-800">
              {errorDeRango}
            </div>
          )}
        </section>

        <p role="status" aria-live="polite" className="sr-only">
          {anuncio}
        </p>

        {errorDeRango === null && (
          <ContenidoAusentismo
            reporte={consulta.data}
            error={consulta.isError ? consulta.error : null}
            esProfesional={esProfesional}
            alReintentar={() => void consulta.refetch()}
          />
        )}
      </main>
    </>
  )
}

interface ContenidoProps {
  reporte: ReporteDeAusentismo | undefined
  error: Error | null
  esProfesional: boolean
  alReintentar: () => void
}

function ContenidoAusentismo({ reporte, error, esProfesional, alReintentar }: ContenidoProps) {
  if (error !== null) {
    const sinPermiso = error instanceof ErrorApi && error.status === 403
    return (
      <div
        role="alert"
        className="flex flex-col items-start gap-3 rounded-md border border-red-700 bg-red-50 p-4 text-red-800"
      >
        <p>
          {sinPermiso
            ? 'No tenés permiso para consultar estos indicadores.'
            : 'No se pudieron cargar los indicadores de ausentismo.'}
        </p>
        {!sinPermiso && (
          <button type="button" onClick={alReintentar} className={CLASES_BOTON_SECUNDARIO}>
            Reintentar
          </button>
        )}
      </div>
    )
  }

  if (reporte === undefined) {
    return <p className="text-slate-700">Cargando indicadores de ausentismo…</p>
  }

  if (reporte.general.totalEvaluados === 0) {
    return (
      <section className="flex flex-col items-start gap-3 rounded-lg border border-slate-300 bg-slate-50 p-6">
        <h2 className="text-lg font-semibold text-slate-900">Sin turnos evaluados en el período</h2>
        <p className="text-sm text-slate-700">
          No hay turnos con asistencia registrada en este período. Registrá asistencia desde la agenda para ver las
          métricas de ausentismo.
        </p>
        <Link to="/panel/agenda" className={CLASES_BOTON_SECUNDARIO}>
          Ir a la agenda
        </Link>
      </section>
    )
  }

  return (
    <div className="flex flex-col gap-6">
      <section aria-label="Resumen general de ausentismo" className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <TarjetaResumen
          titulo="Tasa de ausentismo"
          valor={formatearPorcentaje(reporte.general.tasaAusentismo)}
          detalle={`${reporte.general.totalAusentes} de ${reporte.general.totalEvaluados} evaluados`}
          destacada
        />
        <TarjetaResumen
          titulo="Turnos evaluados"
          valor={String(reporte.general.totalEvaluados)}
          detalle="Con asistencia registrada"
        />
        <TarjetaResumen
          titulo="Asistieron"
          valor={String(reporte.general.asistidos)}
          detalle="Turnos completados"
        />
        <TarjetaResumen
          titulo="Ausentes"
          valor={String(reporte.general.totalAusentes)}
          detalle={`${reporte.general.ausentesSinAviso} sin aviso · ${reporte.general.ausentesConAviso} con aviso`}
        />
      </section>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <section className="flex flex-col gap-4 rounded-lg border border-slate-300 bg-white p-4">
          <h2 className="text-lg font-semibold text-slate-900">Por franja horaria</h2>
          <div className="flex flex-col gap-4">
            {reporte.porFranja.map((franja) => (
              <BarraDeTasa
                key={franja.codigo}
                etiqueta={franja.etiqueta}
                tasa={franja.tasaAusentismo}
                totalAusentes={franja.totalAusentes}
                totalEvaluados={franja.totalEvaluados}
                ausentesSinAviso={franja.ausentesSinAviso}
                ausentesConAviso={franja.ausentesConAviso}
              />
            ))}
          </div>
        </section>

        <section className="flex flex-col gap-4 rounded-lg border border-slate-300 bg-white p-4">
          <h2 className="text-lg font-semibold text-slate-900">Por día de la semana</h2>
          {reporte.porDiaSemana.length === 0 ? (
            <p className="text-sm text-slate-700">Sin turnos evaluados por día en este período.</p>
          ) : (
            <div className="flex flex-col gap-4">
              {reporte.porDiaSemana.map((dia) => (
                <BarraDeTasa
                  key={dia.dia}
                  etiqueta={dia.etiqueta}
                  tasa={dia.tasaAusentismo}
                  totalAusentes={dia.totalAusentes}
                  totalEvaluados={dia.totalEvaluados}
                  ausentesSinAviso={dia.ausentesSinAviso}
                  ausentesConAviso={dia.ausentesConAviso}
                />
              ))}
            </div>
          )}
        </section>
      </div>

      {!esProfesional && reporte.porProfesional.length > 0 && (
        <TablaProfesionales filas={reporte.porProfesional} />
      )}

      <TablaPacientesReiterados filas={reporte.pacientesReiterados} minimoAusencias={reporte.minimoAusencias} />
    </div>
  )
}

function TarjetaResumen({
  titulo,
  valor,
  detalle,
  destacada = false,
}: {
  titulo: string
  valor: string
  detalle: string
  destacada?: boolean
}) {
  return (
    <article
      className={
        destacada
          ? 'flex flex-col gap-1 rounded-lg border border-sky-800 bg-sky-50 p-4'
          : 'flex flex-col gap-1 rounded-lg border border-slate-300 bg-white p-4'
      }
    >
      <p className="text-sm font-medium text-slate-700">{titulo}</p>
      <p className="text-2xl font-bold tabular-nums text-slate-900">{valor}</p>
      <p className="text-xs text-slate-700">{detalle}</p>
    </article>
  )
}

function TablaProfesionales({ filas }: { filas: AusentismoPorProfesional[] }) {
  return (
    <section className="flex flex-col gap-3 rounded-lg border border-slate-300 bg-white p-4">
      <h2 className="text-lg font-semibold text-slate-900">Por profesional</h2>
      <div className="overflow-x-auto">
        <table aria-label="Ausentismo por profesional" className="w-full border-collapse text-left text-sm">
          <thead className="hidden border-b border-slate-300 text-slate-700 md:table-header-group">
            <tr>
              <th scope="col" className="px-3 py-2 font-semibold">Profesional</th>
              <th scope="col" className="px-3 py-2 font-semibold">Evaluados</th>
              <th scope="col" className="px-3 py-2 font-semibold">Asistieron</th>
              <th scope="col" className="px-3 py-2 font-semibold">Sin aviso</th>
              <th scope="col" className="px-3 py-2 font-semibold">Con aviso</th>
              <th scope="col" className="px-3 py-2 font-semibold">Tasa</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-200">
            {filas.map((fila) => (
              <tr key={fila.profesionalId} className="flex flex-col py-2 md:table-row md:py-0">
                <Celda etiqueta="Profesional">
                  <span className="font-medium text-slate-900">
                    {fila.nombre} {fila.apellido}
                  </span>
                </Celda>
                <Celda etiqueta="Evaluados">{fila.totalEvaluados}</Celda>
                <Celda etiqueta="Asistieron">{fila.asistidos}</Celda>
                <Celda etiqueta="Sin aviso">{fila.ausentesSinAviso}</Celda>
                <Celda etiqueta="Con aviso">{fila.ausentesConAviso}</Celda>
                <Celda etiqueta="Tasa">
                  <span className="font-semibold tabular-nums text-slate-900">
                    {formatearPorcentaje(fila.tasaAusentismo)}
                  </span>
                </Celda>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  )
}

function TablaPacientesReiterados({
  filas,
  minimoAusencias,
}: {
  filas: AusentismoPorPaciente[]
  minimoAusencias: number
}) {
  return (
    <section className="flex flex-col gap-3 rounded-lg border border-slate-300 bg-white p-4">
      <div className="flex flex-col gap-0.5">
        <h2 className="text-lg font-semibold text-slate-900">Pacientes con ausencias reiteradas</h2>
        <p className="text-xs text-slate-700">
          Pacientes con {minimoAusencias} o más ausencias en el período. Se muestra únicamente nombre y apellido (Ley
          25.326).
        </p>
      </div>
      {filas.length === 0 ? (
        <p className="text-sm text-slate-700">
          Ningún paciente acumula {minimoAusencias} o más ausencias en este período.
        </p>
      ) : (
        <div className="overflow-x-auto">
          <table aria-label="Pacientes con ausencias reiteradas" className="w-full border-collapse text-left text-sm">
            <thead className="hidden border-b border-slate-300 text-slate-700 md:table-header-group">
              <tr>
                <th scope="col" className="px-3 py-2 font-semibold">Paciente</th>
                <th scope="col" className="px-3 py-2 font-semibold">Sin aviso</th>
                <th scope="col" className="px-3 py-2 font-semibold">Con aviso</th>
                <th scope="col" className="px-3 py-2 font-semibold">Total ausentes</th>
                <th scope="col" className="px-3 py-2 font-semibold">Evaluados</th>
                <th scope="col" className="px-3 py-2 font-semibold">Tasa</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200">
              {filas.map((fila) => (
                <tr key={fila.pacienteId} className="flex flex-col py-2 md:table-row md:py-0">
                  <Celda etiqueta="Paciente">
                    <span className="font-medium text-slate-900">
                      {fila.nombre} {fila.apellido}
                    </span>
                  </Celda>
                  <Celda etiqueta="Sin aviso">{fila.ausentesSinAviso}</Celda>
                  <Celda etiqueta="Con aviso">{fila.ausentesConAviso}</Celda>
                  <Celda etiqueta="Total ausentes">
                    <span className="font-semibold text-red-800">{fila.totalAusentes}</span>
                  </Celda>
                  <Celda etiqueta="Evaluados">{fila.totalEvaluados}</Celda>
                  <Celda etiqueta="Tasa">
                    <span className="font-semibold tabular-nums text-slate-900">
                      {formatearPorcentaje(fila.tasaAusentismo)}
                    </span>
                  </Celda>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}
