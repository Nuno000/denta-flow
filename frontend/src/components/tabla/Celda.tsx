import type { ReactNode } from 'react'

/**
 * Celda de una tabla que en el celular se apila como tarjeta: muestra su encabezado antes del valor. Así
 * lectores de pantalla y tests ven una sola tabla semántica en todos los tamaños.
 */
export function Celda({ etiqueta, children }: { etiqueta: string; children: ReactNode }) {
  return (
    <td
      role="cell"
      data-etiqueta={etiqueta}
      className="flex gap-2 px-1 py-0.5 before:min-w-28 before:font-medium before:text-slate-700 before:content-[attr(data-etiqueta)] md:table-cell md:px-3 md:py-2 md:before:content-none"
    >
      {children}
    </td>
  )
}
