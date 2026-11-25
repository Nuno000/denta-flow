import { BarraDelPanel } from './BarraDelPanel'

/** Destino mínimo tras el ingreso (C-04). C-11 lo reemplaza por la agenda interna. */
export function PanelPage() {
  return (
    <>
      <BarraDelPanel />
      <main className="mx-auto flex max-w-5xl flex-col gap-4 p-4 sm:p-6">
        <h1 className="text-2xl font-bold text-slate-900">Panel</h1>
        <p className="text-slate-700">La agenda del consultorio llega en los próximos cambios.</p>
      </main>
    </>
  )
}
