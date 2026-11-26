import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { z } from 'zod'
import type { Rol } from '../auth/sesion'
import {
  useCambiarRol,
  type ProfesionalDisponible,
  type RolDisponible,
  type UsuarioGestionado,
} from './api'
import { leerErrorDeApi } from '../../lib/erroresApi'
import { Campo } from '../../components/formulario/Campo'
import { CLASES_BOTON_PRIMARIO, CLASES_BOTON_SECUNDARIO, CLASES_CONTROL, ariaDeError } from '../../components/formulario/estilos'

const esquema = z
  .object({ rol: z.string().min(1, 'Elegí el rol'), profesionalId: z.string() })
  .superRefine((datos, ctx) => {
    if (datos.rol === 'profesional' && datos.profesionalId === '') {
      ctx.addIssue({ code: 'custom', path: ['profesionalId'], message: 'Elegí el profesional' })
    }
  })

type DatosFormulario = z.infer<typeof esquema>

interface FormularioCambioRolProps {
  usuario: UsuarioGestionado
  roles: RolDisponible[]
  profesionales: ProfesionalDisponible[]
  alTerminar: (mensaje: string) => void
  alCancelar: () => void
}

/**
 * Cambio de rol de otro usuario. Las opciones de profesional son los disponibles más el que el usuario ya
 * tiene vinculado, para poder conservarlo.
 */
export function FormularioCambioRol({ usuario, roles, profesionales, alTerminar, alCancelar }: FormularioCambioRolProps) {
  const cambiar = useCambiarRol()
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    control,
    formState: { errors, isSubmitting },
  } = useForm<DatosFormulario>({
    resolver: zodResolver(esquema),
    defaultValues: { rol: usuario.rol, profesionalId: usuario.profesionalId?.toString() ?? '' },
  })
  const esProfesional = useWatch({ control, name: 'rol' }) === 'profesional'

  const opciones: { id: number; nombre: string }[] = profesionales.map((p) => ({ id: p.id, nombre: `${p.nombre} ${p.apellido}` }))
  if (usuario.profesionalId !== null && !opciones.some((o) => o.id === usuario.profesionalId)) {
    opciones.unshift({ id: usuario.profesionalId, nombre: usuario.profesionalNombre ?? `Profesional ${usuario.profesionalId}` })
  }

  const enviar = handleSubmit(async (datos) => {
    setErrorGeneral(null)
    try {
      await cambiar.mutateAsync({
        id: usuario.id,
        rol: datos.rol as Rol,
        profesionalId: datos.rol === 'profesional' ? Number(datos.profesionalId) : null,
      })
      alTerminar('Rol actualizado')
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
      <Campo id="cambio-rol" etiqueta="Rol" error={errors.rol?.message}>
        <select id="cambio-rol" className={CLASES_CONTROL} {...ariaDeError('cambio-rol', errors.rol?.message)} {...register('rol')}>
          {roles.map((rol) => (
            <option key={rol.codigo} value={rol.codigo}>
              {rol.nombre}
            </option>
          ))}
        </select>
      </Campo>
      {esProfesional && (
        <Campo id="cambio-profesional" etiqueta="Profesional" error={errors.profesionalId?.message}>
          <select
            id="cambio-profesional"
            className={CLASES_CONTROL}
            {...ariaDeError('cambio-profesional', errors.profesionalId?.message)}
            {...register('profesionalId')}
          >
            <option value="">Elegí un profesional</option>
            {opciones.map((opcion) => (
              <option key={opcion.id} value={opcion.id}>
                {opcion.nombre}
              </option>
            ))}
          </select>
        </Campo>
      )}
      <div className="flex flex-wrap justify-end gap-2">
        <button type="button" onClick={alCancelar} className={CLASES_BOTON_SECUNDARIO}>
          Cancelar
        </button>
        <button type="submit" disabled={isSubmitting} className={CLASES_BOTON_PRIMARIO}>
          {isSubmitting ? 'Guardando…' : 'Guardar'}
        </button>
      </div>
    </form>
  )
}
