import type { FormEventHandler } from 'react'
import type { UseFormReturn } from 'react-hook-form'
import { Campo } from '../../components/formulario/Campo'
import { CLASES_BOTON_PRIMARIO, CLASES_BOTON_SECUNDARIO, CLASES_CONTROL, ariaDeError } from '../../components/formulario/estilos'
import { AvisoPrivacidad } from './AvisoPrivacidad'
import type { DatosReserva, DatosReservaEnFormulario } from './esquemasReserva'
import { CLASES_ALERTA } from './estilosReserva'
import { TituloDePaso } from './TituloDePaso'

type CampoDeTexto = 'dni' | 'nombre' | 'apellido' | 'telefono' | 'email'

const CAMPOS: { nombre: CampoDeTexto; etiqueta: string; tipo: string; autocompletar: string; modo?: 'numeric' | 'tel' | 'email' }[] = [
  { nombre: 'dni', etiqueta: 'DNI', tipo: 'text', autocompletar: 'off', modo: 'numeric' },
  { nombre: 'nombre', etiqueta: 'Nombre', tipo: 'text', autocompletar: 'given-name' },
  { nombre: 'apellido', etiqueta: 'Apellido', tipo: 'text', autocompletar: 'family-name' },
  { nombre: 'telefono', etiqueta: 'Teléfono', tipo: 'tel', autocompletar: 'tel', modo: 'tel' },
  { nombre: 'email', etiqueta: 'Email', tipo: 'email', autocompletar: 'email', modo: 'email' },
]

interface PasoDatosProps {
  formulario: UseFormReturn<DatosReservaEnFormulario, unknown, DatosReserva>
  resumen: string
  aviso: string | null
  enviando: boolean
  alEnviar: FormEventHandler<HTMLFormElement>
  alVolver: () => void
}

/**
 * Paso 4: datos mínimos del paciente (RN-PR-03) y aviso de privacidad (C-15). Los errores quedan asociados a su
 * campo y, al enviar con errores, el foco va al primero (lo hace React Hook Form).
 */
export function PasoDatos({ formulario, resumen, aviso, enviando, alEnviar, alVolver }: PasoDatosProps) {
  const {
    register,
    formState: { errors },
  } = formulario

  return (
    <section className="flex flex-col gap-3">
      <TituloDePaso>Tus datos</TituloDePaso>
      <p className="text-base text-slate-800">{resumen}</p>
      {aviso !== null && (
        <p role="alert" className={CLASES_ALERTA}>
          {aviso}
        </p>
      )}
      <form onSubmit={alEnviar} noValidate className="flex flex-col gap-3">
        {CAMPOS.map((campo) => (
          <Campo key={campo.nombre} id={campo.nombre} etiqueta={campo.etiqueta} error={errors[campo.nombre]?.message}>
            <input
              id={campo.nombre}
              type={campo.tipo}
              inputMode={campo.modo}
              autoComplete={campo.autocompletar}
              className={CLASES_CONTROL}
              {...ariaDeError(campo.nombre, errors[campo.nombre]?.message)}
              {...register(campo.nombre)}
            />
          </Campo>
        ))}
        <AvisoPrivacidad registro={register('aceptaAvisoPrivacidad')} error={errors.aceptaAvisoPrivacidad?.message} />
        <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-between">
          <button type="button" className={`${CLASES_BOTON_SECUNDARIO} min-h-11`} onClick={alVolver}>
            Volver
          </button>
          <button type="submit" className={`${CLASES_BOTON_PRIMARIO} min-h-11`} disabled={enviando}>
            {enviando ? 'Reservando…' : 'Reservar turno'}
          </button>
        </div>
      </form>
    </section>
  )
}
