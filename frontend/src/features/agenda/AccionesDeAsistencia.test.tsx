import { QueryClientProvider } from '@tanstack/react-query'
import { act, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { crearQueryClient } from '../../lib/queryClient'
import { TURNOS_DEL_1_12, TURNO_CANCELADO } from '../../test/datosAgenda'
import type { TurnoDeAgenda } from './api'
import { AccionesDeAsistencia } from './AccionesDeAsistencia'

/** El primer turno de los datos de prueba empieza el 1/12 a las 09:00 del consultorio (12:00Z). */
const TURNO: TurnoDeAgenda = TURNOS_DEL_1_12[0]

function mostrar(turno: TurnoDeAgenda) {
  return render(
    <QueryClientProvider client={crearQueryClient()}>
      <AccionesDeAsistencia turno={turno} />
    </QueryClientProvider>,
  )
}

describe('acciones de asistencia (spec "Acciones de asistencia en la agenda")', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] })
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('con el turno ya empezado ofrece las tres acciones habilitadas', () => {
    vi.setSystemTime(new Date('2026-12-01T12:10:00Z'))
    mostrar(TURNO)

    expect(screen.getByRole('group', { name: 'Asistencia' })).toBeInTheDocument()
    for (const nombre of ['Asistió', 'Ausente', 'Ausente con aviso']) {
      expect(screen.getByRole('button', { name: nombre })).toBeEnabled()
    }
  })

  it('justo a la hora de inicio también se habilitan', () => {
    vi.setSystemTime(new Date('2026-12-01T12:00:00Z'))
    mostrar(TURNO)

    expect(screen.getByRole('button', { name: 'Asistió' })).toBeEnabled()
  })

  it('antes del inicio solo se puede marcar ausente con aviso, y dice desde cuándo', () => {
    vi.setSystemTime(new Date('2026-12-01T11:00:00Z'))
    mostrar(TURNO)

    expect(screen.getByRole('button', { name: 'Asistió' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Ausente' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Ausente con aviso' })).toBeEnabled()
    expect(screen.getByText('Disponible desde las 09:00')).toBeInTheDocument()
  })

  it('indica como presionado el estado actual', () => {
    vi.setSystemTime(new Date('2026-12-01T15:00:00Z'))
    mostrar({ ...TURNO, estado: 'ausente' })

    expect(screen.getByRole('button', { name: 'Ausente' })).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByRole('button', { name: 'Asistió' })).toHaveAttribute('aria-pressed', 'false')
  })

  it('no ofrece acciones en un turno cancelado', () => {
    vi.setSystemTime(new Date('2026-12-01T15:00:00Z'))
    mostrar(TURNO_CANCELADO)

    expect(screen.queryByRole('group', { name: 'Asistencia' })).not.toBeInTheDocument()
  })
})

describe('habilitación al llegar la hora', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  it('una tarjeta abierta antes del inicio habilita "Asistió" sola cuando llega la hora', async () => {
    vi.useFakeTimers({ toFake: ['Date', 'setInterval', 'clearInterval'] })
    vi.setSystemTime(new Date('2026-12-01T11:59:50Z'))
    mostrar(TURNO)
    expect(screen.getByRole('button', { name: 'Asistió' })).toBeDisabled()

    await act(async () => {
      vi.advanceTimersByTime(30_000)
    })

    expect(screen.getByRole('button', { name: 'Asistió' })).toBeEnabled()
  })
})
