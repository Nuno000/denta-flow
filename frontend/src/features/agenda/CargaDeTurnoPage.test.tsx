import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { renderizarEn, simularApi, type RespuestaSimulada } from '../../test/apiSimulada'
import { sesionRecepcion } from '../../test/datosAgenda'
import { apiDeRecursos, sesionAdmin } from '../../test/datosRecursos'
import type { Alternativa, TurnoCargado } from './api'

/**
 * Pantalla "Nuevo turno" (C-12, specs "Pantalla de carga de turno en el panel", "Conflicto y alternativas en la
 * pantalla", "Sobreturno explícito en la pantalla" y "Formulario accesible y validado"). Datos ficticios.
 */

const RUTA = '/panel/agenda/nuevo-turno'

const PACIENTE = { id: 5, dni: '30123456', nombre: 'Ana', apellido: 'Ejemplo', telefono: '+541155550000', email: 'ana@example.com' }

const TURNO: TurnoCargado = {
  id: 90,
  inicio: '2026-12-01T09:00:00-03:00',
  fin: '2026-12-01T09:40:00-03:00',
  estado: 'pendiente',
  esSobreturno: false,
  pacienteId: 5,
  prestacionId: 4,
  profesionalId: 10,
  sillonId: 1,
}

const ALTERNATIVA: Alternativa = {
  inicio: '2026-12-01T10:00:00-03:00',
  fin: '2026-12-01T10:40:00-03:00',
  profesionalId: 10,
  sillonId: 2,
}

type Manejadores = Record<string, RespuestaSimulada | ((cuerpo: unknown) => RespuestaSimulada)>

/** La API con ambos sillones activos y los manejadores extra de cada caso. */
function api(extra: Manejadores = {}) {
  return simularApi(
    apiDeRecursos(sesionRecepcion.usuario, {
      'GET /api/sillones': {
        status: 200,
        cuerpo: [
          { id: 1, nombre: 'Sillón 1', activo: true },
          { id: 2, nombre: 'Sillón 2', activo: true },
          { id: 3, nombre: 'Sillón 3', activo: false },
        ],
      },
      ...extra,
    }),
  )
}

async function abrir(sesion = sesionRecepcion) {
  const resultado = renderizarEn(RUTA, sesion)
  await screen.findByRole('heading', { level: 1, name: 'Nuevo turno' })
  await screen.findByRole('option', { name: 'Limpieza' })
  return { ...resultado, usuario: userEvent.setup() }
}

type Usuario = ReturnType<typeof userEvent.setup>

async function completarPaciente(usuario: Usuario, datos = PACIENTE) {
  const campos = [
    ['DNI', datos.dni],
    ['Nombre', datos.nombre],
    ['Apellido', datos.apellido],
    ['Teléfono', datos.telefono],
    ['Email', datos.email],
  ] as const
  for (const [etiqueta, valor] of campos) {
    if (valor !== '') {
      await usuario.type(screen.getByLabelText(etiqueta), valor)
    }
  }
}

async function completarTurno(usuario: Usuario) {
  await usuario.selectOptions(screen.getByLabelText('Prestación'), 'Limpieza')
  await usuario.selectOptions(screen.getByLabelText('Profesional'), 'Laura Ejemplo')
  await usuario.selectOptions(screen.getByLabelText('Sillón'), 'Sillón 1')
  fireEvent.change(screen.getByLabelText('Fecha'), { target: { value: '2026-12-01' } })
  fireEvent.change(screen.getByLabelText('Hora'), { target: { value: '09:00' } })
}

async function completarTodo(usuario: Usuario) {
  await completarPaciente(usuario)
  await completarTurno(usuario)
}

function cuerpos(fetchSimulado: ReturnType<typeof simularApi>, metodoYRuta: string): unknown[] {
  return fetchSimulado.mock.calls
    .filter(([url, init]) => `${init?.method ?? 'GET'} ${new URL(String(url), 'http://localhost').pathname}` === metodoYRuta)
    .map(([, init]) => (typeof init?.body === 'string' ? (JSON.parse(init.body) as unknown) : undefined))
}

