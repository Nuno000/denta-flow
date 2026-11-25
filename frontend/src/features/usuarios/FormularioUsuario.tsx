import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { z } from 'zod'
import type { Rol } from '../auth/sesion'
import { leerErrorDeApi, useCrearUsuario, type ProfesionalDisponible, type RolDisponible } from './api'
import { Campo } from './Campo'
import { CLASES_BOTON_PRIMARIO, CLASES_BOTON_SECUNDARIO, CLASES_CONTROL, ariaDeError } from './estilosFormulario'

const esquema = z
  .object({
    email: z.string().trim().min(1, 'Ingresá el email').email('El email no tiene un formato válido'),
    contrasena: z
      .string()
      .min(8, 'La contraseña debe tener al menos 8 caracteres')
      .max(72, 'La contraseña no puede superar los 72 caracteres'),
    rol: z.string().min(1, 'Elegí el rol'),
    profesionalId: z.string(),
  })
  .superRefine((datos, ctx) => {
    if (datos.rol === 'profesional' && datos.profesionalId === '') {
      ctx.addIssue({ code: 'custom', path: ['profesionalId'], message: 'Elegí el profesional' })
    }
  })

type DatosFormulario = z.infer<typeof esquema>

interface FormularioUsuarioProps {
  roles: RolDisponible[]
  profesionales: ProfesionalDisponible[]
  alTerminar: (mensaje: string) => void
  alCancelar: () => void
}

/** Alta de usuario (spec "Alta de usuario desde la pantalla"): el profesional solo con el rol profesional. */
export function FormularioUsuario({ roles, profesionales, alTerminar, alCancelar }: FormularioUsuarioProps) {
  const crear = useCrearUsuario()
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    control,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<DatosFormulario>({
    resolver: zodResolver(esquema),
    defaultValues: { email: '', contrasena: '', rol: '', profesionalId: '' },
  })
  const esProfesional = useWatch({ control, name: 'rol' }) === 'profesional'

  const enviar = handleSubmit(async (datos) => {
    setErrorGeneral(null)
    try {
      await crear.mutateAsync({
        email: datos.email,
        contrasena: datos.contrasena,
        rol: datos.rol as Rol,
        profesionalId: datos.rol === 'profesional' ? Number(datos.profesionalId) : null,
      })
      alTerminar('Usuario creado')
    } catch (error) {
      const { campos, general } = leerErrorDeApi(error)
      const nombres = ['email', 'contrasena', 'rol', 'profesionalId'] as const
      const conCampo = nombres.filter((nombre) => campos[nombre] !== undefined)
      conCampo.forEach((nombre) => setError(nombre, { message: campos[nombre] }))
      if (conCampo.length > 0) {
        return
      }
      if (/email/i.test(general)) {
        setError('email', { message: general }, { shouldFocus: true })
      } else if (/profesional/i.test(general)) {
        setError('profesionalId', { message: general }, { shouldFocus: true })
      } else {
        setErrorGeneral(general)
      }
    }
  })

  return (
    <form onSubmit={enviar} noValidate className="flex flex-col gap-4">
      {errorGeneral !== null && (
        <p role="alert" className="rounded-md border border-red-700 bg-red-50 p-3 text-sm text-red-800">
          {errorGeneral}
        </p>
      )}
      <Campo id="alta-email" etiqueta="Email" error={errors.email?.message}>
        <input
          id="alta-email"
          type="email"
          autoComplete="off"
          className={CLASES_CONTROL}
          {...ariaDeError('alta-email', errors.email?.message)}
          {...register('email')}
        />
      </Campo>
      <Campo id="alta-contrasena" etiqueta="Contraseña" error={errors.contrasena?.message}>
        <input
          id="alta-contrasena"
          type="password"
          autoComplete="new-password"
          className={CLASES_CONTROL}
          {...ariaDeError('alta-contrasena', errors.contrasena?.message)}
          {...register('contrasena')}
        />
      </Campo>
      <Campo id="alta-rol" etiqueta="Rol" error={errors.rol?.message}>
        <select id="alta-rol" className={CLASES_CONTROL} {...ariaDeError('alta-rol', errors.rol?.message)} {...register('rol')}>
          <option value="">Elegí un rol</option>
          {roles.map((rol) => (
            <option key={rol.codigo} value={rol.codigo}>
              {rol.nombre}
            </option>
          ))}
        </select>
      </Campo>
      {esProfesional && (
        <Campo id="alta-profesional" etiqueta="Profesional" error={errors.profesionalId?.message}>
          <select
            id="alta-profesional"
            className={CLASES_CONTROL}
            {...ariaDeError('alta-profesional', errors.profesionalId?.message)}
            {...register('profesionalId')}
          >
            <option value="">Elegí un profesional</option>
            {profesionales.map((profesional) => (
              <option key={profesional.id} value={profesional.id}>
                {profesional.nombre} {profesional.apellido}
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
          {isSubmitting ? 'Creando…' : 'Crear usuario'}
        </button>
      </div>
    </form>
  )
}
