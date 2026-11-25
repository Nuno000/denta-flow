import { QueryClient } from '@tanstack/react-query'

/** Cliente de TanStack Query compartido por toda la SPA. */
export function crearQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: 1,
        refetchOnWindowFocus: false,
      },
    },
  })
}