const PEDIDO_ESPERADO = {
  paciente: { dni: '30123456', nombre: 'Ana', apellido: 'Ejemplo', telefono: '+541155550000', email: 'ana@example.com' },
  prestacionId: 4,
  profesionalId: 10,
  sillonId: 1,
  inicio: '2026-12-01T09:00:00-03:00',
}

describe('pantalla Nuevo turno (C-12)', () => {
  afterEach(() => {
    sessionStorage.clear()
    vi.unstubAllGlobals()
  })

  describe('búsqueda del paciente por DNI', () => {
    it('un paciente encontrado precarga nombre, apellido, teléfono y email, editables', async () => {
      const simulada = api({ 'POST /api/pacientes/busqueda': { status: 200, cuerpo: PACIENTE } })
      const { usuario } = await abrir()

      await usuario.type(screen.getByLabelText('DNI'), '30.123.456')
      await usuario.click(screen.getByRole('button', { name: 'Buscar paciente' }))

      expect(await screen.findByText('Paciente encontrado: revisá sus datos y corregilos si hace falta')).toBeInTheDocument()
      expect(screen.getByLabelText('Nombre')).toHaveValue('Ana')
      expect(screen.getByLabelText('Apellido')).toHaveValue('Ejemplo')
      expect(screen.getByLabelText('Teléfono')).toHaveValue('+541155550000')
      expect(screen.getByLabelText('Email')).toHaveValue('ana@example.com')
      expect(cuerpos(simulada, 'POST /api/pacientes/busqueda')).toEqual([{ dni: '30123456' }])

      await usuario.clear(screen.getByLabelText('Teléfono'))
      await usuario.type(screen.getByLabelText('Teléfono'), '+541166660000')
      expect(screen.getByLabelText('Teléfono')).toHaveValue('+541166660000')
    })

    it('un DNI no registrado avisa que es un paciente nuevo y deja los campos vacíos', async () => {
      api({ 'POST /api/pacientes/busqueda': { status: 404, cuerpo: { detail: 'No hay un paciente con ese DNI' } } })
      const { usuario } = await abrir()

      await usuario.type(screen.getByLabelText('DNI'), '30999888')
      await usuario.click(screen.getByRole('button', { name: 'Buscar paciente' }))

      expect(await screen.findByText('Paciente nuevo: completá sus datos')).toBeInTheDocument()
      expect(screen.getByLabelText('Nombre')).toHaveValue('')
      expect(screen.getByLabelText('Email')).toHaveValue('')
    })

    it('un DNI mal escrito se marca en el campo y no se envía la búsqueda', async () => {
      const simulada = api()
      const { usuario } = await abrir()

      await usuario.type(screen.getByLabelText('DNI'), '30A23')
      await usuario.click(screen.getByRole('button', { name: 'Buscar paciente' }))

      expect(await screen.findByText('Ingresá un DNI de 7 u 8 dígitos')).toBeInTheDocument()
      expect(screen.getByLabelText('DNI')).toHaveAttribute('aria-invalid', 'true')
      expect(cuerpos(simulada, 'POST /api/pacientes/busqueda')).toEqual([])
    })

    it('si la búsqueda falla por otra causa lo informa sin tomarlo por paciente nuevo', async () => {
      api({ 'POST /api/pacientes/busqueda': { status: 500, cuerpo: { detail: 'Ocurrió un error inesperado' } } })
      const { usuario } = await abrir()

      await usuario.type(screen.getByLabelText('DNI'), '30123456')
      await usuario.click(screen.getByRole('button', { name: 'Buscar paciente' }))

      expect(await screen.findByRole('alert')).toHaveTextContent('Ocurrió un error inesperado')
      expect(screen.queryByText('Paciente nuevo: completá sus datos')).not.toBeInTheDocument()
    })
  })

  describe('opciones del formulario', () => {
    it('ofrece solo prestaciones y sillones activos', async () => {
      api()
      await abrir()

      const prestaciones = within(screen.getByLabelText('Prestación')).getAllByRole('option').map((o) => o.textContent)
      expect(prestaciones).toEqual(['Elegí una prestación', 'Consulta', 'Extracción', 'Limpieza'])
      const sillones = within(screen.getByLabelText('Sillón')).getAllByRole('option').map((o) => o.textContent)
      expect(sillones).toEqual(['Elegí un sillón', 'Sillón 1', 'Sillón 2'])
    })

    it('los profesionales se filtran por la prestación y solo salen los activos', async () => {
      api()
      const { usuario } = await abrir()
      const profesionales = () =>
        within(screen.getByLabelText('Profesional')).getAllByRole('option').map((o) => o.textContent)

      expect(profesionales()).toEqual(['Elegí primero la prestación'])
      await usuario.selectOptions(screen.getByLabelText('Prestación'), 'Limpieza')
      expect(profesionales()).toEqual(['Elegí un profesional', 'Laura Ejemplo'])
      await usuario.selectOptions(screen.getByLabelText('Prestación'), 'Consulta')
      expect(profesionales()).toEqual(['No hay profesionales para esta prestación'])
    })

    it('al cambiar de prestación se descarta un profesional que ya no corresponde', async () => {
      api()
      const { usuario } = await abrir()
      await usuario.selectOptions(screen.getByLabelText('Prestación'), 'Limpieza')
      await usuario.selectOptions(screen.getByLabelText('Profesional'), 'Laura Ejemplo')

      await usuario.selectOptions(screen.getByLabelText('Prestación'), 'Consulta')
      await usuario.selectOptions(screen.getByLabelText('Prestación'), 'Limpieza')

      expect(screen.getByLabelText('Profesional')).toHaveValue('')
    })
  })

  describe('validación en el cliente', () => {
    it('un campo obligatorio vacío se marca, se anuncia y no se envía el pedido', async () => {
      const simulada = api({ 'POST /api/turnos': { status: 201, cuerpo: TURNO } })
      const { usuario } = await abrir()
      await completarPaciente(usuario, { ...PACIENTE, telefono: '' })
      await completarTurno(usuario)

      await usuario.click(screen.getByRole('button', { name: 'Cargar turno' }))

      const telefono = screen.getByLabelText('Teléfono')
      expect(await screen.findByText('Ingresá el teléfono')).toBeInTheDocument()
      expect(telefono).toHaveAttribute('aria-invalid', 'true')
      expect(telefono).toHaveAccessibleDescription('Ingresá el teléfono')
      expect(screen.getByRole('alert')).toHaveTextContent('Revisá los campos marcados')
      expect(cuerpos(simulada, 'POST /api/turnos')).toEqual([])
    })

    it('un email sin formato válido se marca en su campo', async () => {
      api()
      const { usuario } = await abrir()
      await completarPaciente(usuario, { ...PACIENTE, email: 'ana-sin-arroba' })
      await completarTurno(usuario)

      await usuario.click(screen.getByRole('button', { name: 'Cargar turno' }))

      expect(await screen.findByText('El email no tiene un formato válido')).toBeInTheDocument()
    })
  })

  describe('carga exitosa', () => {
    it('envía el pedido con el inicio en hora del consultorio, anuncia "Turno cargado" y ofrece volver a la agenda', async () => {
      const simulada = api({ 'POST /api/turnos': { status: 201, cuerpo: TURNO } })
      const { usuario } = await abrir()
      await completarTodo(usuario)

      await usuario.click(screen.getByRole('button', { name: 'Cargar turno' }))

      const anuncio = await screen.findByText('Turno cargado')
      expect(anuncio).toHaveAttribute('aria-live', 'polite')
      expect(cuerpos(simulada, 'POST /api/turnos')).toEqual([PEDIDO_ESPERADO])
      expect(screen.getByRole('link', { name: 'Volver a la agenda' })).toHaveAttribute('href', '/panel/agenda?fecha=2026-12-01')
      expect(screen.getByLabelText('Nombre')).toHaveValue('')
      expect(screen.getByLabelText('DNI')).toHaveValue('')
    })

    it('el administrador también puede cargar', async () => {
      const simulada = simularApi(
        apiDeRecursos(sesionAdmin.usuario, { 'POST /api/turnos': { status: 201, cuerpo: TURNO } }),
      )
      const { usuario } = await abrir(sesionAdmin)
      await completarTodo(usuario)

      await usuario.click(screen.getByRole('button', { name: 'Cargar turno' }))

      expect(await screen.findByText('Turno cargado')).toBeInTheDocument()
      expect(cuerpos(simulada, 'POST /api/turnos')).toHaveLength(1)
    })
  })

  describe('errores del backend', () => {
    it('un 400 con error en paciente.email lo muestra junto al campo email', async () => {
      api({
        'POST /api/turnos': {
          status: 400,
          cuerpo: { detail: 'Revisá los datos marcados', errores: { 'paciente.email': 'El email es obligatorio' } },
        },
      })
      const { usuario } = await abrir()
      await completarTodo(usuario)

      await usuario.click(screen.getByRole('button', { name: 'Cargar turno' }))

      const email = screen.getByLabelText('Email')
      await waitFor(() => expect(email).toHaveAttribute('aria-invalid', 'true'))
      expect(email).toHaveAccessibleDescription('El email es obligatorio')
    })

    it('un 400 sin campo (p. ej., inicio en el pasado) se muestra como aviso general', async () => {
      api({
        'POST /api/turnos': {
          status: 400,
          cuerpo: { detail: 'El inicio del turno no puede ser anterior al momento actual' },
        },
      })
      const { usuario } = await abrir()
      await completarTodo(usuario)

      await usuario.click(screen.getByRole('button', { name: 'Cargar turno' }))

      expect(await screen.findByRole('alert')).toHaveTextContent('El inicio del turno no puede ser anterior al momento actual')
    })

    it('un 409 con alternativas muestra el motivo y las opciones en hora del consultorio', async () => {
      api({
        'POST /api/turnos': {
          status: 409,
          cuerpo: {
            detail: 'El horario ya está ocupado: el profesional tiene otro turno en ese horario',
            alternativas: [ALTERNATIVA, { ...ALTERNATIVA, inicio: '2026-12-01T10:15:00-03:00', fin: '2026-12-01T10:55:00-03:00', sillonId: 1 }],
          },
        },
      })
      const { usuario } = await abrir()
      await completarTodo(usuario)

      await usuario.click(screen.getByRole('button', { name: 'Cargar turno' }))

      expect(await screen.findByText('El horario ya está ocupado: el profesional tiene otro turno en ese horario')).toBeInTheDocument()
      expect(screen.getByRole('button', { name: 'Usar martes 1/12 10:00 — Sillón 2' })).toBeInTheDocument()
      expect(screen.getByRole('button', { name: 'Usar martes 1/12 10:15 — Sillón 1' })).toBeInTheDocument()
    })

    it('elegir una alternativa cambia fecha, hora y sillón sin enviar un pedido nuevo', async () => {
      const simulada = api({
        'POST /api/turnos': { status: 409, cuerpo: { detail: 'El horario ya está ocupado', alternativas: [ALTERNATIVA] } },
      })
      const { usuario } = await abrir()
      await completarTodo(usuario)
      await usuario.click(screen.getByRole('button', { name: 'Cargar turno' }))
      await screen.findByRole('button', { name: 'Usar martes 1/12 10:00 — Sillón 2' })

      await usuario.click(screen.getByRole('button', { name: 'Usar martes 1/12 10:00 — Sillón 2' }))

      expect(screen.getByLabelText('Hora')).toHaveValue('10:00')
      expect(screen.getByLabelText('Fecha')).toHaveValue('2026-12-01')
      expect(screen.getByLabelText('Sillón')).toHaveValue('2')
      expect(cuerpos(simulada, 'POST /api/turnos')).toHaveLength(1)
    })

    it('un 409 sin alternativas avisa que no hay horarios cercanos libres para ese profesional', async () => {
      api({ 'POST /api/turnos': { status: 409, cuerpo: { detail: 'El horario ya está ocupado', alternativas: [] } } })
      const { usuario } = await abrir()
      await completarTodo(usuario)

      await usuario.click(screen.getByRole('button', { name: 'Cargar turno' }))

      expect(await screen.findByText('El horario ya está ocupado')).toBeInTheDocument()
      expect(screen.getByText('No hay horarios cercanos libres para ese profesional')).toBeInTheDocument()
      expect(screen.queryByRole('button', { name: /^Usar / })).not.toBeInTheDocument()
    })

    it('un 403 muestra que no tiene permiso para cargar turnos', async () => {
      api({ 'POST /api/turnos': { status: 403, cuerpo: { detail: 'No tenés permiso para realizar esta operación' } } })
      const { usuario } = await abrir()
      await completarTodo(usuario)

      await usuario.click(screen.getByRole('button', { name: 'Cargar turno' }))

      expect(await screen.findByRole('alert')).toHaveTextContent('No tenés permiso para cargar turnos')
    })
  })

  describe('sobreturno explícito', () => {
    it('con el formulario incompleto valida y no abre el diálogo', async () => {
      api()
      const { usuario } = await abrir()

      await usuario.click(screen.getByRole('button', { name: 'Cargar sobreturno' }))

      expect(await screen.findByRole('alert')).toHaveTextContent('Revisá los campos marcados')
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    })

    it('confirmar en el diálogo envía el sobreturno y anuncia "Sobreturno cargado"', async () => {
      const simulada = api({
        'POST /api/turnos/sobreturnos': { status: 201, cuerpo: { ...TURNO, esSobreturno: true } },
      })
      const { usuario } = await abrir()
      await completarTodo(usuario)

      await usuario.click(screen.getByRole('button', { name: 'Cargar sobreturno' }))
      const dialogo = await screen.findByRole('dialog', { name: '¿Cargar un sobreturno?' })
      expect(within(dialogo).getByText(/se superpone con otros turnos del profesional o del sillón/)).toBeInTheDocument()
      expect(cuerpos(simulada, 'POST /api/turnos/sobreturnos')).toEqual([])
      await usuario.click(within(dialogo).getByRole('button', { name: 'Confirmar sobreturno' }))

      expect(await screen.findByText('Sobreturno cargado')).toBeInTheDocument()
      expect(cuerpos(simulada, 'POST /api/turnos/sobreturnos')).toEqual([PEDIDO_ESPERADO])
      expect(cuerpos(simulada, 'POST /api/turnos')).toEqual([])
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    })

    it('cancelar el diálogo no envía ningún pedido y conserva los datos', async () => {
      const simulada = api()
      const { usuario } = await abrir()
      await completarTodo(usuario)
      await usuario.click(screen.getByRole('button', { name: 'Cargar sobreturno' }))
      const dialogo = await screen.findByRole('dialog')

      await usuario.click(within(dialogo).getByRole('button', { name: 'Cancelar' }))

      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
      expect(cuerpos(simulada, 'POST /api/turnos/sobreturnos')).toEqual([])
      expect(cuerpos(simulada, 'POST /api/turnos')).toEqual([])
      expect(screen.getByLabelText('Nombre')).toHaveValue('Ana')
    })

    it('un 409 del sobreturno (fuera de horario) muestra el motivo sin ofrecer alternativas', async () => {
      api({
        'POST /api/turnos/sobreturnos': { status: 409, cuerpo: { detail: 'El profesional no atiende en ese horario' } },
      })
      const { usuario } = await abrir()
      await completarTodo(usuario)
      await usuario.click(screen.getByRole('button', { name: 'Cargar sobreturno' }))
      await usuario.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Confirmar sobreturno' }))

      expect(await screen.findByText('El profesional no atiende en ese horario')).toBeInTheDocument()
      expect(screen.queryByText('No hay horarios cercanos libres para ese profesional')).not.toBeInTheDocument()
    })
  })
})
