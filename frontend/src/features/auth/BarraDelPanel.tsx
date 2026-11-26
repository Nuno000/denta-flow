import { Link, NavLink } from 'react-router-dom'
import { useSesion } from './contextoSesion'
import { NOMBRES_DE_ROL, type Rol } from './sesion'

interface Seccion {
  ruta: string
  nombre: string
  /** Roles que ven la sección; sin indicar, todos los roles internos. */
  roles?: Rol[]
  /** Si la sección tiene sub-rutas (pestañas), queda marcada como actual en todas ellas. */
  conSubrutas?: boolean
}

/**
 * Secciones del panel interno (spec "Menú del panel según el rol"). Los changes que agreguen pantallas
 * suman su entrada acá; el backend igual verifica el rol en cada endpoint.
 */
const SECCIONES: Seccion[] = [
  { ruta: '/panel', nombre: 'Panel' },
  { ruta: '/panel/agenda', nombre: 'Agenda' },
  { ruta: '/panel/lista-espera', nombre: 'Lista de espera', roles: ['administrador', 'recepcion'] },
  { ruta: '/panel/recursos', nombre: 'Recursos', conSubrutas: true },
  { ruta: '/panel/usuarios', nombre: 'Usuarios', roles: ['administrador'] },
]

function clasesDeEnlace({ isActive }: { isActive: boolean }): string {
  const base =
    'rounded-md px-3 py-1.5 text-sm focus:outline-none focus-visible:ring-2 focus-visible:ring-sky-700'
  return isActive
    ? `${base} bg-sky-50 font-semibold text-sky-800`
    : `${base} font-medium text-slate-700 hover:bg-slate-100 hover:text-slate-900`
}

/** Encabezado del panel interno: menú de secciones, quién está ingresado, su rol y el cierre de sesión. */
export function BarraDelPanel() {
  const { usuario, cerrarSesion } = useSesion()
  if (usuario === null) {
    return null
  }
  const secciones = SECCIONES.filter((seccion) => seccion.roles === undefined || seccion.roles.includes(usuario.rol))

  return (
    <header className="border-b border-slate-300 bg-white">
      <div className="mx-auto flex max-w-5xl flex-wrap items-center justify-between gap-3 p-4">
        <div className="flex flex-wrap items-center gap-x-5 gap-y-2">
          <Link to="/panel" className="font-bold text-sky-800">
            DentaFlow
          </Link>
          <nav aria-label="Secciones del panel" className="flex flex-wrap gap-1">
            {secciones.map((seccion) => (
              <NavLink key={seccion.ruta} to={seccion.ruta} end={seccion.conSubrutas !== true} className={clasesDeEnlace}>
                {seccion.nombre}
              </NavLink>
            ))}
          </nav>
        </div>
        <div className="flex flex-wrap items-center gap-3 text-sm">
          <span className="text-slate-900">{usuario.email}</span>
          <span className="rounded-full bg-slate-100 px-2 py-0.5 text-slate-800">{NOMBRES_DE_ROL[usuario.rol]}</span>
          <button
            type="button"
            onClick={cerrarSesion}
            className="rounded-md border border-slate-500 px-3 py-1 font-medium text-slate-900 hover:bg-slate-100 focus:outline-none focus-visible:ring-2 focus-visible:ring-sky-700"
          >
            Cerrar sesión
          </button>
        </div>
      </div>
    </header>
  )
}
