import { QueryClient } from '@tanstack/react-query'
import { ErrorApi } from './apiClient'

/** Un error 4xx (datos inválidos, sin permiso, no encontrado) no se arregla reintentando. */
function reintentar(intentos: number, error: unknown): boolean {
  if (error instanceof ErrorApi && error.status >= 400 && error.status < 500) {
    return false
  }
  return intentos < 1
}

/** Cliente de TanStack Query compartido por toda la SPA. */
export function crearQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: reintentar,
        refetchOnWindowFocus: false,
      },
    },
  })
}
