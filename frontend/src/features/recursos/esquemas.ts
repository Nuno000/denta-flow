import { z } from 'zod'

/**
 * Validación de los formularios de recursos (design D6): mismas reglas y largos que el backend, solo por
 * usabilidad; la API vuelve a validar todo. Los campos entran como texto y salen con su tipo final.
 */

function textoObligatorio(mensajeVacio: string, maximo: number, mensajeMaximo: string) {
  return z.string().trim().min(1, mensajeVacio).max(maximo, mensajeMaximo)
}

export const esquemaSillon = z.object({
  nombre: textoObligatorio('Ingresá el nombre', 50, 'El nombre no puede superar los 50 caracteres'),
})

export const esquemaPrestacion = z.object({
  nombre: textoObligatorio('Ingresá el nombre', 100, 'El nombre no puede superar los 100 caracteres'),
  duracionMinutos: z
    .string()
    .superRefine((valor, ctx) => {
      const texto = valor.trim()
      if (texto === '') {
        ctx.addIssue({ code: 'custom', message: 'Ingresá la duración en minutos' })
      } else if (!/^\d+$/.test(texto) || Number(texto) <= 0) {
        ctx.addIssue({ code: 'custom', message: 'La duración debe ser mayor que cero' })
      }
    })
    .transform((valor) => Number(valor.trim())),
})

export const esquemaProfesional = z.object({
  nombre: textoObligatorio('Ingresá el nombre', 100, 'El nombre no puede superar los 100 caracteres'),
  apellido: textoObligatorio('Ingresá el apellido', 100, 'El apellido no puede superar los 100 caracteres'),
  matricula: z
    .string()
    .trim()
    .max(30, 'La matrícula no puede superar los 30 caracteres')
    .transform((valor) => (valor === '' ? null : valor)),
  email: z
    .string()
    .trim()
    .min(1, 'Ingresá el email')
    .max(254, 'El email no puede superar los 254 caracteres')
    .email('El email no tiene un formato válido'),
})

export type EntradaSillon = z.input<typeof esquemaSillon>
export type EntradaPrestacion = z.input<typeof esquemaPrestacion>
export type EntradaProfesional = z.input<typeof esquemaProfesional>
