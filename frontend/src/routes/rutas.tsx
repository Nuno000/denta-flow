import { Navigate, type RouteObject } from 'react-router-dom'
import { AgendaPage } from '../features/agenda/AgendaPage'
import { CargaDeTurnoPage } from '../features/agenda/CargaDeTurnoPage'
import { IngresarPage } from '../features/auth/IngresarPage'
import { PanelPage } from '../features/auth/PanelPage'
import { RutaProtegida } from '../features/auth/RutaProtegida'
import { PrestacionesTab } from '../features/recursos/PrestacionesTab'
import { ProfesionalesTab } from '../features/recursos/ProfesionalesTab'
import { RecursosPage } from '../features/recursos/RecursosPage'
import { ReservaPage } from '../features/reserva/ReservaPage'
import { SillonesTab } from '../features/recursos/SillonesTab'
import { CancelarTurnoPage } from '../features/turno-paciente/CancelarTurnoPage'
import { ConfirmarTurnoPage } from '../features/turno-paciente/ConfirmarTurnoPage'
import { ReprogramarTurnoPage } from '../features/turno-paciente/ReprogramarTurnoPage'
import { UsuariosPage } from '../features/usuarios/UsuariosPage'
import { InicioPage } from './InicioPage'

/** Rutas de la SPA. Cada feature agrega las suyas acá; las del panel van dentro de RutaProtegida. */
export const rutas: RouteObject[] = [
  {
    path: '/',
    element: <InicioPage />,
  },
  {
    // C-16: reserva online pública, sin sesión (RN-DI-03).
    path: '/reservar',
    element: <ReservaPage />,
  },
  // C-17: gestión del turno con el token del email, pública y sin sesión.
  { path: '/turno/:token/confirmar', element: <ConfirmarTurnoPage /> },
  { path: '/turno/:token/cancelar', element: <CancelarTurnoPage /> },
  { path: '/turno/:token/reprogramar', element: <ReprogramarTurnoPage /> },
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
