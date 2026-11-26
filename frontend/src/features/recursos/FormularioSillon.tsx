import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Campo } from '../../components/formulario/Campo'
import {
  CLASES_BOTON_PRIMARIO,
  CLASES_BOTON_SECUNDARIO,
  CLASES_CONTROL,
  ariaDeError,
} from '../../components/formulario/estilos'
import { useCrearSillon, useEditarSillon, type Sillon } from './api'
import { aplicarErrorDeApi } from './avisosYErrores'
import { MensajeDeError } from './comunes'
import { esquemaSillon, type EntradaSillon } from './esquemas'

interface FormularioSillonProps {
  /** Sin sillón es un alta; con sillón, su edición. */
  sillon?: Sillon
  alTerminar: (mensaje: string) => void
  alCancelar: () => void
}

/** Alta y edición de un sillón (spec "Formularios de recursos con validación y respuesta de la API"). */
export function FormularioSillon({ sillon, alTerminar, alCancelar }: FormularioSillonProps) {
  const crear = useCrearSillon()
  const editar = useEditarSillon()
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<EntradaSillon>({
    resolver: zodResolver(esquemaSillon),
    defaultValues: { nombre: sillon?.nombre ?? '' },
  })

  const enviar = handleSubmit(async (datos) => {
    setErrorGeneral(null)
    try {
      if (sillon === undefined) {
        await crear.mutateAsync(datos)
        alTerminar('Sillón creado')
      } else {
        await editar.mutateAsync({ id: sillon.id, ...datos })
        alTerminar('Sillón actualizado')
      }
    } catch (causa) {
      setErrorGeneral(aplicarErrorDeApi(causa, setError, { nombre: 'nombre' }))
    }
  })

  return (
    <form onSubmit={enviar} noValidate className="flex flex-col gap-4">
      {errorGeneral !== null && <MensajeDeError>{errorGeneral}</MensajeDeError>}
      <Campo id="sillon-nombre" etiqueta="Nombre" error={errors.nombre?.message}>
        <input
          id="sillon-nombre"
          type="text"
          autoComplete="off"
          className={CLASES_CONTROL}
          {...ariaDeError('sillon-nombre', errors.nombre?.message)}
          {...register('nombre')}
        />
      </Campo>
      <div className="flex flex-wrap justify-end gap-2">
        <button type="button" onClick={alCancelar} className={CLASES_BOTON_SECUNDARIO}>
          Cancelar
        </button>
        <button type="submit" disabled={isSubmitting} className={CLASES_BOTON_PRIMARIO}>
          {sillon === undefined ? 'Crear sillón' : 'Guardar cambios'}
        </button>
      </div>
    </form>
  )
}
