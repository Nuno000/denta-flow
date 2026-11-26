import { z } from 'zod'
import { esFechaValida } from './fechas'

/**
 * Validación del formulario "Nuevo turno" (C-12, design D6): mismas reglas que el backend (`Dni`, `Paciente`), solo
 * por usabilidad; la API vuelve a validar todo. Los campos entran como texto y salen con su tipo final.
 */

const SEPARADORES_DE_DNI = /[.\s-]/g
const SEPARADORES_DE_TELEFONO = /[\s\-().]/g
const FORMATO_EMAIL = /^[^@\s]+@[^@\s.]+(\.[^@\s.]+)+$/
const FORMATO_HORA = /^([01]\d|2[0-3]):[0-5]\d$/

function textoObligatorio(mensajeVacio: string, maximo: number, mensajeMaximo: string) {
  return z.string().trim().min(1, mensajeVacio).max(maximo, mensajeMaximo)
}

function idObligatorio(mensaje: string) {
  return z
    .string()
    .refine((valor) => /^\d+$/.test(valor), mensaje)
    .transform(Number)
}

/** DNI de 7 u 8 dígitos; sale sin puntos, espacios ni guiones. También lo usa la búsqueda por DNI. */
export const esquemaDni = z
  .string()
  .transform((valor) => valor.replace(SEPARADORES_DE_DNI, ''))
  .refine((valor) => /^\d{7,8}$/.test(valor), 'Ingresá un DNI de 7 u 8 dígitos')

const esquemaPaciente = z.object({
  dni: esquemaDni,
  nombre: textoObligatorio('Ingresá el nombre', 100, 'El nombre no puede superar los 100 caracteres'),
  apellido: textoObligatorio('Ingresá el apellido', 100, 'El apellido no puede superar los 100 caracteres'),
  telefono: z
    .string()
    .transform((valor) => valor.replace(SEPARADORES_DE_TELEFONO, ''))
    .refine((valor) => valor !== '', 'Ingresá el teléfono')
    .refine((valor) => valor === '' || /^\+?\d{8,15}$/.test(valor), 'El teléfono debe tener entre 8 y 15 dígitos'),
  email: z
    .string()
    .trim()
    .min(1, 'Ingresá el email')
    .max(254, 'El email no puede superar los 254 caracteres')
    .refine((valor) => valor === '' || FORMATO_EMAIL.test(valor), 'El email no tiene un formato válido'),
})

export const esquemaCargaDeTurno = z.object({
  paciente: esquemaPaciente,
  prestacionId: idObligatorio('Elegí la prestación'),
  profesionalId: idObligatorio('Elegí el profesional'),
  sillonId: idObligatorio('Elegí el sillón'),
  fecha: z.string().refine(esFechaValida, 'Ingresá una fecha válida'),
  hora: z.string().refine((valor) => FORMATO_HORA.test(valor), 'Ingresá una hora válida (HH:mm)'),
})

export type EntradaCargaDeTurno = z.input<typeof esquemaCargaDeTurno>
export type DatosCargaDeTurno = z.output<typeof esquemaCargaDeTurno>
