import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { Link } from 'react-router-dom'
import { Campo } from '../../components/formulario/Campo'
import { Dialogo } from '../../components/formulario/Dialogo'
import {
  CLASES_BOTON_PRIMARIO,
  CLASES_BOTON_SECUNDARIO,
  CLASES_CONTROL,
  ariaDeError,
} from '../../components/formulario/estilos'
import { ErrorApi } from '../../lib/apiClient'
import { leerErrorDeApi } from '../../lib/erroresApi'
import { BarraDelPanel } from '../auth/BarraDelPanel'
import { usePrestaciones, useProfesionales, useSillones } from '../recursos/api'
import {
  alternativasDeError,
  useBuscarPacientePorDni,
  useCargarSobreturno,
  useCargarTurno,
  type Alternativa,
  type PedidoDeCarga,
} from './api'
import {
  esquemaCargaDeTurno,
  esquemaDni,
  type DatosCargaDeTurno,
  type EntradaCargaDeTurno,
} from './esquemaCargaDeTurno'
import { fechaCorta, fechaDeInstante, horaEnConsultorio, hoyEnConsultorio, inicioEnConsultorio } from './fechas'

type TipoDeCarga = 'comun' | 'sobreturno'

const AVISO_REVISAR = 'Revisá los campos marcados'

/** Campos del formulario que el backend puede marcar con un error en `errores` (claves de Bean Validation). */
const CAMPOS_DEL_BACKEND = [
  'paciente.dni',
  'paciente.nombre',
  'paciente.apellido',
  'paciente.telefono',
  'paciente.email',
  'prestacionId',
  'profesionalId',
  'sillonId',
] as const

function valoresIniciales(): EntradaCargaDeTurno {
  return {
    paciente: { dni: '', nombre: '', apellido: '', telefono: '', email: '' },
    prestacionId: '',
    profesionalId: '',
    sillonId: '',
    fecha: hoyEnConsultorio(),
    hora: '',
  }
}

function pedidoDe(datos: DatosCargaDeTurno): PedidoDeCarga {
  return {
    paciente: datos.paciente,
    prestacionId: datos.prestacionId,
    profesionalId: datos.profesionalId,
    sillonId: datos.sillonId,
    inicio: inicioEnConsultorio(datos.fecha, datos.hora),
  }
}

/**
 * Pantalla "Nuevo turno" (C-12, US-021 y US-022; spec carga-manual-turnos, design D6). Solo administrador y
 * recepción (la ruta lo restringe y el backend lo verifica). El sobreturno es una acción separada que pide
 * confirmación; el conflicto muestra el motivo y las alternativas, que se eligen sin enviar nada.
 */
