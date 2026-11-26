import { useState } from 'react'
import { Dialogo } from '../../components/formulario/Dialogo'
import { CLASES_ACCION } from '../../components/formulario/estilos'
import { Celda } from '../../components/tabla/Celda'
import { useActivarSillon, useDesactivarSillon, useSillones, type Sillon } from './api'
import { useAvisos, useEsAdministrador } from './avisosYErrores'
import { Avisos, ConfirmarDesactivacion, EncabezadoDeTab, Estado, MensajeDeError } from './comunes'
import { FormularioSillon } from './FormularioSillon'
import { FilaDeRecurso, TablaDeRecursos } from './TablaDeRecursos'

type DialogoAbierto = { tipo: 'alta' } | { tipo: 'editar' | 'desactivar'; sillon: Sillon }

/** Pestaña Sillones: lectura para el personal interno y gestión para el administrador (US-001). */
export function SillonesTab() {
  const esAdmin = useEsAdministrador()
  const sillones = useSillones()
  const desactivar = useDesactivarSillon()
  const activar = useActivarSillon()
  const avisos = useAvisos()
  const [dialogo, setDialogo] = useState<DialogoAbierto | null>(null)

  const cerrar = () => setDialogo(null)
  const terminar = (mensaje: string) => {
    setDialogo(null)
    avisos.anunciar(mensaje)
  }
  const reactivar = async (sillon: Sillon) => {
    try {
      await activar.mutateAsync(sillon.id)
      avisos.anunciar('Sillón reactivado')
    } catch (causa) {
      avisos.fallar(causa)
    }
  }
  const confirmarDesactivacion = async (sillon: Sillon) => {
    try {
      await desactivar.mutateAsync(sillon.id)
      terminar('Sillón desactivado')
    } catch (causa) {
      setDialogo(null)
      avisos.fallar(causa)
    }
  }

  return (
    <section className="flex flex-col gap-3">
      <EncabezadoDeTab
        titulo="Sillones"
        alta={esAdmin ? { texto: 'Nuevo sillón', alPulsar: () => setDialogo({ tipo: 'alta' }) } : undefined}
      />
      <Avisos aviso={avisos.aviso} error={avisos.error} />
      {sillones.isPending && <p className="text-slate-700">Cargando sillones…</p>}
      {sillones.isError && <MensajeDeError>No pudimos cargar los sillones. Probá de nuevo en unos minutos.</MensajeDeError>}
      {sillones.data !== undefined && (
        <TablaDeRecursos
          etiqueta="Sillones del consultorio"
          columnas={esAdmin ? ['Nombre', 'Estado', 'Acciones'] : ['Nombre', 'Estado']}
        >
          {sillones.data.map((sillon) => (
            <FilaDeRecurso key={sillon.id}>
              <Celda etiqueta="Nombre">
                <span className="font-medium text-slate-900">{sillon.nombre}</span>
              </Celda>
              <Celda etiqueta="Estado">
                <Estado activo={sillon.activo} />
              </Celda>
              {esAdmin && (
                <Celda etiqueta="Acciones">
                  <span className="flex flex-wrap gap-2 md:flex-nowrap">
                    <button
                      type="button"
                      aria-label={`Editar ${sillon.nombre}`}
                      onClick={() => setDialogo({ tipo: 'editar', sillon })}
                      className={CLASES_ACCION}
                    >
                      Editar
                    </button>
                    {sillon.activo ? (
                      <button
                        type="button"
                        aria-label={`Desactivar ${sillon.nombre}`}
                        onClick={() => setDialogo({ tipo: 'desactivar', sillon })}
                        className={CLASES_ACCION}
                      >
                        Desactivar
                      </button>
                    ) : (
                      <button
                        type="button"
                        aria-label={`Reactivar ${sillon.nombre}`}
                        disabled={activar.isPending}
                        onClick={() => void reactivar(sillon)}
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
        <Dialogo titulo="Nuevo sillón" alCerrar={cerrar}>
          <FormularioSillon alTerminar={terminar} alCancelar={cerrar} />
        </Dialogo>
      )}
      {dialogo?.tipo === 'editar' && (
        <Dialogo titulo={`Editar ${dialogo.sillon.nombre}`} alCerrar={cerrar}>
          <FormularioSillon sillon={dialogo.sillon} alTerminar={terminar} alCancelar={cerrar} />
        </Dialogo>
      )}
      {dialogo?.tipo === 'desactivar' && (
        <ConfirmarDesactivacion
          nombre={dialogo.sillon.nombre}
          pendiente={desactivar.isPending}
          alConfirmar={() => void confirmarDesactivacion(dialogo.sillon)}
          alCerrar={cerrar}
        />
      )}
    </section>
  )
}
