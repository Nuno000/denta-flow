import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { pedidos, renderizarEn, simularApi, type RespuestaSimulada } from '../../test/apiSimulada'
import type { SolicitudReserva } from './reservaApi'

const AVISO = { texto: 'Usamos tus datos solo para gestionar tus turnos.', leyenda: 'Leí y acepto el aviso de privacidad' }
const HUECOS = [
  { inicio: '2026-12-02T10:00:00-03:00', fin: '2026-12-02T10:30:00-03:00', profesionalId: null },
  { inicio: '2026-12-02T10:15:00-03:00', fin: '2026-12-02T10:45:00-03:00', profesionalId: null },
]
const CONFIRMADA = {
  inicio: '2026-12-02T10:00:00-03:00',
  fin: '2026-12-02T10:30:00-03:00',
  prestacion: 'Limpieza',
  profesional: 'Ana Pérez',
  estado: 'pendiente',
}

type Reservar = RespuestaSimulada | ((cuerpo: unknown) => RespuestaSimulada)

function api(reservar: Reservar = { status: 201, cuerpo: CONFIRMADA }, huecos: () => unknown[] = () => HUECOS) {
  return simularApi({
    'GET /api/publico/prestaciones': { status: 200, cuerpo: [{ id: 2, nombre: 'Limpieza', duracionMinutos: 30 }] },
    'GET /api/publico/profesionales': { status: 200, cuerpo: [{ id: 3, nombre: 'Ana', apellido: 'Pérez' }] },
    'GET /api/publico/huecos': () => ({ status: 200, cuerpo: huecos() }),
    'GET /api/publico/contacto': { status: 200, cuerpo: { contacto: 'Llamanos al 0261 555-0000' } },
    'GET /api/publico/aviso-privacidad': { status: 200, cuerpo: AVISO },
    'POST /api/publico/reservas': reservar,
  })
}

async function hastaElHorario(user: ReturnType<typeof userEvent.setup>) {
  await user.click(await screen.findByRole('button', { name: 'Limpieza (30 min)' }))
  await user.click(await screen.findByRole('button', { name: 'Cualquier profesional' }))
  const dias = await screen.findByRole('group', { name: 'Días' })
  await user.click(within(dias).getAllByRole('button')[0])
}

async function completarDatos(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText('DNI'), '30111222')
  await user.type(screen.getByLabelText('Nombre'), 'Carla')
  await user.type(screen.getByLabelText('Apellido'), 'Ejemplo')
  await user.type(screen.getByLabelText('Teléfono'), '2615550000')
  await user.type(screen.getByLabelText('Email'), 'carla@example.com')
  await user.click(await screen.findByLabelText(AVISO.leyenda))
}