export function CargaDeTurnoPage() {
  const prestaciones = usePrestaciones()
  const profesionales = useProfesionales()
  const sillones = useSillones()
  const buscarPaciente = useBuscarPacientePorDni()
  const cargarTurno = useCargarTurno()
  const cargarSobreturno = useCargarSobreturno()

  const [avisoDePaciente, setAvisoDePaciente] = useState('')
  const [resultado, setResultado] = useState<{ mensaje: string; fecha: string } | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [alternativas, setAlternativas] = useState<Alternativa[]>([])
  const [sinAlternativas, setSinAlternativas] = useState(false)
  const [sobreturnoPendiente, setSobreturnoPendiente] = useState<DatosCargaDeTurno | null>(null)

  const {
    register,
    handleSubmit,
    setValue,
    getValues,
    setError: marcarCampo,
    clearErrors,
    reset,
    control,
    formState: { errors },
  } = useForm<EntradaCargaDeTurno, unknown, DatosCargaDeTurno>({
    resolver: zodResolver(esquemaCargaDeTurno),
    defaultValues: valoresIniciales(),
  })

  const prestacionElegida = Number(useWatch({ control, name: 'prestacionId' }))
  const prestacionesActivas = (prestaciones.data ?? []).filter((prestacion) => prestacion.activa)
  const sillonesActivos = (sillones.data ?? []).filter((sillon) => sillon.activo)
  const profesionalesDeLaPrestacion = (profesionales.data ?? []).filter(
    (profesional) => profesional.activo && profesional.prestacionIds.includes(prestacionElegida),
  )
  const enCurso = cargarTurno.isPending || cargarSobreturno.isPending

  async function buscar() {
    setAvisoDePaciente('')
    setError(null)
    const dni = esquemaDni.safeParse(getValues('paciente.dni'))
    if (!dni.success) {
      marcarCampo('paciente.dni', { message: dni.error.issues[0].message }, { shouldFocus: true })
      return
    }
    clearErrors('paciente.dni')
    try {
      const paciente = await buscarPaciente.mutateAsync(dni.data)
      setValue('paciente.dni', paciente.dni)
      setValue('paciente.nombre', paciente.nombre)
      setValue('paciente.apellido', paciente.apellido)
      setValue('paciente.telefono', paciente.telefono)
      setValue('paciente.email', paciente.email)
      clearErrors('paciente')
      setAvisoDePaciente('Paciente encontrado: revisá sus datos y corregilos si hace falta')
    } catch (causa) {
      if (causa instanceof ErrorApi && causa.status === 404) {
        setAvisoDePaciente('Paciente nuevo: completá sus datos')
      } else {
        setError(mensajeDe(causa))
      }
    }
  }

  async function enviar(datos: DatosCargaDeTurno, tipo: TipoDeCarga) {
    setError(null)
    setResultado(null)
    setAlternativas([])
    setSinAlternativas(false)
    const mutacion = tipo === 'comun' ? cargarTurno : cargarSobreturno
    try {
      await mutacion.mutateAsync(pedidoDe(datos))
      setResultado({ mensaje: tipo === 'comun' ? 'Turno cargado' : 'Sobreturno cargado', fecha: datos.fecha })
      setAvisoDePaciente('')
      reset(valoresIniciales())
    } catch (causa) {
      tratarError(causa, tipo)
    }
  }

  function tratarError(causa: unknown, tipo: TipoDeCarga) {
    const { campos, general } = leerErrorDeApi(causa)
    const conCampo = CAMPOS_DEL_BACKEND.filter((campo) => campos[campo] !== undefined)
    if (conCampo.length > 0) {
      conCampo.forEach((campo) => marcarCampo(campo, { message: campos[campo] }))
      setError(AVISO_REVISAR)
      return
    }
    if (causa instanceof ErrorApi && causa.status === 403) {
      setError('No tenés permiso para cargar turnos')
      return
    }
    setError(general)
    if (causa instanceof ErrorApi && causa.status === 409 && tipo === 'comun') {
      const sugeridas = alternativasDeError(causa)
      setAlternativas(sugeridas)
      setSinAlternativas(sugeridas.length === 0)
    }
  }

  function usarAlternativa(alternativa: Alternativa) {
    setValue('fecha', fechaDeInstante(alternativa.inicio), { shouldValidate: true })
    setValue('hora', horaEnConsultorio(alternativa.inicio), { shouldValidate: true })
    setValue('sillonId', String(alternativa.sillonId), { shouldValidate: true })
    setError(null)
    setAlternativas([])
    setSinAlternativas(false)
  }

  const alValidarCargaComun = handleSubmit(
    (datos) => enviar(datos, 'comun'),
    () => setError(AVISO_REVISAR),
  )
  const alPedirSobreturno = handleSubmit(
    (datos) => {
      setError(null)
      setSobreturnoPendiente(datos)
    },
    () => setError(AVISO_REVISAR),
  )

  function confirmarSobreturno() {
    const datos = sobreturnoPendiente
    setSobreturnoPendiente(null)
    if (datos !== null) {
      void enviar(datos, 'sobreturno')
    }
  }

  return (
    <>
      <BarraDelPanel />
      <main className="mx-auto flex max-w-3xl flex-col gap-4 p-4 sm:p-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h1 className="text-2xl font-bold text-slate-900">Nuevo turno</h1>
          <Link to="/panel/agenda" className="font-semibold text-sky-800 underline">
            Ir a la agenda
          </Link>
        </div>

        <p role="status" aria-live="polite" className="min-h-6 text-sm font-medium text-green-800">
          {resultado?.mensaje ?? ''}
        </p>
        {resultado !== null && (
          <Link to={`/panel/agenda?fecha=${resultado.fecha}`} className="font-semibold text-sky-800 underline">
            Volver a la agenda
          </Link>
        )}
        {error !== null && (
          <p role="alert" className="rounded-md border border-red-700 bg-red-50 p-3 text-sm text-red-800">
            {error}
          </p>
        )}
        {alternativas.length > 0 && (
          <section aria-label="Horarios alternativos" className="flex flex-col gap-2">
            <h2 className="text-base font-semibold text-slate-900">Horarios libres cercanos</h2>
            <ul className="flex flex-wrap gap-2">
              {alternativas.map((alternativa) => (
                <li key={`${alternativa.inicio}-${alternativa.sillonId}`}>
                  <button type="button" onClick={() => usarAlternativa(alternativa)} className={CLASES_BOTON_SECUNDARIO}>
                    {`Usar ${etiquetaDeAlternativa(alternativa, nombreDelSillon(sillones.data, alternativa.sillonId))}`}
                  </button>
                </li>
              ))}
            </ul>
          </section>
        )}
        {sinAlternativas && (
          <p role="status" className="text-sm text-slate-700">
            No hay horarios cercanos libres para ese profesional
          </p>
        )}

        <form onSubmit={alValidarCargaComun} noValidate className="flex flex-col gap-6">
          <fieldset className="flex flex-col gap-4">
            <legend className="mb-2 text-lg font-bold text-slate-900">Paciente</legend>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-[1fr_auto] sm:items-end">
              <Campo id="paciente-dni" etiqueta="DNI" error={errors.paciente?.dni?.message}>
                <input
                  id="paciente-dni"
                  type="text"
                  inputMode="numeric"
                  autoComplete="off"
                  className={CLASES_CONTROL}
                  {...ariaDeError('paciente-dni', errors.paciente?.dni?.message)}
                  {...register('paciente.dni')}
                />
              </Campo>
              <button
                type="button"
                onClick={() => void buscar()}
                disabled={buscarPaciente.isPending}
                className={CLASES_BOTON_SECUNDARIO}
              >
                Buscar paciente
              </button>
            </div>
            <p role="status" aria-live="polite" className="min-h-6 text-sm font-medium text-slate-800">
              {avisoDePaciente}
            </p>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <Campo id="paciente-nombre" etiqueta="Nombre" error={errors.paciente?.nombre?.message}>
                <input
                  id="paciente-nombre"
                  type="text"
                  autoComplete="off"
                  className={CLASES_CONTROL}
                  {...ariaDeError('paciente-nombre', errors.paciente?.nombre?.message)}
                  {...register('paciente.nombre')}
                />
              </Campo>
              <Campo id="paciente-apellido" etiqueta="Apellido" error={errors.paciente?.apellido?.message}>
                <input
                  id="paciente-apellido"
                  type="text"
                  autoComplete="off"
                  className={CLASES_CONTROL}
                  {...ariaDeError('paciente-apellido', errors.paciente?.apellido?.message)}
                  {...register('paciente.apellido')}
                />
              </Campo>
              <Campo id="paciente-telefono" etiqueta="Teléfono" error={errors.paciente?.telefono?.message}>
                <input
                  id="paciente-telefono"
                  type="tel"
                  autoComplete="off"
                  className={CLASES_CONTROL}
                  {...ariaDeError('paciente-telefono', errors.paciente?.telefono?.message)}
                  {...register('paciente.telefono')}
                />
              </Campo>
              <Campo id="paciente-email" etiqueta="Email" error={errors.paciente?.email?.message}>
                <input
                  id="paciente-email"
                  type="email"
                  autoComplete="off"
                  className={CLASES_CONTROL}
                  {...ariaDeError('paciente-email', errors.paciente?.email?.message)}
                  {...register('paciente.email')}
                />
              </Campo>
            </div>
          </fieldset>

          <fieldset className="flex flex-col gap-4">
            <legend className="mb-2 text-lg font-bold text-slate-900">Turno</legend>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <Campo id="turno-prestacion" etiqueta="Prestación" error={errors.prestacionId?.message}>
                <select
                  id="turno-prestacion"
                  className={CLASES_CONTROL}
                  {...ariaDeError('turno-prestacion', errors.prestacionId?.message)}
                  {...register('prestacionId', { onChange: () => setValue('profesionalId', '') })}
                >
                  <option value="">Elegí una prestación</option>
                  {prestacionesActivas.map((prestacion) => (
                    <option key={prestacion.id} value={prestacion.id}>
                      {prestacion.nombre}
                    </option>
                  ))}
                </select>
              </Campo>
              <Campo id="turno-profesional" etiqueta="Profesional" error={errors.profesionalId?.message}>
                <select
                  id="turno-profesional"
                  className={CLASES_CONTROL}
                  {...ariaDeError('turno-profesional', errors.profesionalId?.message)}
                  {...register('profesionalId')}
                >
                  <option value="">{textoVacioDeProfesional(prestacionElegida, profesionalesDeLaPrestacion.length)}</option>
                  {profesionalesDeLaPrestacion.map((profesional) => (
                    <option key={profesional.id} value={profesional.id}>
                      {`${profesional.nombre} ${profesional.apellido}`}
                    </option>
                  ))}
                </select>
              </Campo>
              <Campo id="turno-sillon" etiqueta="Sillón" error={errors.sillonId?.message}>
                <select
                  id="turno-sillon"
                  className={CLASES_CONTROL}
                  {...ariaDeError('turno-sillon', errors.sillonId?.message)}
                  {...register('sillonId')}
                >
                  <option value="">Elegí un sillón</option>
                  {sillonesActivos.map((sillon) => (
                    <option key={sillon.id} value={sillon.id}>
                      {sillon.nombre}
                    </option>
                  ))}
                </select>
              </Campo>
              <div className="grid grid-cols-2 gap-3">
                <Campo id="turno-fecha" etiqueta="Fecha" error={errors.fecha?.message}>
                  <input
                    id="turno-fecha"
                    type="date"
                    className={CLASES_CONTROL}
                    {...ariaDeError('turno-fecha', errors.fecha?.message)}
                    {...register('fecha')}
                  />
                </Campo>
                <Campo id="turno-hora" etiqueta="Hora" error={errors.hora?.message}>
                  <input
                    id="turno-hora"
                    type="time"
                    step={300}
                    className={CLASES_CONTROL}
                    {...ariaDeError('turno-hora', errors.hora?.message)}
                    {...register('hora')}
                  />
                </Campo>
              </div>
            </div>
            <p className="text-sm text-slate-700">La fecha y la hora son las del consultorio (Buenos Aires).</p>
          </fieldset>

          <div className="flex flex-wrap justify-end gap-2">
            <button type="button" onClick={() => void alPedirSobreturno()} disabled={enCurso} className={CLASES_BOTON_SECUNDARIO}>
              Cargar sobreturno
            </button>
            <button type="submit" disabled={enCurso} className={CLASES_BOTON_PRIMARIO}>
              Cargar turno
            </button>
          </div>
        </form>
      </main>

      {sobreturnoPendiente !== null && (
        <Dialogo titulo="¿Cargar un sobreturno?" alCerrar={() => setSobreturnoPendiente(null)}>
          <p className="text-slate-700">
            El sobreturno se superpone con otros turnos del profesional o del sillón. Queda registrado quién lo cargó.
          </p>
          <div className="flex flex-wrap justify-end gap-2">
            <button type="button" onClick={() => setSobreturnoPendiente(null)} className={CLASES_BOTON_SECUNDARIO}>
              Cancelar
            </button>
            <button type="button" onClick={confirmarSobreturno} className={CLASES_BOTON_PRIMARIO}>
              Confirmar sobreturno
            </button>
          </div>
        </Dialogo>
      )}
    </>
  )
}

function textoVacioDeProfesional(prestacionElegida: number, cantidad: number): string {
  if (!(prestacionElegida > 0)) {
    return 'Elegí primero la prestación'
  }
  return cantidad === 0 ? 'No hay profesionales para esta prestación' : 'Elegí un profesional'
}

function nombreDelSillon(sillones: { id: number; nombre: string }[] | undefined, id: number): string {
  return sillones?.find((sillon) => sillon.id === id)?.nombre ?? `Sillón ${id}`
}

/** "martes 1/12 10:00 — Sillón 2", en hora del consultorio. */
function etiquetaDeAlternativa(alternativa: Alternativa, sillon: string): string {
  return `${fechaCorta(fechaDeInstante(alternativa.inicio))} ${horaEnConsultorio(alternativa.inicio)} — ${sillon}`
}

function mensajeDe(causa: unknown): string {
  return leerErrorDeApi(causa).general
}
