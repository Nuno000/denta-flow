import { useState, type FormEvent } from 'react'
import { useQuery } from '@tanstack/react-query'
import { apiClient } from '../../lib/apiClient'
import { BarraDelPanel } from '../auth/BarraDelPanel'
import {
  buscarPacientePorDni,
  mensajeErrorListaEspera,
  useCrearListaEspera,
  useDarDeBajaListaEspera,
  useListaEspera,
  type EstadoListaEspera,
  type PacienteEncontrado,
} from './api'

interface OpcionRecurso {
  id: number
  nombre: string
  apellido?: string
  activo?: boolean
  activa?: boolean
}

const ETIQUETAS_ESTADO: Record<EstadoListaEspera, string> = {
  activa: 'Activa',
  ofrecida: 'Ofrecida',
  atendida: 'Atendida',
  vencida: 'Vencida',
}

export function ListaEsperaPage() {
  const [filtroEstado, setFiltroEstado] = useState<EstadoListaEspera | ''>('')
  const consulta = useListaEspera(filtroEstado)
  const crear = useCrearListaEspera()
  const darDeBaja = useDarDeBajaListaEspera()

  const prestaciones = useQuery({
    queryKey: ['prestaciones-lista-espera'],
    queryFn: () => apiClient.get<OpcionRecurso[]>('/api/prestaciones'),
  })
  const profesionales = useQuery({
    queryKey: ['profesionales-lista-espera'],
    queryFn: () => apiClient.get<OpcionRecurso[]>('/api/profesionales'),
  })

  const [dni, setDni] = useState('')
  const [paciente, setPaciente] = useState<PacienteEncontrado | null>(null)
  const [errorDni, setErrorDni] = useState<string | null>(null)
  const [prestacionId, setPrestacionId] = useState('')
  const [profesionalId, setProfesionalId] = useState('')
  const [fechaDesde, setFechaDesde] = useState('')
  const [fechaHasta, setFechaHasta] = useState('')
  const [horaDesde, setHoraDesde] = useState('')
  const [horaHasta, setHoraHasta] = useState('')
  const [errorForm, setErrorForm] = useState<string | null>(null)

  async function manejarBuscarDni() {
    setErrorDni(null)
    setPaciente(null)
    try {
      const encontrado = await buscarPacientePorDni(dni.trim())
      setPaciente(encontrado)
    } catch (err) {
      setErrorDni(mensajeErrorListaEspera(err))
    }
  }

  function manejarGuardar(e: FormEvent) {
    e.preventDefault()
    setErrorForm(null)
    if (!paciente) {
      setErrorForm('Buscá y seleccioná un paciente por DNI antes de guardar.')
      return
    }
    if (!prestacionId || !fechaDesde || !fechaHasta) {
      setErrorForm('Completá prestación, fecha desde y fecha hasta.')
      return
    }
    crear.mutate(
      {
        pacienteId: paciente.id,
        prestacionId: Number(prestacionId),
        profesionalId: profesionalId ? Number(profesionalId) : null,
        fechaDesde,
        fechaHasta,
        horaDesde: horaDesde || null,
        horaHasta: horaHasta || null,
      },
      {
        onSuccess: () => {
          setPaciente(null)
          setDni('')
          setFechaDesde('')
          setFechaHasta('')
          setHoraDesde('')
          setHoraHasta('')
        },
        onError: (err) => setErrorForm(mensajeErrorListaEspera(err)),
      },
    )
  }

  return (
    <div className="min-h-screen bg-slate-50">
      <BarraDelPanel />
      <main className="mx-auto flex max-w-5xl flex-col gap-6 p-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h1 className="text-2xl font-bold text-slate-900">Lista de espera</h1>
          <label className="flex items-center gap-2 text-sm text-slate-800">
            <span>Estado</span>
            <select
              aria-label="Filtrar por estado"
              value={filtroEstado}
              onChange={(e) => setFiltroEstado(e.target.value as EstadoListaEspera | '')}
              className="rounded-md border border-slate-300 bg-white px-3 py-1.5"
            >
              <option value="">Todos</option>
              <option value="activa">Activas</option>
              <option value="ofrecida">Ofrecidas</option>
              <option value="atendida">Atendidas</option>
              <option value="vencida">Vencidas</option>
            </select>
          </label>
        </div>

        <section className="rounded-lg border border-slate-200 bg-white p-4">
          <h2 className="text-lg font-semibold text-slate-900">Agregar paciente a la lista</h2>
          <div className="mt-3 flex flex-wrap items-end gap-3">
            <label className="flex flex-col gap-1 text-sm">
              <span>DNI del paciente</span>
              <input
                type="text"
                value={dni}
                onChange={(e) => setDni(e.target.value)}
                placeholder="Ej. 30123456"
                className="rounded-md border border-slate-300 px-3 py-1.5"
              />
            </label>
            <button
              type="button"
              onClick={() => void manejarBuscarDni()}
              className="rounded-md border border-slate-400 px-3 py-1.5 text-sm font-medium text-slate-900 hover:bg-slate-100"
            >
              Buscar paciente
            </button>
            {paciente && (
              <p className="text-sm font-medium text-emerald-800">
                Paciente: {paciente.nombre} {paciente.apellido} (DNI {paciente.dni})
              </p>
            )}
            {errorDni && (
              <p role="alert" className="text-sm text-rose-700">
                {errorDni}
              </p>
            )}
          </div>

          <form onSubmit={manejarGuardar} className="mt-4 grid gap-3 sm:grid-cols-3">
            <label className="flex flex-col gap-1 text-sm">
              <span>Prestación</span>
              <select
                aria-label="Prestación"
                value={prestacionId}
                onChange={(e) => setPrestacionId(e.target.value)}
                className="rounded-md border border-slate-300 bg-white px-3 py-1.5"
              >
                <option value="">Elegí una prestación</option>
                {(prestaciones.data ?? [])
                  .filter((p) => p.activa !== false)
                  .map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.nombre}
                    </option>
                  ))}
              </select>
            </label>

            <label className="flex flex-col gap-1 text-sm">
              <span>Profesional (opcional)</span>
              <select
                aria-label="Profesional"
                value={profesionalId}
                onChange={(e) => setProfesionalId(e.target.value)}
                className="rounded-md border border-slate-300 bg-white px-3 py-1.5"
              >
                <option value="">Cualquier profesional</option>
                {(profesionales.data ?? [])
                  .filter((p) => p.activo !== false)
                  .map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.nombre} {p.apellido}
                    </option>
                  ))}
              </select>
            </label>

            <div className="grid grid-cols-2 gap-2">
              <label className="flex flex-col gap-1 text-sm">
                <span>Fecha desde</span>
                <input
                  type="date"
                  value={fechaDesde}
                  onChange={(e) => setFechaDesde(e.target.value)}
                  className="rounded-md border border-slate-300 px-2 py-1.5"
                />
              </label>
              <label className="flex flex-col gap-1 text-sm">
                <span>Fecha hasta</span>
                <input
                  type="date"
                  value={fechaHasta}
                  onChange={(e) => setFechaHasta(e.target.value)}
                  className="rounded-md border border-slate-300 px-2 py-1.5"
                />
              </label>
            </div>

            <label className="flex flex-col gap-1 text-sm">
              <span>Hora desde (opcional)</span>
              <input
                type="time"
                value={horaDesde}
                onChange={(e) => setHoraDesde(e.target.value)}
                className="rounded-md border border-slate-300 px-2 py-1.5"
              />
            </label>

            <label className="flex flex-col gap-1 text-sm">
              <span>Hora hasta (opcional)</span>
              <input
                type="time"
                value={horaHasta}
                onChange={(e) => setHoraHasta(e.target.value)}
                className="rounded-md border border-slate-300 px-2 py-1.5"
              />
            </label>

            <div className="flex items-end">
              <button
                type="submit"
                disabled={crear.isPending}
                className="rounded-md bg-sky-700 px-4 py-2 text-sm font-semibold text-white hover:bg-sky-800 disabled:opacity-60"
              >
                {crear.isPending ? 'Guardando...' : 'Agregar a lista de espera'}
              </button>
            </div>
          </form>

          {errorForm && (
            <p role="alert" className="mt-3 text-sm text-rose-700">
              {errorForm}
            </p>
          )}
        </section>

        <section className="rounded-lg border border-slate-200 bg-white p-4">
          {consulta.isLoading && <p className="text-slate-700">Cargando lista de espera...</p>}
          {consulta.isError && (
            <p role="alert" className="text-rose-700">
              {mensajeErrorListaEspera(consulta.error)}
            </p>
          )}
          {consulta.data && consulta.data.length === 0 && (
            <p className="text-slate-600">No hay entradas en la lista de espera.</p>
          )}
          {consulta.data && consulta.data.length > 0 && (
            <ul className="divide-y divide-slate-200">
              {consulta.data.map((item) => (
                <li key={item.id} className="flex flex-wrap items-center justify-between gap-3 py-3">
                  <div>
                    <p className="font-semibold text-slate-900">
                      {item.pacienteNombre} — {item.prestacionNombre}
                    </p>
                    <p className="text-sm text-slate-700">
                      {item.profesionalNombre ?? 'Cualquier profesional'} · {item.fechaDesde} a {item.fechaHasta}
                      {item.horaDesde && item.horaHasta ? ` (${item.horaDesde} a ${item.horaHasta})` : ''}
                    </p>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className="rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-medium text-slate-800">
                      {ETIQUETAS_ESTADO[item.estado]}
                    </span>
                    {item.estado === 'activa' && (
                      <button
                        type="button"
                        onClick={() => darDeBaja.mutate(item.id)}
                        className="rounded-md border border-rose-300 px-2.5 py-1 text-xs font-medium text-rose-800 hover:bg-rose-50"
                      >
                        Dar de baja
                      </button>
                    )}
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>
      </main>
    </div>
  )
}
