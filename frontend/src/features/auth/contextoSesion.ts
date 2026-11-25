import { createContext, useContext } from 'react'
import type { DatosIngreso } from './api'
import type { UsuarioSesion } from './sesion'

export interface ValorSesion {
  /** Usuario de la sesión, o null si no hay nadie ingresado. */
  usuario: UsuarioSesion | null
  /** true mientras se valida con la API un token que ya estaba guardado. */
  verificando: boolean
  ingresar: (datos: DatosIngreso) => Promise<UsuarioSesion>
  cerrarSesion: () => void
}

export const ContextoSesion = createContext<ValorSesion | null>(null)

export function useSesion(): ValorSesion {
  const valor = useContext(ContextoSesion)
  if (valor === null) {
    throw new Error('useSesion debe usarse dentro de ProveedorDeSesion')
  }
  return valor
}
