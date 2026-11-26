import { z } from 'zod'
import { esquemaAvisoPrivacidad } from './avisoPrivacidadApi'

/**
 * Datos del paciente para reservar (design D6). Replica las reglas del backend (`Paciente` y `Dni` de C-09) para
 * avisar antes de enviar; el backend las vuelve a aplicar (spec "Datos de la reserva validados en el backend").
 */
const MENSAJE_DNI = 'Ingresá un DNI de 7 u 8 dígitos'
const MENSAJE_TELEFONO = 'Ingresá un teléfono de 8 a 15 dígitos'
const MENSAJE_EMAIL = 'Ingresá un email válido'

const SEPARADORES_DNI = /[.\s-]/g
const SEPARADORES_TELEFONO = /[\s\-().]/g
const FORMATO_EMAIL = /^[^@\s]+@[^@\s.]+(\.[^@\s.]+)+$/

export const esquemaDatosReserva = z
  .object({
    dni: z
      .string({ error: MENSAJE_DNI })
      .transform((dni) => dni.replace(SEPARADORES_DNI, ''))
      .pipe(z.string().regex(/^\d{7,8}$/, MENSAJE_DNI)),
    nombre: z.string({ error: 'Ingresá tu nombre' }).trim().min(1, 'Ingresá tu nombre').max(100, 'Usá hasta 100 caracteres'),
    apellido: z
      .string({ error: 'Ingresá tu apellido' })
      .trim()
      .min(1, 'Ingresá tu apellido')
      .max(100, 'Usá hasta 100 caracteres'),
    telefono: z
      .string({ error: MENSAJE_TELEFONO })
      .transform((telefono) => telefono.replace(SEPARADORES_TELEFONO, ''))
      .pipe(z.string().regex(/^\+?\d{8,15}$/, MENSAJE_TELEFONO)),
    email: z
      .string({ error: MENSAJE_EMAIL })
      .trim()
      .toLowerCase()
      .max(254, MENSAJE_EMAIL)
      .regex(FORMATO_EMAIL, MENSAJE_EMAIL),
  })
  .extend(esquemaAvisoPrivacidad.shape)

/** Lo que escribe el paciente en el formulario. */
export type DatosReservaEnFormulario = z.input<typeof esquemaDatosReserva>

/** Los datos ya validados y normalizados que se envían. */
export type DatosReserva = z.output<typeof esquemaDatosReserva>
