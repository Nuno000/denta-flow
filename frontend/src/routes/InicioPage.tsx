import { Link } from 'react-router-dom'
import { useSesion } from '../features/auth/contextoSesion'

/**
 * Portada pública. Por ahora solo da acceso al ingreso del personal (spec "Acceso al ingreso desde la
 * portada"); la reserva online de pacientes llega con C-16 y será la acción principal de esta página.
 */
export function InicioPage() {
  const { usuario } = useSesion()

  return (
    <main className="mx-auto flex min-h-screen max-w-xl flex-col justify-center gap-2 p-6">
      <h1 className="text-3xl font-bold">DentaFlow</h1>
      <p className="text-base">Turnos online para tu consultorio odontológico.</p>
      <div className="mt-4">
        <Link
          to={usuario === null ? '/ingresar' : '/panel'}
          className="inline-block rounded-md border border-sky-800 px-4 py-2 font-semibold text-sky-800 hover:bg-sky-50 focus:outline-none focus-visible:ring-2 focus-visible:ring-sky-700"
        >
          {usuario === null ? 'Ingreso del personal' : 'Ir al panel'}
        </Link>
      </div>
    </main>
  )
}
