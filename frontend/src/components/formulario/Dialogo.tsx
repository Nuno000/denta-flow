import { useEffect, useId, useRef, useState, type ReactNode } from 'react'

interface DialogoProps {
  titulo: string
  alCerrar: () => void
  children: ReactNode
}

/**
 * Diálogo modal accesible con el `<dialog>` nativo (design D13): nombre accesible por su título, `Esc`
 * para cerrar y foco devuelto al elemento que lo abrió. Si el navegador no implementa `showModal` (p. ej.
 * jsdom), lo muestra como diálogo abierto no modal. Se monta solo mientras está abierto.
 */
export function Dialogo({ titulo, alCerrar, children }: DialogoProps) {
  const ref = useRef<HTMLDialogElement>(null)
  const idTitulo = useId()
  // Quién abrió el diálogo, leído una sola vez al montar. En StrictMode el efecto se repite con el modal ya
  // abierto y document.activeElement ya es un control del diálogo: leerlo ahí perdía el foco al cerrar (C-21).
  const [anterior] = useState(() => (document.activeElement instanceof HTMLElement ? document.activeElement : null))

  useEffect(() => {
    const dialogo = ref.current
    if (dialogo !== null && !dialogo.open) {
      if (typeof dialogo.showModal === 'function') {
        dialogo.showModal()
      } else {
        dialogo.setAttribute('open', '')
      }
    }
    dialogo?.querySelector<HTMLElement>('input, select, button')?.focus()
    return () => {
      anterior?.focus()
    }
  }, [anterior])

  return (
    <dialog
      ref={ref}
      aria-labelledby={idTitulo}
      onCancel={(evento) => {
        evento.preventDefault()
        alCerrar()
      }}
      onKeyDown={(evento) => {
        if (evento.key === 'Escape') {
          alCerrar()
        }
      }}
      className="m-auto w-[calc(100%-2rem)] max-w-md rounded-lg p-0 shadow-xl backdrop:bg-slate-900/50"
    >
      <div className="flex flex-col gap-4 p-5">
        <h2 id={idTitulo} className="text-lg font-bold text-slate-900">
          {titulo}
        </h2>
        {children}
      </div>
    </dialog>
  )
}
