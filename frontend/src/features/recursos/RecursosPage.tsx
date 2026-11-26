import { NavLink, Outlet } from 'react-router-dom'
import { BarraDelPanel } from '../auth/BarraDelPanel'

const PESTANAS = [
  { ruta: 'profesionales', nombre: 'Profesionales' },
  { ruta: 'sillones', nombre: 'Sillones' },
  { ruta: 'prestaciones', nombre: 'Prestaciones' },
]

function clasesDePestana({ isActive }: { isActive: boolean }): string {
  const base =
    '-mb-px border-b-2 px-3 py-2 text-sm font-medium focus:outline-none focus-visible:ring-2 focus-visible:ring-sky-700'
  return isActive
    ? `${base} border-sky-800 font-semibold text-sky-800`
    : `${base} border-transparent text-slate-700 hover:border-slate-400 hover:text-slate-900`
}

/**
 * Sección Recursos del panel (US-001, spec "Sección Recursos del panel", design D6): una pestaña por tipo
 * de recurso, cada una con su propia URL. Las pestañas son enlaces, así el botón atrás y los lectores de
 * pantalla funcionan sin estado extra.
 */
export function RecursosPage() {
  return (
    <>
      <BarraDelPanel />
      <main className="mx-auto flex max-w-5xl flex-col gap-4 p-4 sm:p-6">
        <h1 className="text-2xl font-bold text-slate-900">Recursos</h1>
        <nav aria-label="Tipos de recurso" className="flex flex-wrap gap-1 border-b border-slate-300">
          {PESTANAS.map((pestana) => (
            <NavLink key={pestana.ruta} to={pestana.ruta} className={clasesDePestana}>
              {pestana.nombre}
            </NavLink>
          ))}
        </nav>
        <Outlet />
      </main>
    </>
  )
}
