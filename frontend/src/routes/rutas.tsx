import { Navigate, type RouteObject } from 'react-router-dom'
import { AgendaPage } from '../features/agenda/AgendaPage'
import { CargaDeTurnoPage } from '../features/agenda/CargaDeTurnoPage'
import { IngresarPage } from '../features/auth/IngresarPage'
import { PanelPage } from '../features/auth/PanelPage'
import { RutaProtegida } from '../features/auth/RutaProtegida'
import { PrestacionesTab } from '../features/recursos/PrestacionesTab'
import { ProfesionalesTab } from '../features/recursos/ProfesionalesTab'
import { RecursosPage } from '../features/recursos/RecursosPage'
import { SillonesTab } from '../features/recursos/SillonesTab'
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
    // C-11: los tres roles internos; el profesional solo ve sus turnos (lo verifica el backend).
    path: '/panel/agenda',
    element: (
      <RutaProtegida>
        <AgendaPage />
      </RutaProtegida>
    ),
  },
  {
    // C-12: la carga manual de turnos y sobreturnos es de recepción y del administrador (lo verifica el backend).
    path: '/panel/agenda/nuevo-turno',
    element: (
      <RutaProtegida roles={['administrador', 'recepcion']}>
        <CargaDeTurnoPage />
      </RutaProtegida>
    ),
  },
  {
    // C-05: lectura para los tres roles internos; las acciones de edición son solo del administrador.
    path: '/panel/recursos',
    element: (
      <RutaProtegida>
        <RecursosPage />
      </RutaProtegida>
    ),
    children: [
      { index: true, element: <Navigate to="profesionales" replace /> },
      { path: 'profesionales', element: <ProfesionalesTab /> },
      { path: 'sillones', element: <SillonesTab /> },
      { path: 'prestaciones', element: <PrestacionesTab /> },
    ],
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
