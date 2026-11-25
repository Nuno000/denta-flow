import type { ReactNode } from 'react'
import { Link, Navigate, useLocation } from 'react-router-dom'
import { useSesion } from './contextoSesion'
import type { Rol } from './sesion'

interface RutaProtegidaProps {
  /** Roles que pueden ver la ruta; sin indicar, cualquier usuario ingresado. */
  roles?: Rol[]
  children: ReactNode
}

/**
 * Protección de usabilidad de las rutas del panel (spec "Rutas del panel protegidas por rol en el
 * frontend"): sin sesión lleva a /ingresar recordando la ruta; con otro rol muestra un aviso. La
 * autorización real es la del backend.
 */
export function RutaProtegida({ roles, children }: RutaProtegidaProps) {
  const { usuario, verificando } = useSesion()
  const location = useLocation()

  if (usuario === null) {
    return <Navigate to="/ingresar" replace state={{ desde: location.pathname }} />
  }
  if (verificando) {
    return (
      <p role="status" className="p-6 text-slate-700">
        Cargando…
      </p>
    )
  }
  if (roles !== undefined && !roles.includes(usuario.rol)) {
    return (
      <main className="mx-auto flex max-w-xl flex-col gap-4 p-6">
        <h1 className="text-xl font-bold text-slate-900">No tenés permiso para esta sección</h1>
        <p className="text-slate-700">Si necesitás acceder, pedíselo a quien administra el consultorio.</p>
        <Link to="/panel" className="font-semibold text-sky-800 underline">
          Volver al panel
        </Link>
      </main>
    )
  }
  return <>{children}</>
}
