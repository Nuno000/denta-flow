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
import { useCrearPrestacion, useEditarPrestacion, type Prestacion } from './api'
import { aplicarErrorDeApi } from './avisosYErrores'
import { MensajeDeError } from './comunes'
import { esquemaPrestacion, type EntradaPrestacion } from './esquemas'

interface FormularioPrestacionProps {
  /** Sin prestación es un alta; con prestación, su edición. */
  prestacion?: Prestacion
  alTerminar: (mensaje: string) => void
  alCancelar: () => void
}

/** Alta y edición de una prestación y su duración en minutos (RN-TU-03). */
export function FormularioPrestacion({ prestacion, alTerminar, alCancelar }: FormularioPrestacionProps) {
  const crear = useCrearPrestacion()
  const editar = useEditarPrestacion()
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<EntradaPrestacion, unknown, z.output<typeof esquemaPrestacion>>({
    resolver: zodResolver(esquemaPrestacion),
    defaultValues: {
      nombre: prestacion?.nombre ?? '',
      duracionMinutos: prestacion === undefined ? '' : String(prestacion.duracionMinutos),
    },
  })

  const enviar = handleSubmit(async (datos) => {
    setErrorGeneral(null)
    try {
      if (prestacion === undefined) {
        await crear.mutateAsync(datos)
        alTerminar('Prestación creada')
      } else {
        await editar.mutateAsync({ id: prestacion.id, ...datos })
        alTerminar('Prestación actualizada')
      }
    } catch (causa) {
      setErrorGeneral(
        aplicarErrorDeApi(causa, setError, { nombre: 'nombre', duración: 'duracionMinutos', duracionMinutos: 'duracionMinutos' }),
      )
    }
  })

  return (
    <form onSubmit={enviar} noValidate className="flex flex-col gap-4">
      {errorGeneral !== null && <MensajeDeError>{errorGeneral}</MensajeDeError>}
      <Campo id="prestacion-nombre" etiqueta="Nombre" error={errors.nombre?.message}>
        <input
          id="prestacion-nombre"
          type="text"
          autoComplete="off"
          className={CLASES_CONTROL}
          {...ariaDeError('prestacion-nombre', errors.nombre?.message)}
          {...register('nombre')}
        />
      </Campo>
      <Campo id="prestacion-duracion" etiqueta="Duración (minutos)" error={errors.duracionMinutos?.message}>
        <input
          id="prestacion-duracion"
          type="number"
          inputMode="numeric"
          min={1}
          step={1}
          className={CLASES_CONTROL}
          {...ariaDeError('prestacion-duracion', errors.duracionMinutos?.message)}
          {...register('duracionMinutos')}
        />
      </Campo>
      <div className="flex flex-wrap justify-end gap-2">
        <button type="button" onClick={alCancelar} className={CLASES_BOTON_SECUNDARIO}>
          Cancelar
        </button>
        <button type="submit" disabled={isSubmitting} className={CLASES_BOTON_PRIMARIO}>
          {prestacion === undefined ? 'Crear prestación' : 'Guardar cambios'}
        </button>
      </div>
    </form>
  )
}