describe('pantalla de reserva /reservar (US-010, spec "Pantalla de reserva mobile-first y accesible")', () => {
  afterEach(() => {
    sessionStorage.clear()
    vi.unstubAllGlobals()
  })

  it('reserva con "Cualquier profesional" de punta a punta y muestra la confirmación', async () => {
    const simulada = api()
    const user = userEvent.setup()
    renderizarEn('/reservar')

    expect(await screen.findByRole('heading', { level: 1, name: 'Reservá tu turno' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 2, name: 'Elegí la prestación' })).toHaveFocus()
    await user.click(await screen.findByRole('button', { name: 'Limpieza (30 min)' }))

    expect(await screen.findByRole('heading', { level: 2, name: 'Elegí el profesional' })).toHaveFocus()
    expect(screen.getAllByRole('button').map((b) => b.textContent)).toContain('Ana Pérez')
    await user.click(screen.getByRole('button', { name: 'Cualquier profesional' }))

    expect(await screen.findByRole('heading', { level: 2, name: 'Elegí día y horario' })).toHaveFocus()
    const dias = screen.getByRole('group', { name: 'Días' })
    expect(within(dias).getAllByRole('button')).toHaveLength(14)
    await user.click(within(dias).getAllByRole('button')[0])
    await user.click(await screen.findByRole('button', { name: '10:00' }))

    expect(await screen.findByRole('heading', { level: 2, name: 'Tus datos' })).toHaveFocus()
    await completarDatos(user)
    await user.click(screen.getByRole('button', { name: 'Reservar turno' }))

    const listo = await screen.findByRole('heading', { level: 2, name: '¡Listo! Reservaste tu turno' })
    expect(listo).toHaveFocus()
    expect(screen.getByText(/02\/12\/2026 10:00/)).toBeInTheDocument()
    expect(screen.getByText(/Ana Pérez/)).toBeInTheDocument()
    expect(screen.getByText(/pendiente hasta que lo confirmes/)).toBeInTheDocument()

    const post = simulada.mock.calls.find(([, init]) => init?.method === 'POST')
    const cuerpo = JSON.parse(String(post?.[1]?.body)) as SolicitudReserva
    expect(cuerpo).toEqual({
      prestacionId: 2,
      profesionalId: null,
      inicio: '2026-12-02T10:00:00-03:00',
      paciente: { dni: '30111222', nombre: 'Carla', apellido: 'Ejemplo', telefono: '2615550000', email: 'carla@example.com' },
      aceptaAvisoPrivacidad: true,
    })
  })

  it('con un profesional elegido pide sus huecos y lo manda en la reserva', async () => {
    const simulada = api()
    const user = userEvent.setup()
    renderizarEn('/reservar')

    await user.click(await screen.findByRole('button', { name: 'Limpieza (30 min)' }))
    await user.click(await screen.findByRole('button', { name: 'Ana Pérez' }))
    const dias = await screen.findByRole('group', { name: 'Días' })
    await user.click(within(dias).getAllByRole('button')[0])
    await user.click(await screen.findByRole('button', { name: '10:15' }))
    await completarDatos(user)
    await user.click(screen.getByRole('button', { name: 'Reservar turno' }))

    await screen.findByRole('heading', { level: 2, name: '¡Listo! Reservaste tu turno' })
    const urlsDeHuecos = simulada.mock.calls.map(([url]) => String(url)).filter((u) => u.includes('/huecos'))
    expect(urlsDeHuecos.every((u) => u.includes('profesionalId=3'))).toBe(true)
    const post = simulada.mock.calls.find(([, init]) => init?.method === 'POST')
    expect(JSON.parse(String(post?.[1]?.body))).toMatchObject({ profesionalId: 3, inicio: '2026-12-02T10:15:00-03:00' })
  })

  it('"Volver" conserva lo elegido', async () => {
    api()
    const user = userEvent.setup()
    renderizarEn('/reservar')
    await hastaElHorario(user)
    await user.click(await screen.findByRole('button', { name: '10:00' }))
    await user.type(screen.getByLabelText('DNI'), '30111222')

    await user.click(screen.getByRole('button', { name: 'Volver' }))
    expect(await screen.findByRole('heading', { level: 2, name: 'Elegí día y horario' })).toHaveFocus()
    expect(screen.getByRole('button', { name: '10:00' })).toHaveAttribute('aria-pressed', 'true')
    await user.click(screen.getByRole('button', { name: '10:00' }))

    expect(screen.getByLabelText('DNI')).toHaveValue('30111222')
  })

  it('con el DNI vacío muestra el error asociado y lleva el foco al campo, sin enviar', async () => {
    const simulada = api()
    const user = userEvent.setup()
    renderizarEn('/reservar')
    await hastaElHorario(user)
    await user.click(await screen.findByRole('button', { name: '10:00' }))
    await completarDatos(user)
    await user.clear(screen.getByLabelText('DNI'))

    await user.click(screen.getByRole('button', { name: 'Reservar turno' }))

    const dni = screen.getByLabelText('DNI')
    expect(await screen.findByText('Ingresá un DNI de 7 u 8 dígitos')).toHaveAttribute('id', 'dni-error')
    expect(dni).toHaveAttribute('aria-describedby', 'dni-error')
    expect(dni).toHaveAttribute('aria-invalid', 'true')
    expect(dni).toHaveFocus()
    expect(pedidos(simulada)).not.toContain('POST /api/publico/reservas')
  })

  it('un doble toque en "Reservar turno" manda un solo pedido', async () => {
    let liberar: () => void = () => undefined
    const simulada = api()
    const fetchOriginal = simulada.getMockImplementation()
    simulada.mockImplementation(async (url: string, init?: RequestInit) => {
      if (init?.method === 'POST') {
        await new Promise<void>((resolve) => {
          liberar = resolve
        })
      }
      return fetchOriginal!(url, init)
    })
    const user = userEvent.setup()
    renderizarEn('/reservar')
    await hastaElHorario(user)
    await user.click(await screen.findByRole('button', { name: '10:00' }))
    await completarDatos(user)

    const boton = screen.getByRole('button', { name: 'Reservar turno' })
    await user.click(boton)
    await waitFor(() => expect(boton).toBeDisabled())
    await user.click(boton)
    liberar()

    await screen.findByRole('heading', { level: 2, name: '¡Listo! Reservaste tu turno' })
    expect(pedidos(simulada).filter((p) => p === 'POST /api/publico/reservas')).toHaveLength(1)
  })

  it('si el horario fue tomado avisa, vuelve a pedir los horarios del día y conserva los datos', async () => {
    let consultas = 0
    let intentos = 0
    api(
      () => {
        intentos++
        return intentos === 1
          ? { status: 409, cuerpo: { title: 'Conflicto', detail: 'El horario elegido ya no está disponible; elegí otro' } }
          : { status: 201, cuerpo: { ...CONFIRMADA, inicio: '2026-12-02T10:15:00-03:00' } }
      },
      () => {
        consultas++
        return consultas === 1 ? HUECOS : [HUECOS[1]]
      },
    )
    const user = userEvent.setup()
    renderizarEn('/reservar')
    await hastaElHorario(user)
    await user.click(await screen.findByRole('button', { name: '10:00' }))
    await completarDatos(user)
    await user.click(screen.getByRole('button', { name: 'Reservar turno' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('El horario elegido ya no está disponible; elegí otro')
    expect(screen.getByRole('heading', { level: 2, name: 'Elegí día y horario' })).toBeInTheDocument()
    await waitFor(() => expect(screen.queryByRole('button', { name: '10:00' })).not.toBeInTheDocument())
    await user.click(screen.getByRole('button', { name: '10:15' }))

    expect(screen.getByLabelText('DNI')).toHaveValue('30111222')
    expect(screen.getByLabelText('Email')).toHaveValue('carla@example.com')
    await user.click(screen.getByRole('button', { name: 'Reservar turno' }))
    expect(await screen.findByRole('heading', { level: 2, name: '¡Listo! Reservaste tu turno' })).toBeInTheDocument()
  })

  it('si se pasó el límite de pedidos muestra el mensaje de espera', async () => {
    api({ status: 429, cuerpo: { detail: 'Hiciste demasiados pedidos de reserva. Probá de nuevo en unos minutos.' } })
    const user = userEvent.setup()
    renderizarEn('/reservar')
    await hastaElHorario(user)
    await user.click(await screen.findByRole('button', { name: '10:00' }))
    await completarDatos(user)

    await user.click(screen.getByRole('button', { name: 'Reservar turno' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Hiciste demasiados pedidos de reserva')
    expect(screen.getByRole('heading', { level: 2, name: 'Tus datos' })).toBeInTheDocument()
  })

  it('un 400 con errores por campo los muestra en cada campo', async () => {
    api({ status: 400, cuerpo: { detail: 'Revisá los datos marcados', errores: { email: 'El email del paciente no es válido' } } })
    const user = userEvent.setup()
    renderizarEn('/reservar')
    await hastaElHorario(user)
    await user.click(await screen.findByRole('button', { name: '10:00' }))
    await completarDatos(user)

    await user.click(screen.getByRole('button', { name: 'Reservar turno' }))

    expect(await screen.findByText('El email del paciente no es válido')).toHaveAttribute('id', 'email-error')
  })

  it('sin huecos ese día lo dice y muestra el contacto del consultorio', async () => {
    api(undefined, () => [])
    const user = userEvent.setup()
    renderizarEn('/reservar')

    await hastaElHorario(user)

    expect(await screen.findByText(/No hay horarios disponibles/)).toBeInTheDocument()
    expect(await screen.findByText('Llamanos al 0261 555-0000')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /^\d\d:\d\d$/ })).not.toBeInTheDocument()
  })
})
