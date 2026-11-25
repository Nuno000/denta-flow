import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { alNoAutorizado, configurarToken } from '../../lib/apiClient'
import { CLAVE_USUARIO_ACTUAL, ingresar, obtenerUsuarioActual, type DatosIngreso } from './api'
import { ContextoSesion, type ValorSesion } from './contextoSesion'
import { borrarSesion, guardarSesion, leerSesion, type Sesion, type UsuarioSesion } from './sesion'

interface ProveedorDeSesionProps {
  children: ReactNode
}

/**
 * Sesión del personal (C-04, design D12): conecta el token guardado con el apiClient, valida al iniciar
 * con GET /api/auth/yo y descarta la sesión ante un 401 (vencida o usuario desactivado). Sin sesión, las
 * rutas protegidas llevan a /ingresar.
 */
export function ProveedorDeSesion({ children }: ProveedorDeSesionProps) {
  const queryClient = useQueryClient()
  const [sesion, setSesion] = useState<Sesion | null>(() => {
    // Antes de que los hijos hagan su primer pedido, el apiClient ya sabe de dónde sacar el token.
    configurarToken(() => leerSesion()?.token ?? null)
    return leerSesion()
  })

  const descartar = useCallback(() => {
    borrarSesion()
    setSesion(null)
    queryClient.clear()
  }, [queryClient])

  useEffect(() => alNoAutorizado(descartar), [descartar])

  const usuarioActual = useQuery({
    queryKey: CLAVE_USUARIO_ACTUAL,
    queryFn: obtenerUsuarioActual,
    enabled: sesion !== null,
    retry: false,
    staleTime: Infinity,
  })

  // El rol y los datos vigentes son los de /api/auth/yo; mientras llegan, los guardados al ingresar.
  const usuario: UsuarioSesion | null = sesion === null ? null : (usuarioActual.data ?? sesion.usuario)

  const ingreso = useMutation({ mutationFn: ingresar })
  const { mutateAsync } = ingreso

  const valor = useMemo<ValorSesion>(
    () => ({
      usuario,
      verificando: sesion !== null && usuarioActual.isPending,
      ingresar: async (datos: DatosIngreso): Promise<UsuarioSesion> => {
        const nueva = await mutateAsync(datos)
        guardarSesion(nueva)
        queryClient.setQueryData(CLAVE_USUARIO_ACTUAL, nueva.usuario)
        setSesion(nueva)
        return nueva.usuario
      },
      cerrarSesion: descartar,
    }),
    [sesion, usuario, usuarioActual.isPending, mutateAsync, queryClient, descartar],
  )

  return <ContextoSesion.Provider value={valor}>{children}</ContextoSesion.Provider>
}
