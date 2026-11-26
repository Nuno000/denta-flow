import { QueryClientProvider } from '@tanstack/react-query'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { crearQueryClient } from '../../lib/queryClient'
import { simularApi } from '../../test/apiSimulada'
import { TURNOS_DEL_1_12 } from '../../test/datosAgenda'
import type { CambioDeTurno } from './api'
import { HistorialDeTurnoDialogo } from './HistorialDeTurnoDialogo'

const TURNO = TURNOS_DEL_1_12[0]

const CAMBIOS: CambioDeTurno[] = [
  {
    id: 10,
    ocurridoEn: '2026-11-26T13:15:00Z', // 10:15 AR
    actorTipo: 'paciente',
    actorEmail: null,
    accion: 'creacion',
    valorAnterior: {},
    valorNuevo: { estado: 'pendiente', origen: 'online', es_sobreturno: 'false' },
  },
  {
    id: 11,
    ocurridoEn: '2026-11-26T14:30:00Z', // 11:30 AR
    actorTipo: 'usuario',
    actorEmail: 'recepcion@example.com',
    accion: 'cambio_estado',
    valorAnterior: { estado: 'confirmado' },
    valorNuevo: { estado: 'asistio' },
  },
  {
    id: 12,
    ocurridoEn: '2026-11-26T15:00:00Z', // 12:00 AR
    actorTipo: 'sistema',
    actorEmail: null,
    accion: 'confirmacion',
    valorAnterior: { estado: 'pendiente' },
    valorNuevo: { estado: 'confirmado' },
  },
]

function abrirDialogo(alCerrar = vi.fn()) {
  return render(
    <QueryClientProvider client={crearQueryClient()}>
      <HistorialDeTurnoDialogo turno={TURNO} alCerrar={alCerrar} />
    </QueryClientProvider>,
  )
}

describe('HistorialDeTurnoDialogo (US-025, spec auditoria-turnos)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('muestra los cambios en orden con hora del consultorio, actor, acción y valores en solo lectura', async () => {
    simularApi({ 'GET /api/turnos/1/historial': { status: 200, cuerpo: CAMBIOS } })
    abrirDialogo()

    const dialogo = await screen.findByRole('dialog', { name: /Historial de cambios/i })
    expect(await within(dialogo).findByText('Creación')).toBeInTheDocument()
    expect(within(dialogo).getByText('Paciente')).toBeInTheDocument()
    expect(within(dialogo).getByText('recepcion@example.com')).toBeInTheDocument()
    expect(within(dialogo).getByText('Sistema')).toBeInTheDocument()
    expect(within(dialogo).getByText('Cambio de estado')).toBeInTheDocument()
    expect(within(dialogo).getByText(/26\/11\/2026.*10:15/)).toBeInTheDocument()
    expect(within(dialogo).getByText(/26\/11\/2026.*11:30/)).toBeInTheDocument()
    expect(within(dialogo).getByText(/Confirmado → Asistió/)).toBeInTheDocument()

    // Solo lectura: sin botones de editar ni borrar
    expect(within(dialogo).queryByRole('button', { name: /editar|borrar|eliminar/i })).not.toBeInTheDocument()
  })

  it('informa cuando el turno todavía no tiene cambios registrados', async () => {
    simularApi({ 'GET /api/turnos/1/historial': { status: 200, cuerpo: [] } })
    abrirDialogo()

    expect(await screen.findByText('Este turno todavía no tiene cambios registrados.')).toBeInTheDocument()
  })

  it('muestra alerta con botón Reintentar si la API falla y permite cerrar', async () => {
    const user = userEvent.setup()
    const alCerrar = vi.fn()
    let fallar = true
    simularApi({
      'GET /api/turnos/1/historial': () => (fallar ? { status: 500 } : { status: 200, cuerpo: CAMBIOS }),
    })
    abrirDialogo(alCerrar)

    const alerta = await screen.findByRole('alert', {}, { timeout: 4000 })
    expect(alerta).toHaveTextContent(/No se pudo cargar el historial/i)

    fallar = false
    await user.click(within(alerta).getByRole('button', { name: 'Reintentar' }))
    expect(await screen.findByText('recepcion@example.com')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Cerrar' }))
    expect(alCerrar).toHaveBeenCalledTimes(1)
  })
})
