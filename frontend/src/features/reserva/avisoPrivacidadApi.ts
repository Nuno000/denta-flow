import { useQuery } from '@tanstack/react-query'
import { z } from 'zod'
import { apiClient } from '../../lib/apiClient'

/** Aviso de privacidad vigente (spec "Aviso de privacidad público"). */
export interface AvisoPrivacidadVigente {
  texto: string
  leyenda: string
}

export const MENSAJE_AVISO_NO_ACEPTADO = 'Tenés que aceptar el aviso de privacidad para reservar'

/**
 * Parte del formulario de reserva para el aviso (US-040). C-16 la combina con su esquema
 * (`esquemaReserva.merge(esquemaAvisoPrivacidad)` o `.extend(esquemaAvisoPrivacidad.shape)`).
 */
export const esquemaAvisoPrivacidad = z.object({
  aceptaAvisoPrivacidad: z
    .boolean({ error: MENSAJE_AVISO_NO_ACEPTADO })
    .refine((aceptado) => aceptado, { message: MENSAJE_AVISO_NO_ACEPTADO }),
})

/** Lee el aviso sin token: la reserva es pública. */
export function useAvisoPrivacidad() {
  return useQuery({
    queryKey: ['publico', 'aviso-privacidad'] as const,
    queryFn: () => apiClient.get<AvisoPrivacidadVigente>('/api/publico/aviso-privacidad', { autenticar: false }),
  })
}
