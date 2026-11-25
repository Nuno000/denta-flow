import type { RouteObject } from 'react-router-dom'
import { IngresarPage } from '../features/auth/IngresarPage'
import { PanelPage } from '../features/auth/PanelPage'
import { RutaProtegida } from '../features/auth/RutaProtegida'
import { UsuariosPage } from '../features/usuarios/UsuariosPage'
import { InicioPage } from './InicioPage'

/** Rutas de la SPA. Cada feature agrega las suyas acá; las del panel van dentro de RutaProtegida. */
export const rutas: RouteObject[] = [
  {
    path: '/',
    element: <InicioPage />,
  },
  {
    path: '/ingresar',
    element: <IngresarPage />,
  },
  {
    path: '/panel',
    element: (
      <RutaProtegida>
        <PanelPage />
      </RutaProtegida>
    ),
  },
  {
    path: '/panel/usuarios',
    element: (
      <RutaProtegida roles={['administrador']}>
        <UsuariosPage />
      </RutaProtegida>
    ),
  },
]
