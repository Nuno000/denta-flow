import type { ReactNode } from 'react'

interface TablaDeRecursosProps {
  etiqueta: string
  columnas: string[]
  children: ReactNode
}

/**
 * Tabla semántica que en el celular se apila como tarjetas por CSS (mismo patrón que usuarios): una sola
 * estructura para lectores de pantalla, tests y todos los anchos.
 */
export function TablaDeRecursos({ etiqueta, columnas, children }: TablaDeRecursosProps) {
  return (
    <table role="table" aria-label={etiqueta} className="w-full text-left text-sm md:table">
      <thead className="hidden border-b border-slate-300 md:table-header-group">
        <tr role="row">
          {columnas.map((columna) => (
            <th key={columna} role="columnheader" scope="col" className="px-3 py-2">
              {columna}
            </th>
          ))}
        </tr>
      </thead>
      <tbody className="flex flex-col gap-3 md:table-row-group">{children}</tbody>
    </table>
  )
}

/** Fila que en el celular es una tarjeta con borde. */
export function FilaDeRecurso({ children }: { children: ReactNode }) {
  return (
    <tr
      role="row"
      className="flex flex-col rounded-lg border border-slate-300 p-3 md:table-row md:rounded-none md:border-0 md:border-b md:p-0"
    >
      {children}
    </tr>
  )
}
