import { useIsFetching } from '@tanstack/react-query'
import { useEffect, useState } from 'react'

export const MENSAJE_ARRANQUE = 'Estamos despertando el sistema, puede tardar hasta un minuto.'

interface AvisoDeArranqueProps {
  /** Cuánto esperar una respuesta antes de avisar. */
  demoraMs?: number
}

/**
 * Aviso global para el arranque en frío de la API (C-19, design D5): si alguna consulta tarda más de
 * `demoraMs`, explica que el sistema está despertando. No bloquea la pantalla ni borra lo cargado.
 */
export function AvisoDeArranque({ demoraMs = 4000 }: AvisoDeArranqueProps) {
  const consultando = useIsFetching() > 0
  const [demorado, setDemorado] = useState(false)

  useEffect(() => {
    if (!consultando) {
      return undefined
    }
    const temporizador = setTimeout(() => setDemorado(true), demoraMs)
    // Al terminar las consultas se rearma el aviso para la próxima espera larga.
    return () => {
      clearTimeout(temporizador)
      setDemorado(false)
    }
  }, [consultando, demoraMs])

  if (!consultando || !demorado) {
    return null
  }
  return (
    <p
      role="status"
      className="fixed inset-x-4 bottom-4 z-50 mx-auto max-w-md rounded-md bg-slate-900 px-4 py-3 text-center text-sm text-white shadow-lg"
    >
      {MENSAJE_ARRANQUE}
    </p>
  )
}
