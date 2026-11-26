import type { ReactNode } from 'react'

interface CampoProps {
  id: string
  etiqueta: string
  error?: string
  children: ReactNode
}

/** Etiqueta, control y mensaje de error asociado (el control lleva `aria-describedby={id + '-error'}`). */
export function Campo({ id, etiqueta, error, children }: CampoProps) {
  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={id} className="text-sm font-medium text-slate-900">
        {etiqueta}
      </label>
      {children}
      {error !== undefined && (
        <p id={`${id}-error`} className="text-sm text-red-800">
          {error}
        </p>
      )}
    </div>
  )
}
