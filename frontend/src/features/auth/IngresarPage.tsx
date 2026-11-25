import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { ErrorApi } from '../../lib/apiClient'
import { useSesion } from './contextoSesion'

const esquema = z.object({
  email: z.string().trim().min(1, 'Ingresá tu email').email('El email no tiene un formato válido'),
  contrasena: z.string().min(1, 'Ingresá tu contraseña'),
})

type DatosFormulario = z.infer<typeof esquema>

interface EstadoNavegacion {
  desde?: string
}

function mensajeDeError(error: unknown): string {
  if (error instanceof ErrorApi && error.status === 401) {
    return 'Email o contraseña incorrectos'
  }
  if (error instanceof ErrorApi && error.status === 429) {
    return 'Hubo demasiados intentos de ingreso. Probá de nuevo en unos minutos.'
  }
  return 'No pudimos conectar con el servidor. Probá de nuevo.'
}

/** Pantalla de ingreso del personal (spec "Pantalla de ingreso"): mobile-first y WCAG 2.1 AA. */
export function IngresarPage() {
  const { usuario, ingresar } = useSesion()
  const navigate = useNavigate()
  const location = useLocation()
  const desde = (location.state as EstadoNavegacion | null)?.desde ?? '/panel'
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<DatosFormulario>({ resolver: zodResolver(esquema), defaultValues: { email: '', contrasena: '' } })

  if (usuario !== null && !isSubmitting) {
    return <Navigate to={desde} replace />
  }

  const enviar = handleSubmit(async (datos) => {
    setErrorGeneral(null)
    try {
      await ingresar(datos)
      navigate(desde, { replace: true })
    } catch (error) {
      setErrorGeneral(mensajeDeError(error))
    }
  })

  return (
    <main className="mx-auto flex min-h-screen w-full max-w-sm flex-col justify-center gap-6 p-6">
      <div>
        <p className="text-sm font-semibold uppercase tracking-wide text-sky-800">DentaFlow</p>
        <h1 className="text-2xl font-bold text-slate-900">Ingresar</h1>
        <p className="mt-1 text-sm text-slate-700">Acceso para el personal del consultorio.</p>
      </div>

      <form onSubmit={enviar} noValidate className="flex flex-col gap-4">
        {errorGeneral !== null && (
          <p role="alert" className="rounded-md border border-red-700 bg-red-50 p-3 text-sm text-red-800">
            {errorGeneral}
          </p>
        )}

        <div className="flex flex-col gap-1">
          <label htmlFor="email" className="text-sm font-medium text-slate-900">
            Email
          </label>
          <input
            id="email"
            type="email"
            autoComplete="username"
            inputMode="email"
            aria-invalid={errors.email ? 'true' : 'false'}
            aria-describedby={errors.email ? 'email-error' : undefined}
            className="rounded-md border border-slate-500 px-3 py-2 text-base text-slate-900 focus:outline-none focus-visible:ring-2 focus-visible:ring-sky-700 aria-[invalid=true]:border-red-700"
            {...register('email')}
          />
          {errors.email && (
            <p id="email-error" className="text-sm text-red-800">
              {errors.email.message}
            </p>
          )}
        </div>

        <div className="flex flex-col gap-1">
          <label htmlFor="contrasena" className="text-sm font-medium text-slate-900">
            Contraseña
          </label>
          <input
            id="contrasena"
            type="password"
            autoComplete="current-password"
            aria-invalid={errors.contrasena ? 'true' : 'false'}
            aria-describedby={errors.contrasena ? 'contrasena-error' : undefined}
            className="rounded-md border border-slate-500 px-3 py-2 text-base text-slate-900 focus:outline-none focus-visible:ring-2 focus-visible:ring-sky-700 aria-[invalid=true]:border-red-700"
            {...register('contrasena')}
          />
          {errors.contrasena && (
            <p id="contrasena-error" className="text-sm text-red-800">
              {errors.contrasena.message}
            </p>
          )}
        </div>

        <button
          type="submit"
          disabled={isSubmitting}
          className="rounded-md bg-sky-800 px-4 py-2 font-semibold text-white hover:bg-sky-900 focus:outline-none focus-visible:ring-2 focus-visible:ring-sky-700 focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:bg-slate-500"
        >
          {isSubmitting ? 'Ingresando…' : 'Ingresar'}
        </button>
      </form>
    </main>
  )
}
