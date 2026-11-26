import { useEffect, useState } from 'react'

/**
 * Hora actual (ms) que se actualiza cada {@code intervaloMs}: así una tarjeta abierta antes del inicio del turno
 * habilita "Asistió" y "Ausente" sola cuando llega la hora, sin leer el reloj durante el render.
 */
export function useAhora(intervaloMs = 30_000): number {
  const [ahora, setAhora] = useState(() => Date.now())
  useEffect(() => {
    const id = setInterval(() => setAhora(Date.now()), intervaloMs)
    return () => clearInterval(id)
  }, [intervaloMs])
  return ahora
}
