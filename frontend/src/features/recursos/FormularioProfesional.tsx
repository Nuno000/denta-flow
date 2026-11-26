import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import type { z } from 'zod'
import { Campo } from '../../components/formulario/Campo'
import {
  CLASES_BOTON_PRIMARIO,
  CLASES_BOTON_SECUNDARIO,
  CLASES_CONTROL,
  ariaDeError,
} from '../../components/formulario/estilos'
import { useCrearProfesional, useEditarProfesional, type Profesional } from './api'
import { aplicarErrorDeApi } from './avisosYErrores'
import { MensajeDeError } from './comunes'
import { esquemaProfesional, type EntradaProfesional } from './esquemas'

interface FormularioProfesionalProps {
  /** Sin profesional es un alta; con profesional, la edición de sus datos (no de sus prestaciones). */
  profesional?: Profesional
  alTerminar: (mensaje: string) => void
  alCancelar: () => void
}

type NombreCampo = keyof EntradaProfesional

const CAMPOS: { nombre: NombreCampo; etiqueta: string; tipo: string; autoComplete: string }[] = [
  { nombre: 'nombre', etiqueta: 'Nombre', tipo: 'text', autoComplete: 'off' },
  { nombre: 'apellido', etiqueta: 'Apellido', tipo: 'text', autoComplete: 'off' },
  { nombre: 'matricula', etiqueta: 'Matrícula (opcional)', tipo: 'text', autoComplete: 'off' },
  { nombre: 'email', etiqueta: 'Email', tipo: 'email', autoComplete: 'off' },
]

/** Alta y edición de los datos de un profesional; la matrícula es opcional. */
export function FormularioProfesional({ profesional, alTerminar, alCancelar }: FormularioProfesionalProps) {
  const crear = useCrearProfesional()
  const editar = useEditarProfesional()
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<EntradaProfesional, unknown, z.output<typeof esquemaProfesional>>({
    resolver: zodResolver(esquemaProfesional),
    defaultValues: {
      nombre: profesional?.nombre ?? '',
      apellido: profesional?.apellido ?? '',
      matricula: profesional?.matricula ?? '',
      email: profesional?.email ?? '',
    },
  })

  const enviar = handleSubmit(async (datos) => {
    setErrorGeneral(null)
    try {
      if (profesional === undefined) {
        await crear.mutateAsync(datos)
        alTerminar('Profesional creado')
      } else {
        await editar.mutateAsync({ id: profesional.id, ...datos })
        alTerminar('Profesional actualizado')
      }
    } catch (causa) {
      setErrorGeneral(
        aplicarErrorDeApi(causa, setError, {
          email: 'email',
          matrícula: 'matricula',
          apellido: 'apellido',
          nombre: 'nombre',
          matricula: 'matricula',
        }),
      )
    }
  })

  return (
    <form onSubmit={enviar} noValidate className="flex flex-col gap-4">
      {errorGeneral !== null && <MensajeDeError>{errorGeneral}</MensajeDeError>}
      {CAMPOS.map((campo) => {
        const id = `profesional-${campo.nombre}`
        const error = errors[campo.nombre]?.message
        return (
          <Campo key={campo.nombre} id={id} etiqueta={campo.etiqueta} error={error}>
            <input
              id={id}
              type={campo.tipo}
              autoComplete={campo.autoComplete}
              className={CLASES_CONTROL}
              {...ariaDeError(id, error)}
              {...register(campo.nombre)}
            />
          </Campo>
        )
      })}
      <div className="flex flex-wrap justify-end gap-2">
        <button type="button" onClick={alCancelar} className={CLASES_BOTON_SECUNDARIO}>
          Cancelar
        </button>
        <button type="submit" disabled={isSubmitting} className={CLASES_BOTON_PRIMARIO}>
          {profesional === undefined ? 'Crear profesional' : 'Guardar cambios'}
        </button>
      </div>
    </form>
  )
}
