import { useState } from 'react'
import type { FieldValues, Path, UseFormSetError } from 'react-hook-form'
import { leerErrorDeApi } from '../../lib/erroresApi'
import { useSesion } from '../auth/contextoSesion'

/** Solo el administrador ve las acciones de edición; el backend igual verifica el rol (design D6). */
export function useEsAdministrador(): boolean {
  return useSesion().usuario?.rol === 'administrador'
}

/** Aviso de éxito (anunciado) y error de una acción de fila, compartidos por las tres pestañas (design D7). */
export function useAvisos() {
  const [aviso, setAviso] = useState('')
  const [error, setError] = useState<string | null>(null)
  return {
    aviso,
    error,
    anunciar: (mensaje: string) => {
      setError(null)
      setAviso(mensaje)
    },
    fallar: (causa: unknown) => {
      setAviso('')
      setError(leerErrorDeApi(causa).general)
    },
  }
}

/**
 * Lleva un error de la API al formulario: los errores por campo de Bean Validation a su campo, y un
 * conflicto (409) al campo que nombra; si no nombra ninguno, devuelve el mensaje para mostrarlo arriba.
 */
export function aplicarErrorDeApi<T extends FieldValues>(
  causa: unknown,
  setError: UseFormSetError<T>,
  camposPorPalabra: Record<string, Path<T>>,
): string | null {
  const { campos, general } = leerErrorDeApi(causa)
  const conocidos = Object.values(camposPorPalabra)
  const conCampo = conocidos.filter((campo) => campos[campo] !== undefined)
  if (conCampo.length > 0) {
    conCampo.forEach((campo) => setError(campo, { message: campos[campo] }))
    return null
  }
  const palabra = Object.keys(camposPorPalabra).find((clave) => general.toLowerCase().includes(clave))
  if (palabra !== undefined) {
    setError(camposPorPalabra[palabra], { message: general }, { shouldFocus: true })
    return null
  }
  return general
}
