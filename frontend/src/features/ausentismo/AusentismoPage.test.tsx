import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { renderizarEn, sesionDe, simularApi, type RespuestaSimulada } from '../../test/apiSimulada'
import type { ReporteDeAusentismo } from './api'

const REPORTE_CON_DATOS: ReporteDeAusentismo = {
  desde: '2026-11-01',
  hasta: '2026-11-26',
  profesionalId: null,
  minimoAusencias: 2,
  general: {
    totalEvaluados: 10,
    asistidos: 7,
    ausentesSinAviso: 2,
    ausentesConAviso: 1,
    totalAusentes: 3,
    tasaAusentismo: 30.0,
  },
  porProfesional: [
    {
      profesionalId: 1,
      nombre: 'Laura',
      apellido: 'Gómez',
      totalEvaluados: 6,
      asistidos: 4,
      ausentesSinAviso: 2,
      ausentesConAviso: 0,
      totalAusentes: 2,
      tasaAusentismo: 33.3,
    },
    {
      profesionalId: 2,
      nombre: 'Martín',
      apellido: 'Silva',
      totalEvaluados: 4,
      asistidos: 3,
      ausentesSinAviso: 0,
      ausentesConAviso: 1,
      totalAusentes: 1,
      tasaAusentismo: 25.0,
    },
  ],
  porFranja: [
    {
      codigo: 'manana',
      etiqueta: 'Mañana (antes de las 12:00)',
      totalEvaluados: 4,
      asistidos: 2,
      ausentesSinAviso: 2,
      ausentesConAviso: 0,
      totalAusentes: 2,
      tasaAusentismo: 50.0,
    },
    {
      codigo: 'tarde',
      etiqueta: 'Tarde (12:00 a 17:59)',
      totalEvaluados: 4,
      asistidos: 3,
      ausentesSinAviso: 0,
      ausentesConAviso: 1,
      totalAusentes: 1,
      tasaAusentismo: 25.0,
    },
    {
      codigo: 'noche',
      etiqueta: 'Noche (desde las 18:00)',
      totalEvaluados: 2,
      asistidos: 2,
      ausentesSinAviso: 0,
      ausentesConAviso: 0,
      totalAusentes: 0,
      tasaAusentismo: 0.0,
    },
  ],
  porDiaSemana: [
    {
      dia: 1,
      etiqueta: 'Lunes',
      totalEvaluados: 5,
      asistidos: 3,
      ausentesSinAviso: 2,
      ausentesConAviso: 0,
      totalAusentes: 2,
      tasaAusentismo: 40.0,
    },
    {
      dia: 3,
      etiqueta: 'Miércoles',
      totalEvaluados: 5,
      asistidos: 4,
      ausentesSinAviso: 0,
      ausentesConAviso: 1,
      totalAusentes: 1,
      tasaAusentismo: 20.0,
    },
  ],
  pacientesReiterados: [
    {
      pacienteId: 10,
      nombre: 'Ana',
      apellido: 'López',
      totalEvaluados: 3,
      asistidos: 1,
      ausentesSinAviso: 1,
      ausentesConAviso: 1,
      totalAusentes: 2,
      tasaAusentismo: 66.7,
    },
  ],
}

const REPORTE_VACIO: ReporteDeAusentismo = {
  desde: '2026-11-01',
  hasta: '2026-11-26',
  profesionalId: null,
  minimoAusencias: 2,
  general: {
    totalEvaluados: 0,
    asistidos: 0,
    ausentesSinAviso: 0,
    ausentesConAviso: 0,
    totalAusentes: 0,
    tasaAusentismo: 0.0,
  },
  porProfesional: [],
  porFranja: [
    {
      codigo: 'manana',
      etiqueta: 'Mañana (antes de las 12:00)',
      totalEvaluados: 0,
      asistidos: 0,
      ausentesSinAviso: 0,
      ausentesConAviso: 0,
      totalAusentes: 0,
      tasaAusentismo: 0.0,
    },
  ],
  porDiaSemana: [],
  pacientesReiterados: [],
}

function apiDeAusentismo(
  rol: 'administrador' | 'recepcion' | 'profesional',
  respuesta: RespuestaSimulada,
) {
  return {
    'GET /api/auth/yo': { status: 200, cuerpo: sesionDe(rol).usuario },
    'GET /api/profesionales': {
      status: 200,
      cuerpo: [
        { id: 1, nombre: 'Laura', apellido: 'Gómez', email: 'laura@example.com', activo: true, prestaciones: [] },
        { id: 2, nombre: 'Martín', apellido: 'Silva', email: 'martin@example.com', activo: true, prestaciones: [] },
      ],
    },
    'GET /api/reportes/ausentismo': respuesta,
  }
}

