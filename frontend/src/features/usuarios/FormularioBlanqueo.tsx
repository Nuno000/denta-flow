import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { leerErrorDeApi, useBlanquearContrasena, type UsuarioGestionado } from './api'
import { Campo } from './Campo'
import { CLASES_BOTON_PRIMARIO, CLASES_BOTON_SECUNDARIO, CLASES_CONTROL, ariaDeError } from './estilosFormulario'

const esquema = z
  .object({
    contrasena: z
      .string()
      .min(8, 'La contraseña debe tener al menos 8 caracteres')
      .max(72, 'La contraseña no puede superar los 72 caracteres'),
    confirmacion: z.string(),
  })
  .refine((datos) => datos.contrasena === datos.confirmacion, {
    path: ['confirmacion'],
    message: 'Las contraseñas no coinciden',
    when: () => true,
  })

type DatosFormulario = z.infer<typeof esquema>

interface FormularioBlanqueoProps {
  usuario: UsuarioGestionado
  alTerminar: (mensaje: string) => void
  alCancelar: () => void
}

/** Blanqueo de la contraseña de otro usuario: nunca se muestra la contraseña después de enviarla. */
export function FormularioBlanqueo({ usuario, alTerminar, alCancelar }: FormularioBlanqueoProps) {
  const blanquear = useBlanquearContrasena()
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<DatosFormulario>({ resolver: zodResolver(esquema), defaultValues: { contrasena: '', confirmacion: '' } })

  const enviar = handleSubmit(async (datos) => {
    setErrorGeneral(null)
    try {
      await blanquear.mutateAsync({ id: usuario.id, contrasena: datos.contrasena })
      alTerminar('Contraseña cambiada')
    } catch (error) {
      setErrorGeneral(leerErrorDeApi(error).general)
    }
  })

  return (
    <form onSubmit={enviar} noValidate className="flex flex-col gap-4">
      {errorGeneral !== null && (
        <p role="alert" className="rounded-md border border-red-700 bg-red-50 p-3 text-sm text-red-800">
          {errorGeneral}
        </p>
      )}
      <Campo id="blanqueo-contrasena" etiqueta="Contraseña nueva" error={errors.contrasena?.message}>
        <input
          id="blanqueo-contrasena"
          type="password"
          autoComplete="new-password"
          className={CLASES_CONTROL}
          {...ariaDeError('blanqueo-contrasena', errors.contrasena?.message)}
          {...register('contrasena')}
        />
      </Campo>
      <Campo id="blanqueo-confirmacion" etiqueta="Repetí la contraseña" error={errors.confirmacion?.message}>
        <input
          id="blanqueo-confirmacion"
          type="password"
          autoComplete="new-password"
          className={CLASES_CONTROL}
          {...ariaDeError('blanqueo-confirmacion', errors.confirmacion?.message)}
          {...register('confirmacion')}
        />
      </Campo>
      <div className="flex flex-wrap justify-end gap-2">
        <button type="button" onClick={alCancelar} className={CLASES_BOTON_SECUNDARIO}>
          Cancelar
        </button>
        <button type="submit" disabled={isSubmitting} className={CLASES_BOTON_PRIMARIO}>
          {isSubmitting ? 'Cambiando…' : 'Cambiar contraseña'}
        </button>
      </div>
    </form>
  )
}
