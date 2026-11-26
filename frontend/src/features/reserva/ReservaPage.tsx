import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { esquemaDatosReserva, type DatosReserva, type DatosReservaEnFormulario } from './esquemasReserva'
import { horaDe } from './fechasDelConsultorio'
import { PasoConfirmacion } from './PasoConfirmacion'
import { PasoDatos } from './PasoDatos'
import { PasoHorario } from './PasoHorario'
import { PasoPrestacion } from './PasoPrestacion'
import { PasoProfesional } from './PasoProfesional'
import {
  motivoDelRechazo,
  useReservar,
  type HuecoPublico,
  type PrestacionPublica,
  type ProfesionalPublico,
  type ReservaConfirmada,
} from './reservaApi'

type Paso = 'prestacion' | 'profesional' | 'horario' | 'datos' | 'confirmacion'

const CAMPOS_DEL_FORMULARIO = ['dni', 'nombre', 'apellido', 'telefono', 'email', 'aceptaAvisoPrivacidad'] as const
type CampoDelFormulario = (typeof CAMPOS_DEL_FORMULARIO)[number]

function esCampoDelFormulario(campo: string): campo is CampoDelFormulario {
  return (CAMPOS_DEL_FORMULARIO as readonly string[]).includes(campo)
}

/**
 * Reserva online pública (US-010, Flujo 1, C-16 design D6): prestación → profesional o cualquiera → día y
 * horario → datos y aviso → confirmación. Mobile-first y accesible; lo elegido y los datos se conservan al volver
 * y cuando el horario fue tomado por otra reserva.
 */
export function ReservaPage() {
  const [paso, setPaso] = useState<Paso>('prestacion')
  const [prestacion, setPrestacion] = useState<PrestacionPublica | null>(null)
  const [profesional, setProfesional] = useState<ProfesionalPublico | null | undefined>(undefined)
  const [fecha, setFecha] = useState<string | null>(null)
  const [hueco, setHueco] = useState<HuecoPublico | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)
  const [reserva, setReserva] = useState<ReservaConfirmada | null>(null)

  const formulario = useForm<DatosReservaEnFormulario, unknown, DatosReserva>({
    resolver: zodResolver(esquemaDatosReserva),
    defaultValues: { dni: '', nombre: '', apellido: '', telefono: '', email: '', aceptaAvisoPrivacidad: false },
  })
  const reservar = useReservar()

  function irA(siguiente: Paso) {
    setAviso(null)
    setPaso(siguiente)
  }

  const enviar = formulario.handleSubmit(async (datos) => {
    if (prestacion === null || hueco === null) {
      return
    }
    setAviso(null)
    try {
      const confirmada = await reservar.mutateAsync({
        prestacionId: prestacion.id,
        profesionalId: profesional?.id ?? null,
        inicio: hueco.inicio,
        paciente: {
          dni: datos.dni,
          nombre: datos.nombre,
          apellido: datos.apellido,
          telefono: datos.telefono,
          email: datos.email,
        },
        aceptaAvisoPrivacidad: datos.aceptaAvisoPrivacidad,
      })
      setReserva(confirmada)
      setPaso('confirmacion')
    } catch (error) {
      const rechazo = motivoDelRechazo(error)
      if (rechazo.motivo === 'horario-tomado') {
        setHueco(null)
        setPaso('horario')
        setAviso(rechazo.mensaje)
        return
      }
      const campos = Object.entries(rechazo.campos)
        .map(([campo, mensaje]) => [campo.split('.').at(-1) ?? campo, mensaje] as const)
        .filter(([campo]) => esCampoDelFormulario(campo))
      campos.forEach(([campo, mensaje], i) =>
        formulario.setError(campo as CampoDelFormulario, { message: mensaje }, { shouldFocus: i === 0 }),
      )
      if (campos.length === 0) {
        setAviso(rechazo.mensaje)
      }
    }
  })

  const resumen =
    prestacion !== null && hueco !== null
      ? `${prestacion.nombre} · ${hueco.inicio.slice(8, 10)}/${hueco.inicio.slice(5, 7)} a las ${horaDe(hueco.inicio)} · ${
          profesional ? `${profesional.nombre} ${profesional.apellido}` : 'Cualquier profesional'
        }`
      : ''

  return (
    <main className="mx-auto flex min-h-screen w-full max-w-xl flex-col gap-4 p-4 sm:p-6">
      <h1 className="text-2xl font-bold text-slate-900">Reservá tu turno</h1>
      {paso === 'prestacion' && (
        <PasoPrestacion
          elegida={prestacion}
          alElegir={(elegida) => {
            if (elegida.id !== prestacion?.id) {
              setProfesional(undefined)
              setFecha(null)
              setHueco(null)
            }
            setPrestacion(elegida)
            irA('profesional')
          }}
        />
      )}
      {paso === 'profesional' && prestacion !== null && (
        <PasoProfesional
          prestacionId={prestacion.id}
          elegido={profesional}
          alElegir={(elegido) => {
            if (elegido?.id !== profesional?.id || profesional === undefined) {
              setHueco(null)
            }
            setProfesional(elegido)
            irA('horario')
          }}
          alVolver={() => irA('prestacion')}
        />
      )}
      {paso === 'horario' && prestacion !== null && (
        <PasoHorario
          prestacionId={prestacion.id}
          profesionalId={profesional?.id ?? null}
          fecha={fecha}
          hueco={hueco}
          aviso={aviso}
          alElegirFecha={(elegida) => {
            setFecha(elegida)
            setHueco(null)
          }}
          alElegirHueco={(elegido) => {
            setHueco(elegido)
            irA('datos')
          }}
          alVolver={() => irA('profesional')}
        />
      )}
      {paso === 'datos' && (
        <PasoDatos
          formulario={formulario}
          resumen={resumen}
          aviso={aviso}
          enviando={formulario.formState.isSubmitting || reservar.isPending}
          alEnviar={(evento) => void enviar(evento)}
          alVolver={() => irA('horario')}
        />
      )}
      {paso === 'confirmacion' && reserva !== null && <PasoConfirmacion reserva={reserva} />}
    </main>
  )
}
