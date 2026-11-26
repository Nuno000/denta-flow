import { useState } from 'react'
import { Dialogo } from '../../components/formulario/Dialogo'
import { CLASES_ACCION } from '../../components/formulario/estilos'
import { Celda } from '../../components/tabla/Celda'
import { useActivarPrestacion, useDesactivarPrestacion, usePrestaciones, type Prestacion } from './api'
import { useAvisos, useEsAdministrador } from './avisosYErrores'
import { Avisos, ConfirmarDesactivacion, EncabezadoDeTab, Estado, MensajeDeError } from './comunes'
import { FormularioPrestacion } from './FormularioPrestacion'
import { FilaDeRecurso, TablaDeRecursos } from './TablaDeRecursos'

type DialogoAbierto = { tipo: 'alta' } | { tipo: 'editar' | 'desactivar'; prestacion: Prestacion }

/** Pestaña Prestaciones: nombre y duración de cada una (US-001, RN-TU-03). */
export function PrestacionesTab() {
  const esAdmin = useEsAdministrador()
  const prestaciones = usePrestaciones()
  const desactivar = useDesactivarPrestacion()
  const activar = useActivarPrestacion()
  const avisos = useAvisos()
  const [dialogo, setDialogo] = useState<DialogoAbierto | null>(null)

  const cerrar = () => setDialogo(null)
  const terminar = (mensaje: string) => {
    setDialogo(null)
    avisos.anunciar(mensaje)
  }
  const reactivar = async (prestacion: Prestacion) => {
    try {
      await activar.mutateAsync(prestacion.id)
      avisos.anunciar('Prestación reactivada')
    } catch (causa) {
      avisos.fallar(causa)
    }
  }
  const confirmarDesactivacion = async (prestacion: Prestacion) => {
    try {
      await desactivar.mutateAsync(prestacion.id)
      terminar('Prestación desactivada')
    } catch (causa) {
      setDialogo(null)
      avisos.fallar(causa)
    }
  }

  return (
    <section className="flex flex-col gap-3">
      <EncabezadoDeTab
        titulo="Prestaciones"
        alta={esAdmin ? { texto: 'Nueva prestación', alPulsar: () => setDialogo({ tipo: 'alta' }) } : undefined}
      />
      <Avisos aviso={avisos.aviso} error={avisos.error} />
      {prestaciones.isPending && <p className="text-slate-700">Cargando prestaciones…</p>}
      {prestaciones.isError && (
        <MensajeDeError>No pudimos cargar las prestaciones. Probá de nuevo en unos minutos.</MensajeDeError>
      )}
      {prestaciones.data !== undefined && (
        <TablaDeRecursos
          etiqueta="Prestaciones del consultorio"
          columnas={esAdmin ? ['Nombre', 'Duración', 'Estado', 'Acciones'] : ['Nombre', 'Duración', 'Estado']}
        >
          {prestaciones.data.map((prestacion) => (
            <FilaDeRecurso key={prestacion.id}>
              <Celda etiqueta="Nombre">
                <span className="font-medium text-slate-900">{prestacion.nombre}</span>
              </Celda>
              <Celda etiqueta="Duración">{`${prestacion.duracionMinutos} min`}</Celda>
              <Celda etiqueta="Estado">
                <Estado activo={prestacion.activa} femenino />
              </Celda>
              {esAdmin && (
                <Celda etiqueta="Acciones">
                  <span className="flex flex-wrap gap-2 md:flex-nowrap">
                    <button
                      type="button"
                      aria-label={`Editar ${prestacion.nombre}`}
                      onClick={() => setDialogo({ tipo: 'editar', prestacion })}
                      className={CLASES_ACCION}
                    >
                      Editar
                    </button>
                    {prestacion.activa ? (
                      <button
                        type="button"
                        aria-label={`Desactivar ${prestacion.nombre}`}
                        onClick={() => setDialogo({ tipo: 'desactivar', prestacion })}
                        className={CLASES_ACCION}
                      >
                        Desactivar
                      </button>
                    ) : (
                      <button
                        type="button"
                        aria-label={`Reactivar ${prestacion.nombre}`}
                        disabled={activar.isPending}
                        onClick={() => void reactivar(prestacion)}
                        className={CLASES_ACCION}
                      >
                        Reactivar
                      </button>
                    )}
                  </span>
                </Celda>
              )}
            </FilaDeRecurso>
          ))}
        </TablaDeRecursos>
      )}

      {dialogo?.tipo === 'alta' && (
        <Dialogo titulo="Nueva prestación" alCerrar={cerrar}>
          <FormularioPrestacion alTerminar={terminar} alCancelar={cerrar} />
        </Dialogo>
      )}
      {dialogo?.tipo === 'editar' && (
        <Dialogo titulo={`Editar ${dialogo.prestacion.nombre}`} alCerrar={cerrar}>
          <FormularioPrestacion prestacion={dialogo.prestacion} alTerminar={terminar} alCancelar={cerrar} />
        </Dialogo>
      )}
      {dialogo?.tipo === 'desactivar' && (
        <ConfirmarDesactivacion
          nombre={dialogo.prestacion.nombre}
          pendiente={desactivar.isPending}
          alConfirmar={() => void confirmarDesactivacion(dialogo.prestacion)}
          alCerrar={cerrar}
        />
      )}
    </section>
  )
}
