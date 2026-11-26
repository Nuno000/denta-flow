import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { CLASES_BOTON_PRIMARIO } from '../../components/formulario/estilos'
import { CLASES_ALERTA } from '../reserva/estilosReserva'
import { TituloDePaso } from '../reserva/TituloDePaso'
import { mensajeErrorListaEspera, useAceptarOfertaListaEspera, useOfertaListaEspera } from './api'

function formatearFechaHoraAr(iso: string | null): string {
  if (!iso) return ''
  return new Intl.DateTimeFormat('es-AR', {
    dateStyle: 'full',
    timeStyle: 'short',
    timeZone: 'America/Argentina/Buenos_Aires',
  }).format(new Date(iso))
}

export function AceptarOfertaPage() {
  const [params] = useSearchParams()
  const token = params.get('token')?.trim() ?? ''
  const consulta = useOfertaListaEspera(token)
  const aceptar = useAceptarOfertaListaEspera(token)
  const [confirmadoLocal, setConfirmadoLocal] = useState(false)

  if (!token) {
    return (
      <main className="mx-auto max-w-lg p-6">
        <TituloDePaso>Enlace inválido</TituloDePaso>
        <p role="alert" className={`mt-4 ${CLASES_ALERTA}`}>
          El enlace de oferta de lista de espera no es válido.
        </p>
      </main>
    )
  }

  return (
    <main className="mx-auto max-w-lg p-6">
      <h1 className="text-xl font-bold text-slate-900">Oferta de turno — Lista de espera</h1>

      {consulta.isLoading && <p className="mt-4 text-slate-700">Consultando disponibilidad de la oferta...</p>}

      {consulta.isError && (
        <p role="alert" className={`mt-4 ${CLASES_ALERTA}`}>
          {mensajeErrorListaEspera(consulta.error)}
        </p>
      )}

      {consulta.data && (
        <section className="mt-4 flex flex-col gap-4 rounded-lg border border-slate-200 bg-white p-5">
          <div>
            <p className="text-sm text-slate-600">Prestación</p>
            <p className="font-semibold text-slate-900">{consulta.data.prestacionNombre}</p>
          </div>
          {consulta.data.profesionalNombre && (
            <div>
              <p className="text-sm text-slate-600">Profesional</p>
              <p className="font-semibold text-slate-900">{consulta.data.profesionalNombre}</p>
            </div>
          )}
          <div>
            <p className="text-sm text-slate-600">Fecha y hora (Argentina)</p>
            <p className="font-semibold text-slate-900">{formatearFechaHoraAr(consulta.data.inicioOfrecido)}</p>
          </div>

          {consulta.data.estado === 'atendida' || confirmadoLocal ? (
            <div role="status" className="rounded-md bg-emerald-50 p-3 text-emerald-900">
              ¡Turno confirmado! Tu lugar quedó reservado.
            </div>
          ) : consulta.data.estado === 'vencida' ? (
            <p role="alert" className={CLASES_ALERTA}>
              Esta oferta venció o el horario ya fue tomado por otro paciente.
            </p>
          ) : (
            <>
              {aceptar.isError && (
                <p role="alert" className={CLASES_ALERTA}>
                  {mensajeErrorListaEspera(aceptar.error)}
                </p>
              )}
              <button
                type="button"
                disabled={aceptar.isPending}
                className={`${CLASES_BOTON_PRIMARIO} min-h-11 w-full`}
                onClick={() =>
                  aceptar.mutate(undefined, {
                    onSuccess: () => setConfirmadoLocal(true),
                  })
                }
              >
                {aceptar.isPending ? 'Confirmando turno...' : 'Aceptar turno'}
              </button>
            </>
          )}
        </section>
      )}
    </main>
  )
}
