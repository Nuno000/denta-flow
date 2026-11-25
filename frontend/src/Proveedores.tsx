import { QueryClientProvider } from '@tanstack/react-query'
import { useState } from 'react'
import { RouterProvider } from 'react-router-dom'
import type { createBrowserRouter } from 'react-router-dom'
import { ProveedorDeSesion } from './features/auth/ProveedorDeSesion'
import { crearQueryClient } from './lib/queryClient'

type Router = ReturnType<typeof createBrowserRouter>

interface ProveedoresProps {
  router: Router
}

/** Proveedores globales de la SPA: TanStack Query, la sesión del personal y el router. */
export function Proveedores({ router }: ProveedoresProps) {
  const [queryClient] = useState(crearQueryClient)

  return (
    <QueryClientProvider client={queryClient}>
      <ProveedorDeSesion>
        <RouterProvider router={router} />
      </ProveedorDeSesion>
    </QueryClientProvider>
  )
}