describe('AusentismoPage (C-23, US-031)', () => {
  afterEach(() => {
    sessionStorage.clear()
    vi.unstubAllGlobals()
  })

  it('administrador ve tarjetas resumen, franjas, días, profesionales y pacientes reiterados', async () => {
    simularApi(apiDeAusentismo('administrador', { status: 200, cuerpo: REPORTE_CON_DATOS }))
    renderizarEn(
      '/panel/ausentismo?desde=2026-11-01&hasta=2026-11-26&minimoAusencias=2',
      sesionDe('administrador'),
    )

    expect(await screen.findByRole('heading', { level: 1, name: 'Panel de ausentismo' })).toBeInTheDocument()
    expect(screen.getByLabelText('Profesional')).toBeInTheDocument()
    expect(await screen.findByText('30,0 %')).toBeInTheDocument()
    expect(screen.getByText('2 sin aviso · 1 con aviso')).toBeInTheDocument()

    expect(screen.getByText('Mañana (antes de las 12:00)')).toBeInTheDocument()
    expect(screen.getByText('Lunes')).toBeInTheDocument()

    const tablaProfesionales = screen.getByRole('table', { name: 'Ausentismo por profesional' })
    expect(within(tablaProfesionales).getByText('Laura Gómez')).toBeInTheDocument()
    expect(within(tablaProfesionales).getByText('33,3 %')).toBeInTheDocument()

    const tablaPacientes = screen.getByRole('table', { name: 'Pacientes con ausencias reiteradas' })
    expect(within(tablaPacientes).getByText('Ana López')).toBeInTheDocument()
    expect(within(tablaPacientes).getByText('66,7 %')).toBeInTheDocument()
  })

  it('profesional ve Mi ausentismo sin filtro ni tabla comparativa de otros profesionales', async () => {
    simularApi(
      apiDeAusentismo('profesional', {
        status: 200,
        cuerpo: { ...REPORTE_CON_DATOS, profesionalId: 1, porProfesional: [REPORTE_CON_DATOS.porProfesional[0]] },
      }),
    )
    renderizarEn(
      '/panel/ausentismo?desde=2026-11-01&hasta=2026-11-26&minimoAusencias=2&profesionalId=2',
      sesionDe('profesional'),
    )

    expect(await screen.findByRole('heading', { level: 1, name: 'Mi ausentismo' })).toBeInTheDocument()
    expect(await screen.findByText('30,0 %')).toBeInTheDocument()
    expect(screen.queryByLabelText('Profesional')).not.toBeInTheDocument()
    expect(screen.queryByRole('table', { name: 'Ausentismo por profesional' })).not.toBeInTheDocument()
  })

  it('muestra estado vacío claro con enlace a la agenda cuando no hay turnos evaluados', async () => {
    simularApi(apiDeAusentismo('recepcion', { status: 200, cuerpo: REPORTE_VACIO }))
    renderizarEn(
      '/panel/ausentismo?desde=2026-11-01&hasta=2026-11-26&minimoAusencias=2',
      sesionDe('recepcion'),
    )

    expect(
      await screen.findByText(/No hay turnos con asistencia registrada en este período/i),
    ).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ir a la agenda' })).toHaveAttribute('href', '/panel/agenda')
  })

  it('muestra alerta con botón Reintentar ante error de red o servidor y vuelve a cargar', async () => {
    let fallar = true
    const fetchMock = simularApi({
      'GET /api/auth/yo': { status: 200, cuerpo: sesionDe('administrador').usuario },
      'GET /api/profesionales': { status: 200, cuerpo: [] },
      'GET /api/reportes/ausentismo': () =>
        fallar
          ? { status: 500, cuerpo: { title: 'Error', detail: 'Fallo temporal' } }
          : { status: 200, cuerpo: REPORTE_CON_DATOS },
    })
    renderizarEn(
      '/panel/ausentismo?desde=2026-11-01&hasta=2026-11-26&minimoAusencias=2',
      sesionDe('administrador'),
    )

    const alerta = await screen.findByRole('alert', {}, { timeout: 4000 })
    expect(alerta).toHaveTextContent('No se pudieron cargar los indicadores de ausentismo.')
    const botonReintentar = within(alerta).getByRole('button', { name: 'Reintentar' })

    const llamadasAntes = fetchMock.mock.calls.length
    fallar = false
    await userEvent.click(botonReintentar)

    expect(await screen.findByText('30,0 %')).toBeInTheDocument()
    expect(fetchMock.mock.calls.length).toBeGreaterThan(llamadasAntes)
  })

  it('muestra aviso de permiso sin botón Reintentar ante 403', async () => {
    simularApi(
      apiDeAusentismo('profesional', {
        status: 403,
        cuerpo: { title: 'Sin permiso', detail: 'Tu usuario no está vinculado a un profesional' },
      }),
    )
    renderizarEn(
      '/panel/ausentismo?desde=2026-11-01&hasta=2026-11-26&minimoAusencias=2',
      sesionDe('profesional'),
    )

    const alerta = await screen.findByRole('alert')
    expect(alerta).toHaveTextContent('No tenés permiso para consultar estos indicadores.')
    expect(within(alerta).queryByRole('button', { name: 'Reintentar' })).not.toBeInTheDocument()
  })

  it('muestra error de validación cuando la fecha hasta es anterior a desde', async () => {
    simularApi(apiDeAusentismo('administrador', { status: 200, cuerpo: REPORTE_CON_DATOS }))
    renderizarEn(
      '/panel/ausentismo?desde=2026-11-26&hasta=2026-11-01&minimoAusencias=2',
      sesionDe('administrador'),
    )

    expect(await screen.findByRole('alert')).toHaveTextContent(/anterior/i)
  })
})
