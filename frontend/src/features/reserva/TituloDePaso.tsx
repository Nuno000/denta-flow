import { useEffect, useRef, type ReactNode } from 'react'

/**
 * Encabezado de cada paso de la reserva. Recibe el foco al aparecer, así quien usa lector de pantalla o teclado
 * sabe que cambió el paso (spec "Pantalla de reserva mobile-first y accesible").
 */
export function TituloDePaso({ children }: { children: ReactNode }) {
  const titulo = useRef<HTMLHeadingElement>(null)

  useEffect(() => {
    titulo.current?.focus()
  }, [])

  return (
    <h2
      ref={titulo}
      tabIndex={-1}
      className="text-xl font-semibold text-slate-900 focus:outline-none focus-visible:ring-2 focus-visible:ring-sky-700"
    >
      {children}
    </h2>
  )
}
