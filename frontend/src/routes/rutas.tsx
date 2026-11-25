import type { RouteObject } from 'react-router-dom'
import { InicioPage } from './InicioPage'

/** Rutas de la SPA. Cada feature agrega las suyas acá. */
export const rutas: RouteObject[] = [
  {
    path: '/',
    element: <InicioPage />,
  },
]
